package io.github.moxisuki.blockprint.cat.app.feature.tools.blockpaint

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.feature.tools.BlockPaintTool
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolBlockPalette
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolGrid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
internal class BlockPaintViewModel @Inject constructor() : ViewModel() {
    private val mutableState = MutableStateFlow(BlockPaintState())
    val state: StateFlow<BlockPaintState> = mutableState

    fun selectBlock(blockId: String) {
        if (ToolBlockPalette.block(blockId) == null) return
        mutableState.update { it.copy(selectedBlockId = blockId, activeTool = BlockPaintTool.Paint) }
    }

    fun selectTool(tool: BlockPaintTool) {
        mutableState.update { it.copy(activeTool = tool) }
    }

    fun resize(width: Int? = null, height: Int? = null) {
        mutableState.update { state ->
            val newWidth = (width ?: state.grid.width).coerceIn(4, 96)
            val newHeight = (height ?: state.grid.height).coerceIn(4, 96)
            if (newWidth == state.grid.width && newHeight == state.grid.height) return@update state
            val newCells = List(newWidth * newHeight) { index ->
                val x = index % newWidth
                val y = index / newWidth
                state.grid.cellAt(x, y)
            }
            state.copy(grid = ToolGrid(newWidth, newHeight, newCells))
        }
    }

    fun paint(x: Int, y: Int) {
        mutableState.update { state ->
            val blockId = when (state.activeTool) {
                BlockPaintTool.Paint -> state.selectedBlockId
                BlockPaintTool.Erase -> null
            }
            state.copy(grid = state.grid.withCell(x, y, blockId))
        }
    }

    fun clear() {
        mutableState.update { it.copy(grid = ToolGrid.empty(it.grid.width, it.grid.height)) }
    }
}
