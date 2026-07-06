package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.moxisuki.pixelart.BlockSelector
import com.github.moxisuki.pixelart.ConversionOptions
import com.github.moxisuki.pixelart.DitherMethod as EngineDitherMethod
import com.github.moxisuki.pixelart.BlockFilter as EngineBlockFilter
import com.github.moxisuki.pixelart.PixelArtConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.flow.PreviewDebounce
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

    private val dirtySignal = PreviewDebounce(viewModelScope, debounceMs = 200L)

    init {
        // 收集 debounce 信号：每次静默 200ms 后触发一次 requestConvert()
        viewModelScope.launch {
            dirtySignal.flow.collect { requestConvert() }
        }
    }

    fun setImage(uri: Uri, width: Int, height: Int) {
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
        dirtySignal.push()
    }

    fun setTargetWidth(width: Int) {
        _state.update {
            it.copy(
                targetWidth = width.coerceIn(ImageToBlueprintState.MIN_WIDTH, ImageToBlueprintState.MAX_WIDTH),
                isUpdating = true,
            )
        }
        dirtySignal.push()
    }

    fun setDitherMethod(method: DitherMethod) {
        _state.update { it.copy(ditherMethod = method, isUpdating = true) }
        dirtySignal.push()
    }

    fun setBrightness(value: Int) {
        _state.update {
            it.copy(
                brightness = value.coerceIn(ImageToBlueprintState.MIN_ADJUST, ImageToBlueprintState.MAX_ADJUST),
                isUpdating = true,
            )
        }
        dirtySignal.push()
    }

    fun setContrast(value: Int) {
        _state.update {
            it.copy(
                contrast = value.coerceIn(ImageToBlueprintState.MIN_ADJUST, ImageToBlueprintState.MAX_ADJUST),
                isUpdating = true,
            )
        }
        dirtySignal.push()
    }

    fun setSaturation(value: Int) {
        _state.update {
            it.copy(
                saturation = value.coerceIn(ImageToBlueprintState.MIN_ADJUST, ImageToBlueprintState.MAX_ADJUST),
                isUpdating = true,
            )
        }
        dirtySignal.push()
    }

    fun setTransparencyEnabled(enabled: Boolean) {
        _state.update { it.copy(transparencyEnabled = enabled, isUpdating = true) }
        dirtySignal.push()
    }

    fun setTransparencyTolerance(value: Int) {
        _state.update {
            it.copy(
                transparencyTolerance = value.coerceIn(ImageToBlueprintState.MIN_TOLERANCE, ImageToBlueprintState.MAX_TOLERANCE),
                isUpdating = true,
            )
        }
        dirtySignal.push()
    }

    fun toggleGroup(group: BlockGroup) {
        _state.update { s ->
            val selected = s.selectedGroups.toMutableSet()
            if (selected.contains(group)) selected.remove(group) else selected.add(group)
            s.copy(selectedGroups = selected, isUpdating = true)
        }
        dirtySignal.push()
    }

    fun toggleFilter(filter: BlockFilter) {
        _state.update { s ->
            val active = s.activeFilters.toMutableSet()
            if (active.contains(filter)) active.remove(filter) else active.add(filter)
            s.copy(activeFilters = active, isUpdating = true)
        }
        dirtySignal.push()
    }

    fun resetAdjustments() {
        _state.update {
            it.copy(
                brightness = ImageToBlueprintState.DEFAULT_ADJUST,
                contrast = ImageToBlueprintState.DEFAULT_ADJUST,
                saturation = ImageToBlueprintState.DEFAULT_ADJUST,
                isUpdating = true,
            )
        }
        dirtySignal.push()
    }

    /**
     * 保留为兼容旧 UI 调用入口（Screen 上的"开始转换"按钮）。
     * 现在转换由 setter 防抖自动触发；显式点击等价于立即 push 一次信号。
     */
    fun startConvert() {
        dirtySignal.push()
    }

    private fun requestConvert() {
        val s = _state.value
        val uri = s.imageUri ?: return
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    val bitmap = loadBitmap(uri)
                        ?: throw IllegalStateException("Failed to load image")
                    val groupKeys = s.selectedGroups.map { it.key }.toSet()
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
                    s.activeFilters.forEach { f -> selector.applyFilter(mapBlockFilter(f), true) }
                    PixelArtConverter.convert(bitmap, options, selector)
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
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isUpdating = false, errorMessage = e.message ?: "Conversion failed") }
            }
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap? =
        context?.contentResolver?.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

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

    private fun mapBlockFilter(filter: BlockFilter): EngineBlockFilter = when (filter) {
        BlockFilter.EXCLUDE_FALLING -> EngineBlockFilter.FALLING
        BlockFilter.TRANSPARENT_ONLY -> EngineBlockFilter.TRANSPARENT
        BlockFilter.SURVIVAL_ONLY -> EngineBlockFilter.SURVIVAL
        BlockFilter.LUMINANCE_ONLY -> EngineBlockFilter.LUMINANCE
        BlockFilter.REDSTONE_ONLY -> EngineBlockFilter.REDSTONE
    }
}