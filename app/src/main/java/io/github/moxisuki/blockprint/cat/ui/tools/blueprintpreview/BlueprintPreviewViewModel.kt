package io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.moxisuki.pixelart.api.ExportApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintSink
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.ExportPayloadCodec
import io.github.moxisuki.blockprint.core.SchematicFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 导出目标。MC_COMMANDS 走文本命令（不进 BlueprintManager），其余都生成 .litematic /
 * .schematic / .structure / .json 字节并写入本地蓝图库。
 */
enum class ExportType(val schematic: SchematicFormat?) {
    MC_COMMANDS(null),
    BLUEPRINT_LITEMATICA(SchematicFormat.Litematica),
    BLUEPRINT_SPONGE(SchematicFormat.Sponge),
    BLUEPRINT_STRUCTURE(SchematicFormat.Structure),
    BLUEPRINT_BUILDING_HELPER(SchematicFormat.BuildingHelper),
}

data class BlueprintPreviewState(
    val resultImage: ImageBitmap? = null,
    val width: Int = 0,
    val height: Int = 0,
    val totalBlocks: Int = 0,
    val materials: Map<String, Int> = emptyMap(),
    val exportType: ExportType = ExportType.BLUEPRINT_LITEMATICA,
    val blueprintMode: BlueprintMode = BlueprintMode.WALL,
    val blueprintBytes: ByteArray? = null,
    val commandsText: String = "",
    val isBuilding: Boolean = false,
    val saveMessage: String? = null,
)

@HiltViewModel
class BlueprintPreviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context?,
    private val blueprintSink: BlueprintSink,
) : ViewModel() {

    private val _state = MutableStateFlow(BlueprintPreviewState())
    val state: StateFlow<BlueprintPreviewState> = _state.asStateFlow()

    /**
     * 测试专用副构造器：直接传入 URL-decoded 的编码串，跳过 Hilt 路径。
     * 测试用 [NoopBlueprintSink] 占位，只覆盖 init / format 切换等纯函数路径。
     */
    @VisibleForTesting
    constructor(encodedPayload: String) : this(
        context = null,
        blueprintSink = NoopBlueprintSink,
    ) {
        init(encodedPayload)
    }

    fun init(encodedPayload: String) {
        if (_state.value.resultImage != null) return // 已初始化
        val decoded = ExportPayloadCodec.decode(encodedPayload)
        _state.update {
            it.copy(
                resultImage = decoded.bitmap.asImageBitmap(),
                width = decoded.width,
                height = decoded.height,
                totalBlocks = decoded.totalBlocks,
                materials = decoded.materials,
            )
        }
    }

    fun setExportType(type: ExportType) {
        _state.update { it.copy(exportType = type, saveMessage = null) }
    }

    fun setBlueprintMode(mode: BlueprintMode) {
        _state.update { it.copy(blueprintMode = mode, saveMessage = null) }
    }

    fun consumeSaveMessage() {
        _state.update { it.copy(saveMessage = null) }
    }

    /**
     * 根据当前 exportType / blueprintMode 生成最终的字节或命令文本，存入 state。
     *
     * 对 BLUEPRINT_*：bitmap → grid（最近色匹配，等价于重跑 PixelArtConverter）→ BlockPrintDocument
     * → BlockPrintConverter.encode。
     * 对 MC_COMMANDS：bitmap → grid → ExportApi.generateCommandsWithDirection(默认 ES)。
     */
    fun buildBlueprint() {
        val s = _state.value
        val img: ImageBitmap = s.resultImage ?: return
        val bitmap = img.asAndroidBitmap()
        _state.update { it.copy(isBuilding = true, saveMessage = null, blueprintBytes = null, commandsText = "") }
        viewModelScope.launch {
            try {
                val (bytes, cmds) = withContext(Dispatchers.Default) {
                    val grid = BlueprintBuilder.bitmapToGrid(bitmap)
                    when (val type = s.exportType) {
                        ExportType.MC_COMMANDS -> {
                            val set = ExportApi.generateCommandsWithDirection(
                                blocks = grid,
                                width = s.width,
                                height = s.height,
                                mode = ExportApi.CommandDirection.ES,
                            )
                            null to ExportApi.commandsToString(set)
                        }
                        else -> {
                            val format = type.schematic
                                ?: error("ExportType $type has no SchematicFormat")
                            val doc = BlueprintBuilder.buildDocument(
                                grid = grid,
                                width = s.width,
                                height = s.height,
                                mode = s.blueprintMode,
                                format = format,
                                name = s.exportType.name.lowercase(),
                            )
                            BlueprintBuilder.encode(doc) to ""
                        }
                    }
                }
                _state.update {
                    it.copy(
                        isBuilding = false,
                        blueprintBytes = bytes,
                        commandsText = cmds,
                        saveMessage = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isBuilding = false,
                        blueprintBytes = null,
                        saveMessage = "build failed: ${e.message ?: e.javaClass.simpleName}",
                    )
                }
            }
        }
    }

    /**
     * 把当前 [blueprintBytes] 写入 BlueprintManager。MC_COMMANDS 类型仅写入文本命令预览，
     * 不入蓝图库（用户后续可手动复制粘贴）。
     */
    fun save(name: String) {
        val s = _state.value
        val safeName = name.trim().ifEmpty { defaultName() }
        when (s.exportType) {
            ExportType.MC_COMMANDS -> {
                // 文本命令不写入文件管理器；保留 cmds 文本在 state 供 UI 复制。
                _state.update { it.copy(saveMessage = "mc commands ready: $safeName") }
            }
            else -> {
                val bytes = s.blueprintBytes
                if (bytes == null) {
                    _state.update { it.copy(saveMessage = "no blueprint bytes: build first") }
                    return
                }
                viewModelScope.launch {
                    runCatching { blueprintSink.ingest(safeName, bytes) }
                        .onSuccess { meta ->
                            _state.update { it.copy(saveMessage = "saved: ${meta.displayName}") }
                        }
                        .onFailure { e ->
                            _state.update {
                                it.copy(saveMessage = "save failed: ${e.message ?: e.javaClass.simpleName}")
                            }
                        }
                }
            }
        }
    }

    private fun defaultName(): String {
        val ts = System.currentTimeMillis().toString()
        return "blueprint_$ts"
    }
}