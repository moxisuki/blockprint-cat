package io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.ExportPayloadCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class ExportFormat { LITEMATIC, SCHEMATIC, NBT }

data class BlueprintPreviewState(
    val resultImage: ImageBitmap? = null,
    val width: Int = 0,
    val height: Int = 0,
    val totalBlocks: Int = 0,
    val materials: Map<String, Int> = emptyMap(),
    val format: ExportFormat = ExportFormat.LITEMATIC,
)

@HiltViewModel
class BlueprintPreviewViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(BlueprintPreviewState())
    val state: StateFlow<BlueprintPreviewState> = _state.asStateFlow()

    /**
     * 测试专用副构造器：直接传入 URL-decoded 的编码串，跳过 Hilt 路径。
     * 生产路径由 Composable 在 LaunchedEffect 中调用 init(encodedPayload)。
     */
    @VisibleForTesting
    constructor(encodedPayload: String) : this() {
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

    fun setFormat(format: ExportFormat) {
        _state.update { it.copy(format = format) }
    }
}
