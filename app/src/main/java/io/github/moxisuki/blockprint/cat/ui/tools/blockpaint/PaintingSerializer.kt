package io.github.moxisuki.blockprint.cat.ui.tools.blockpaint

import io.github.moxisuki.blockprint.cat.data.blockpaint.PaintingEntity

internal fun gridToCsv(grid: Array<Array<String?>>): String {
    val sb = StringBuilder(grid.size * grid[0].size * 8)
    for (x in grid.indices) {
        if (x > 0) sb.append(',')
        for (y in grid[x].indices) {
            if (y > 0) sb.append(',')
            sb.append(grid[x][y] ?: "")
        }
    }
    return sb.toString()
}

internal fun csvToGrid(csv: String, width: Int, height: Int): Array<Array<String?>> {
    val grid = Array(width) { arrayOfNulls<String>(height) }
    val tokens = csv.split(',')
    var idx = 0
    for (x in 0 until width) {
        for (y in 0 until height) {
            val value = tokens.getOrNull(idx) ?: ""
            grid[x][y] = if (value.isEmpty()) null else value
            idx++
        }
    }
    return grid
}

internal fun Painting.toEntity(createdAt: Long = 0L, updatedAt: Long = 0L): PaintingEntity =
    PaintingEntity(
        id = id,
        name = name,
        width = width,
        height = height,
        gridCsv = gridToCsv(grid),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun PaintingEntity.toPainting(): Painting =
    Painting(
        id = id,
        name = name,
        width = width,
        height = height,
        grid = csvToGrid(gridCsv, width, height),
    )
