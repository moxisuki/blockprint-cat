package io.github.moxisuki.blockprint.cat.ui.tools.blockpaint

import android.graphics.Bitmap
import java.util.UUID

/** 画笔 / 橡皮二选一。 */
enum class PaintTool { Paint, Erase }

/** 单张图画的基本数据。id 唯一；grid 是 1×1 方块格子（与 BlockPaintState 一致）。 */
data class Painting(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val width: Int,
    val height: Int,
    val grid: Array<Array<String?>>,
) {
    /** 深拷贝 grid，防止用户切换图画时污染其他引用。 */
    fun deepCopy(): Painting {
        val newGrid = Array(width) { x -> Array(height) { y -> grid[x][y] } }
        return copy(grid = newGrid)
    }

    companion object {
        fun empty(width: Int = BlockPaintState.DEFAULT_SIZE, height: Int = BlockPaintState.DEFAULT_SIZE): Painting =
            Painting(
                name = "未命名",
                width = width,
                height = height,
                grid = Array(width) { arrayOfNulls<String>(height) },
            )
    }
}

data class BlockPaintState(
    val width: Int = DEFAULT_SIZE,
    val height: Int = DEFAULT_SIZE,
    val grid: Array<Array<String?>> = emptyGrid(DEFAULT_SIZE, DEFAULT_SIZE),
    val selectedBlockId: String? = DEFAULT_SELECTED_BLOCK,
    val tool: PaintTool = PaintTool.Paint,
    val isUpdating: Boolean = false,
    val lastUpdatedAt: Long = 0L,
    val resultBitmap: Bitmap? = null,
    val resultWidth: Int = 0,
    val resultHeight: Int = 0,
    val resultTotalBlocks: Int = 0,
    val resultMaterialCounts: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null,
) {
    companion object {
        const val MIN_SIZE = 8
        const val MAX_SIZE = 128
        const val DEFAULT_SIZE = 32

        const val DEFAULT_SELECTED_BLOCK = "white_wool"

        fun emptyGrid(w: Int, h: Int): Array<Array<String?>> =
            Array(w) { arrayOfNulls<String>(h) }
    }
}
