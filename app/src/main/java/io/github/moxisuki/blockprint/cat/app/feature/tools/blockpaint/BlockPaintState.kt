package io.github.moxisuki.blockprint.cat.app.feature.tools.blockpaint

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.feature.tools.BlockPaintTool
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolBlockPalette
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolGrid

@Immutable
internal data class BlockPaintState(
    val grid: ToolGrid = ToolGrid.empty(width = 24, height = 24),
    val selectedBlockId: String = ToolBlockPalette.defaultBlockId,
    val activeTool: BlockPaintTool = BlockPaintTool.Paint,
)
