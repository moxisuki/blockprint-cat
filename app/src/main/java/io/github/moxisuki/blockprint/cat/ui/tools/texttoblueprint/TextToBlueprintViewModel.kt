package io.github.moxisuki.blockprint.cat.ui.tools.texttoblueprint

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockCatalog
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.DitherMethod
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.ExportPayloadCodec
import io.github.moxisuki.blockprint.cat.ui.tools.blockpaint.BlockPaintRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TextToBlueprintViewModel @Inject constructor(
    @ApplicationContext private val context: Context?,
) : ViewModel() {

    constructor() : this(context = null)

    private val _state = MutableStateFlow(TextToBlueprintState())
    val state: StateFlow<TextToBlueprintState> = _state.asStateFlow()

    fun setText(text: String) {
        val cjk = Font8x8.hasCjk(text)
        _state.update { it.copy(text = text, useTtf = cjk, height = if (cjk && it.height < 16) 16 else it.height) }
        rebuild()
    }

    fun setSelectedBlock(blockId: String?) {
        if (blockId != null && BlockCatalog.all.none { it.id == blockId }) return
        _state.update { it.copy(selectedBlockId = blockId) }
        rebuild()
    }

    fun setScale(scale: Int) {
        _state.update { it.copy(scale = scale.coerceIn(TextToBlueprintState.MIN_SCALE, TextToBlueprintState.MAX_SCALE)) }
        rebuild()
    }

    fun setHeight(height: Int) {
        _state.update { it.copy(height = height.coerceIn(TextToBlueprintState.MIN_HEIGHT, TextToBlueprintState.MAX_HEIGHT)) }
        rebuild()
    }

    fun clearExport() { _state.update { it.copy(exportPayload = null) } }

    fun setSpacing(spacing: Int) {
        _state.update { it.copy(spacing = spacing.coerceIn(TextToBlueprintState.MIN_SPACING, TextToBlueprintState.MAX_SPACING)) }
        rebuild()
    }

    fun rebuild() {
        val s = _state.value
        if (s.text.isBlank() || s.selectedBlockId == null) {
            _state.update { it.copy(grid = Array(0) { arrayOfNulls(0) }, gridW = 0, gridH = 0) }
            return
        }
        val raw = if (s.useTtf) {
            Font8x8.renderTtf(s.text, s.height, s.spacing.coerceAtLeast(0))
        } else {
            Font8x8.render(s.text, s.spacing.coerceAtLeast(0))
        }
        val rawH = raw.size
        val rawW = if (raw.isNotEmpty()) raw[0].size else 0
        val w = rawW * s.scale
        val gh = rawH * s.scale
        val blockId = s.selectedBlockId
        val grid = Array(w) { x ->
            val srcX = x / s.scale
            Array(gh) { y ->
                val srcY = y / s.scale
                if (srcX < rawW && srcY < rawH && raw[srcY][srcX] == 1) blockId else null
            }
        }
        _state.update { it.copy(grid = grid, gridW = w, gridH = gh) }
    }

    fun prepareExport() {
        val ctx = context ?: return
        _state.update { it.copy(isUpdating = true, exportPayload = null) }
        viewModelScope.launch {
            val s = _state.value
            val grid = s.grid
            if (grid.isEmpty() || s.gridW == 0) {
                _state.update { it.copy(isUpdating = false) }
                return@launch
            }
            try {
                val (bitmap, materials) = withContext(Dispatchers.IO) {
                    BlockPaintRenderer.renderToBitmap(ctx, grid)
                }
                val ids = mutableListOf<String>()
                for (y in 0 until s.gridH) for (x in 0 until s.gridW) ids.add(grid[x][y] ?: "")
                val encoded = ExportPayloadCodec.encode(
                    bitmap = bitmap,
                    width = s.gridW, height = s.gridH,
                    totalBlocks = materials.values.sum(),
                    materials = materials,
                    ditherMethod = DitherMethod.NONE.id,
                    transparencyEnabled = true, transparencyTolerance = 1,
                    selectedGroups = materials.keys.mapNotNull { id ->
                        BlockCatalog.all.firstOrNull { it.id == id }?.group?.key
                    }.distinct(),
                    sourceWidth = s.gridW, sourceHeight = s.gridH,
                    blockIds = ids,
                )
                _state.update {
                    it.copy(isUpdating = false, resultBitmap = bitmap, resultTotalBlocks = materials.values.sum(), resultMaterialCounts = materials, exportPayload = encoded)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isUpdating = false) }
            }
        }
    }
}
