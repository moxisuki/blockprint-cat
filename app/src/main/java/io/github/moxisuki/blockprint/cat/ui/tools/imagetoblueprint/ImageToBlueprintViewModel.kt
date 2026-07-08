package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.moxisuki.pixelart.api.ExportApi
import io.github.moxisuki.pixelart.BlockSelector
import io.github.moxisuki.pixelart.ConversionOptions
import io.github.moxisuki.pixelart.DitherMethod as EngineDitherMethod
import io.github.moxisuki.pixelart.PixelArtConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlueprintUiDefaults
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockGroup
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.DitherMethod
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.PreviewMode
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.ExportPayloadCodec
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.flow.PreviewDebounce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ImageToBlueprintViewModel @Inject constructor(
    @ApplicationContext private val context: Context?,
) : ViewModel() {

    /**
     * 测试专用无参构造器。Hilt 不允许 @Inject 构造器带默认值，所以单开一个无 @Inject
     * 的副构造器供纯 JVM 单测使用。生产路径走 @Inject + @ApplicationContext 注入。
     */
    @VisibleForTesting
    constructor() : this(context = null)

    private val _state = MutableStateFlow(ImageToBlueprintState())
    val state: StateFlow<ImageToBlueprintState> = _state.asStateFlow()

    /**
     * 有图片时：更新 state（设 isUpdating=true）并推 debounce 信号。
     * 无图片时：只更新参数（保留滑块位置等），不推信号、强制 isUpdating=false。
     * 这样用户调参数时没有图就不会显示"更新中"假状态。
     */
    private fun updateAndMaybeSchedule(transform: (ImageToBlueprintState) -> ImageToBlueprintState) {
        _state.update { current ->
            val candidate = transform(current)
            // 强制按 imageUri 是否存在决定 isUpdating——transform 里设的 true 也会被覆盖为 false
            candidate.copy(isUpdating = candidate.imageUri != null)
        }
        if (_state.value.imageUri != null) {
            dirtySignal.push()
        }
    }

    private val dirtySignal = PreviewDebounce(viewModelScope, debounceMs = 200L)

    init {
        // 收集 debounce 信号：每次静默 200ms 后触发一次 requestConvert()
        viewModelScope.launch {
            dirtySignal.flow.collect { requestConvert() }
        }
    }

    fun setImage(uri: Uri, width: Int, height: Int) {
        _cachedResultGrid = null
        _state.update {
            it.copy(
                imageUri = uri,
                imageWidth = width,
                imageHeight = height,
                previewMode = PreviewMode.Source,
                resultBitmap = null,
                resultWidth = 0,
                resultHeight = 0,
                resultTotalBlocks = 0,
                resultMaterialCounts = emptyMap(),
                errorMessage = null,
                isUpdating = true,
            )
        }
        if (width > 0 && height > 0) dirtySignal.push()
    }

    fun setTargetWidth(width: Int) {
        updateAndMaybeSchedule {
            it.copy(
                targetWidth = width.coerceIn(BlueprintUiDefaults.MIN_WIDTH, BlueprintUiDefaults.MAX_WIDTH),
                isUpdating = true,
            )
        }
    }

    fun setDitherMethod(method: DitherMethod) {
        updateAndMaybeSchedule { it.copy(ditherMethod = method, isUpdating = true) }
    }

    fun setBrightness(value: Int) {
        updateAndMaybeSchedule {
            it.copy(
                brightness = value.coerceIn(BlueprintUiDefaults.MIN_ADJUST, BlueprintUiDefaults.MAX_ADJUST),
                isUpdating = true,
            )
        }
    }

    fun setContrast(value: Int) {
        updateAndMaybeSchedule {
            it.copy(
                contrast = value.coerceIn(BlueprintUiDefaults.MIN_ADJUST, BlueprintUiDefaults.MAX_ADJUST),
                isUpdating = true,
            )
        }
    }

    fun setSaturation(value: Int) {
        updateAndMaybeSchedule {
            it.copy(
                saturation = value.coerceIn(BlueprintUiDefaults.MIN_ADJUST, BlueprintUiDefaults.MAX_ADJUST),
                isUpdating = true,
            )
        }
    }

    fun setTransparencyEnabled(enabled: Boolean) {
        updateAndMaybeSchedule { it.copy(transparencyEnabled = enabled, isUpdating = true) }
    }

    fun setTransparencyTolerance(value: Int) {
        updateAndMaybeSchedule {
            it.copy(
                transparencyTolerance = value.coerceIn(BlueprintUiDefaults.MIN_TOLERANCE, BlueprintUiDefaults.MAX_TOLERANCE),
                isUpdating = true,
            )
        }
    }

    fun toggleGroup(group: BlockGroup) {
        updateAndMaybeSchedule { s ->
            val selected = s.selectedGroups.toMutableSet()
            if (selected.contains(group)) selected.remove(group) else selected.add(group)
            s.copy(selectedGroups = selected, isUpdating = true)
        }
    }

    fun resetAdjustments() {
        updateAndMaybeSchedule {
            it.copy(
                brightness = BlueprintUiDefaults.DEFAULT_ADJUST,
                contrast = BlueprintUiDefaults.DEFAULT_ADJUST,
                saturation = BlueprintUiDefaults.DEFAULT_ADJUST,
                isUpdating = true,
            )
        }
    }

    /**
     * 保留为兼容旧 UI 调用入口（Screen 上的"开始转换"按钮）。
     * 现在转换由 setter 防抖自动触发；显式点击等价于立即 push 一次信号。
     */
    fun startConvert() {
        if (_state.value.imageUri != null) dirtySignal.push()
    }

    fun setCommandDirection(direction: ExportApi.CommandDirection) {
        _state.update { it.copy(commandDirection = direction) }
    }

    /**
     * Generates Minecraft commands for the current conversion result and updates state.
     * Must be called after a successful conversion (resultBitmap != null).
     */
    fun generateCommands(): String {
        val s = _state.value
        val blockGrid = _cachedResultGrid ?: return ""
        if (s.resultBitmap == null) return ""
        val text = with(ExportApi) {
            val cmds = generateCommandsWithDirection(
                blockGrid, s.resultWidth, s.resultHeight, s.commandDirection,
                baseX = 0, baseY = 64, baseZ = 0, useFill = true
            )
            commandsToString(cmds)
        }
        _state.update { it.copy(commandsText = text) }
        return text
    }

    private var _cachedResultGrid: Array<Array<io.github.moxisuki.pixelart.Block?>>? = null

    private fun requestConvert() {
        val s = _state.value
        val uri = s.imageUri ?: return
        val groupKeys = s.selectedGroups.map { it.key }.toSet()
        // 没选任何方块组：直接清空结果，不让 engine 抛 "No Block Available"
        if (groupKeys.isEmpty()) {
            _state.update {
                it.copy(
                    isUpdating = false,
                    resultBitmap = null,
                    resultWidth = 0,
                    resultHeight = 0,
                    resultTotalBlocks = 0,
                    resultMaterialCounts = emptyMap(),
                    errorMessage = null,
                    previewMode = PreviewMode.Source,
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val rawBitmap = loadBitmap(uri)
                        ?: throw IllegalStateException("Failed to load image")
                    // 透明开关打开时，先做"白底去除"预处理：把较亮的像素改成 alpha=0，
                    // engine 再用 alpha < tolerance 把它们识别为空气方块。
                    val bitmap = if (s.transparencyEnabled) {
                        makeBackgroundTransparent(rawBitmap, s.transparencyTolerance)
                    } else rawBitmap
                    val options = ConversionOptions(
                        targetWidth = s.targetWidth,
                        ditherMethod = mapDither(s.ditherMethod),
                        blockGroups = groupKeys,
                        brightness = s.brightness,
                        contrast = s.contrast,
                        saturation = s.saturation,
                        transparencyEnabled = s.transparencyEnabled,
                        transparencyTolerance = s.transparencyTolerance,
                    )
                    val selector = BlockSelector().selectGroups(groupKeys)
                    val convResult = PixelArtConverter.convert(bitmap, options, selector)
                    _cachedResultGrid = convResult.blocks
                    convResult
                }
                _state.update {
                    it.copy(
                        isUpdating = false,
                        resultBitmap = result.outputImage,
                        resultWidth = result.width,
                        resultHeight = result.height,
                        resultTotalBlocks = result.width * result.height,
                        resultMaterialCounts = PixelArtConverter.getMaterialList(result),
                        previewMode = PreviewMode.Result,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                // 失败时清掉 result 字段，避免旧图继续显示在 Hero 里
                _state.update {
                    it.copy(
                        isUpdating = false,
                        resultBitmap = null,
                        resultWidth = 0,
                        resultHeight = 0,
                        resultTotalBlocks = 0,
                        resultMaterialCounts = emptyMap(),
                        errorMessage = e.message ?: "Conversion failed",
                        previewMode = PreviewMode.Source,
                    )
                }
            }
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? =
        context?.contentResolver?.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

    /**
     * 把"较亮"的像素（max(R,G,B) > 255 - tolerance）改成 alpha=0，
     * engine 的 transparency 逻辑就会把它们识别为空气方块。
     * tolerance=0 不去除任何像素；tolerance=255 除纯黑以外都变透明。
     */
    private fun makeBackgroundTransparent(src: Bitmap, tolerance: Int): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val cutoff = 255 - tolerance.coerceIn(0, 255)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val max = maxOf(r, g, b)
            if (max > cutoff) {
                // 清掉 alpha 通道，RGB 保留
                pixels[i] = p and 0x00FFFFFF
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * 编码当前结果为可导航传输的字符串。resultBitmap 为 null 时返回 null，UI 端应禁用按钮。
     * 附带所有可复现当前 result grid 的转换参数，使 BP 预览页能用同一份输入重跑
     * PixelArtConverter 后导出真正的 .litematic / .schematic。
     */
    fun encodeForExport(): String? {
        val s = _state.value
        val bitmap = s.resultBitmap ?: return null
        return ExportPayloadCodec.encode(
            bitmap = bitmap,
            width = s.resultWidth,
            height = s.resultHeight,
            totalBlocks = s.resultTotalBlocks,
            materials = s.resultMaterialCounts,
            ditherMethod = s.ditherMethod.id,
            brightness = s.brightness,
            contrast = s.contrast,
            saturation = s.saturation,
            transparencyEnabled = s.transparencyEnabled,
            transparencyTolerance = s.transparencyTolerance,
            selectedGroups = s.selectedGroups.map { it.key },
            sourceWidth = s.imageWidth,
            sourceHeight = s.imageHeight,
        )
    }

    private fun mapDither(method: DitherMethod): EngineDitherMethod = when (method) {
        DitherMethod.NONE -> EngineDitherMethod.NONE
        DitherMethod.FLOYD_STEINBERG -> EngineDitherMethod.FLOYD_STEINBERG
        DitherMethod.BAYER_4X4 -> EngineDitherMethod.BAYER_4X4
        DitherMethod.BAYER_2X2 -> EngineDitherMethod.BAYER_2X2
        DitherMethod.ORDERED_3X3 -> EngineDitherMethod.ORDERED_3X3
        DitherMethod.MIN_AVG_ERR -> EngineDitherMethod.MIN_AVG_ERR
        DitherMethod.BURKES -> EngineDitherMethod.BURKES
        DitherMethod.SIERRA_LITE -> EngineDitherMethod.SIERRA_LITE
        DitherMethod.STUCKI -> EngineDitherMethod.STUCKI
        DitherMethod.ATKINSON -> EngineDitherMethod.ATKINSON
    }
}