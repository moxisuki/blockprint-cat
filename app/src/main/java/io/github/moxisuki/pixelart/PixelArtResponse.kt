package io.github.moxisuki.pixelart.api

import android.graphics.Bitmap

data class PixelArtResponse(
    val width: Int,
    val height: Int,
    val totalBlocks: Int,
    val outputImage: Bitmap,
    val materialList: List<BlockEntry>,
    val materialListCsv: String = "",
    val commands: CommandSet? = null,
    val schematicData: SchematicData? = null,
    val elapsedMs: Long = 0
) {
    val uniqueBlockTypes: Int get() = materialList.size

    val mostUsedBlock: BlockEntry?
        get() = materialList.maxByOrNull { it.count }

    fun blockPlacements(
        originX: Int = 0,
        originY: Int = 0,
        originZ: Int = 0,
        direction: String = "north"
    ): Sequence<BlockPlacement> = sequence {
        val (dx, dz) = directionOffsets[direction] ?: Pair(1, 0)
        val blocks: Array<Array<BlockState?>> = _internalBlockGrid
            ?: throw IllegalStateException("Block grid not available.")
        for (y in 0 until height) {
            for (x in 0 until width) {
                val state = blocks[y][x]
                if (state != null && !state.isAir()) {
                    val worldX = originX + x * dx
                    val worldZ = originZ + x * dz
                    val worldY = originY + (height - 1 - y)
                    yield(BlockPlacement(worldX, worldY, worldZ, state))
                }
            }
        }
    }

    fun fillRegions(
        originX: Int = 0,
        originY: Int = 0,
        originZ: Int = 0,
        direction: String = "north"
    ): Sequence<FillRegion> = sequence {
        val (dx, dz) = directionOffsets[direction] ?: Pair(1, 0)
        val blocks: Array<Array<BlockState?>> = _internalBlockGrid
            ?: throw IllegalStateException("Block grid not available.")
        val processed = Array(height) { BooleanArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (processed[y][x]) continue
                val state = blocks[y][x]
                if (state == null || state.isAir()) { processed[y][x] = true; continue }
                val x2 = expandH(blocks, processed, x, y, width, height, state)
                val y2 = expandV(blocks, processed, x, x2, y, height, state)
                for (py in y..y2) for (px in x..x2) processed[py][px] = true

                val wx1 = originX + x * dx; val wz1 = originZ + x * dz
                val wx2 = originX + x2 * dx; val wz2 = originZ + x2 * dz
                val wy1 = originY + (height - 1 - y)
                val wy2 = originY + (height - 1 - y2)
                yield(FillRegion(wx1, wy1, wz1, wx2, wy2, wz2, state))
            }
        }
    }

    internal var _internalBlockGrid: Array<Array<BlockState?>>? = null

    fun summary(): String = buildString {
        appendLine("Pixel Art Conversion Result")
        appendLine("=".repeat(30))
        appendLine("Dimensions: ${width}x${height} ($totalBlocks total blocks)")
        appendLine("Block types: $uniqueBlockTypes")
        appendLine("Time: ${elapsedMs}ms")
        mostUsedBlock?.let {
            appendLine("Most used: ${it.name} (${it.count}, ${(it.count * 100.0 / totalBlocks).toInt()}%)")
        }
        appendLine()
        appendLine("Top 5 blocks:")
        materialList.take(5).forEach { appendLine("  ${it.name}: ${it.count}") }
    }

    companion object {
        val directionOffsets = mapOf(
            "north" to Pair(1, 0), "south" to Pair(-1, 0),
            "east" to Pair(0, 1), "west" to Pair(0, -1)
        )

        private fun expandH(
            blocks: Array<Array<BlockState?>>, processed: Array<BooleanArray>,
            x: Int, y: Int, w: Int, h: Int, target: BlockState
        ): Int {
            var ex = x
            while (ex + 1 < w && blocks[y][ex + 1] == target && !processed[y][ex + 1]) ex++
            return ex
        }

        private fun expandV(
            blocks: Array<Array<BlockState?>>, processed: Array<BooleanArray>,
            x1: Int, x2: Int, y: Int, h: Int, target: BlockState
        ): Int {
            var ey = y
            while (ey + 1 < h) {
                var ok = true
                for (px in x1..x2)
                    if (blocks[ey + 1][px] != target || processed[ey + 1][px]) { ok = false; break }
                if (!ok) break
                ey++
            }
            return ey
        }
    }
}
