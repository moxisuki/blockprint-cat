package io.github.moxisuki.blockprint.cat.ui.category

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/** Palette index → (main, light, dark) trio of hex colors. */
data class CategoryPaletteEntry(val name: String, val main: String, val light: String, val dark: String)

/** 4x4 bitmap; 1 = paint with light color over main background. */
data class CategoryPattern(val name: String, val cells: List<List<Int>>)

object CategoryCover {

    val palette: List<CategoryPaletteEntry> = listOf(
        CategoryPaletteEntry("青苔", "#6f8b3f", "#a8c475", "#4d6428"),
        CategoryPaletteEntry("红石", "#a8362b", "#d26658", "#7a241c"),
        CategoryPaletteEntry("钻石", "#3fa6c4", "#7ccad9", "#277a92"),
        CategoryPaletteEntry("黄金", "#d8a83c", "#ecc771", "#a07f25"),
        CategoryPaletteEntry("紫晶", "#9b5fb8", "#c08eda", "#6e3f87"),
        CategoryPaletteEntry("煤炭", "#3a3a3a", "#5e5e5e", "#1f1f1f"),
        CategoryPaletteEntry("海蓝", "#2f6a8f", "#5994ba", "#1c4863"),
        CategoryPaletteEntry("绯红", "#c4568c", "#dc89b3", "#923860"),
    )

    val patterns: List<CategoryPattern> = listOf(
        CategoryPattern("bricks", listOf(
            listOf(0,0,1,0), listOf(0,0,1,0), listOf(0,0,1,0), listOf(0,0,1,0),
        )),
        CategoryPattern("waves", listOf(
            listOf(1,1,0,0), listOf(0,0,1,1), listOf(1,1,0,0), listOf(0,0,1,1),
        )),
        CategoryPattern("diamond", listOf(
            listOf(0,1,1,0), listOf(1,1,1,1), listOf(1,1,1,1), listOf(0,1,1,0),
        )),
        CategoryPattern("stack", listOf(
            listOf(1,0,1,0), listOf(0,1,0,1), listOf(1,0,1,0), listOf(0,1,0,1),
        )),
        CategoryPattern("block", listOf(
            listOf(1,1,1,1), listOf(1,0,0,1), listOf(1,0,0,1), listOf(1,1,1,1),
        )),
        CategoryPattern("scatter", listOf(
            listOf(1,0,0,1), listOf(0,1,1,0), listOf(0,1,1,0), listOf(1,0,0,1),
        )),
        CategoryPattern("cross", listOf(
            listOf(0,1,1,0), listOf(1,0,0,1), listOf(1,0,0,1), listOf(0,1,1,0),
        )),
        CategoryPattern("grid", listOf(
            listOf(1,1,0,1), listOf(1,1,0,1), listOf(0,0,1,0), listOf(1,1,0,1),
        )),
    )

    fun safeColor(colorIdx: Int): CategoryPaletteEntry =
        palette.getOrElse(colorIdx.coerceIn(0, palette.lastIndex)) { palette.first() }

    fun safePattern(patternIdx: Int): CategoryPattern =
        patterns.getOrElse(patternIdx.coerceIn(0, patterns.lastIndex)) { patterns.first() }
}

@Composable
fun CategoryCoverView(
    colorIdx: Int,
    patternIdx: Int,
    modifier: Modifier = Modifier,
) {
    val palette = CategoryCover.safeColor(colorIdx)
    val pattern = CategoryCover.safePattern(patternIdx)
    val bg = Color(android.graphics.Color.parseColor(palette.main))
    val fg = Color(android.graphics.Color.parseColor(palette.light))
    Canvas(modifier = modifier) {
        val cellW = size.width / 4f
        val cellH = size.height / 4f
        // Background
        drawRect(color = bg, topLeft = Offset.Zero, size = size)
        // Foreground cells
        pattern.cells.forEachIndexed { rowIdx, row ->
            row.forEachIndexed { colIdx, cell ->
                if (cell == 1) {
                    drawRect(
                        color = fg.copy(alpha = 0.85f),
                        topLeft = Offset(colIdx * cellW, rowIdx * cellH),
                        size = Size(cellW, cellH),
                    )
                }
            }
        }
    }
}