package io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview

import android.graphics.Bitmap
import io.github.moxisuki.pixelart.Block
import io.github.moxisuki.pixelart.BlockPalette
import io.github.moxisuki.pixelart.ColorUtils
import io.github.moxisuki.blockprint.core.BlockPalette as CoreBlockPalette
import io.github.moxisuki.blockprint.core.BlockPrintConverter
import io.github.moxisuki.blockprint.core.BlockState as CoreBlockState
import io.github.moxisuki.blockprint.core.Position
import io.github.moxisuki.blockprint.core.SchematicFormat
import io.github.moxisuki.blockprint.core.model.BlockPrintDocument
import io.github.moxisuki.blockprint.core.model.BlockPrintRegion

/**
 * 蓝图平铺方向。源是 2D 图片，故只支持两种几何铺法：
 *   - [WALL] — 贴在 XY 竖直平面上（墙画），尺寸 `width × height × 1`。
 *   - [FLAT] — 平铺在 XZ 水平面上（地画），尺寸 `width × 1 × height`。
 *
 * 3D（带高度差的立体）超出 ITB 范围，跳过。
 */
enum class BlueprintMode { WALL, FLAT }

/**
 * 一次 fill 操作的 [from..to] 立方体 + 方块名，用于上层 fill 优化 API 与单元测试。
 * 与 blockprint-core 内部的 [BlockPrintRegion.setBlock] / rawBlocks 是两条并行路径。
 */
internal data class FillRegion(
    val x1: Int, val y1: Int, val z1: Int,
    val x2: Int, val y2: Int, val z2: Int,
    val blockName: String,
)

/**
 * 把 2D 方块网格装进 blockprint-core 的 [BlockPrintDocument]。
 *
 * 该库没有 BlueprintBuilder 之类的流式 API，只能直接构造 [BlockPrintRegion] + [BlockPrintDocument]。
 * rawBlocks 布局遵循 `index = y * W * D + z * W + x`（y-major）。
 *
 * 因为源图跨 VM 不可访问，grid 由 [bitmapToGrid] 从 result bitmap 采样还原
 * —— 这与 [io.github.moxisuki.pixelart.PixelArtConverter] 在最后一次转换里
 * 给每个像素找最近色块是同一组运算，效果等价。
 */
internal object BlueprintBuilder {

    private const val AIR_PALETTE_INDEX = 0
    private const val MC_PREFIX = "minecraft:"
    private const val DATA_VERSION_1_21 = 3953
    private const val FORMAT_VERSION_6 = 6
    private const val ORIGIN_Y = 64

    /**
     * 重建一个 region 名 "main" 的 [BlockPrintDocument]。
     *
     * @param grid `blocks[y][x]`，空气方块以 null 表示；block.name 不带 `minecraft:` 前缀。
     * @param width 像素宽
     * @param height 像素高
     * @param mode WALL / FLAT 决定 region 第三维是 height 还是 1
     * @param format 目标 schematic 格式
     * @param name / author / description 元数据
     */
    fun buildDocument(
        grid: Array<Array<Block?>>,
        width: Int,
        height: Int,
        mode: BlueprintMode,
        format: SchematicFormat,
        name: String,
        author: String = "BlockPrintCat",
        description: String = "",
    ): BlockPrintDocument {
        require(width > 0 && height > 0) { "grid must be non-empty ($width x $height)" }
        require(grid.size == height) { "grid rows mismatch (${grid.size} != $height)" }

        // palette：[0] = air；之后是 grid 中出现过的所有方块（去重、稳定顺序）。
        val uniqueBlocks = LinkedHashSet<CoreBlockState>()
        for (y in 0 until height) {
            val row = grid[y]
            require(row.size == width) { "grid row $y has ${row.size} cols, expected $width" }
            for (x in 0 until width) {
                val b = row[x] ?: continue
                uniqueBlocks.add(CoreBlockState(MC_PREFIX + b.name, null))
            }
        }
        val paletteEntries = buildList {
            add(CoreBlockState(MC_PREFIX + "air", null)) // 索引 0 必须是空气
            addAll(uniqueBlocks)
        }
        val palette = CoreBlockPalette(paletteEntries)
        val blockToIndex = HashMap<String, Int>(paletteEntries.size)
        paletteEntries.forEachIndexed { idx, st -> blockToIndex[st.name] = idx }

        // 按 mode 决定 region 的三维尺寸，再把 grid 填进 rawBlocks。
        val (W, H, D) = when (mode) {
            BlueprintMode.WALL -> Triple(width, height, 1)
            BlueprintMode.FLAT -> Triple(width, 1, height)
        }
        val raw = IntArray(W * H * D)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val block = grid[y][x]
                val idx = if (block == null) AIR_PALETTE_INDEX else blockToIndex.getValue(MC_PREFIX + block.name)
                val rawIdx = when (mode) {
                    // 竖直贴墙：image y=0 → region y=height-1（墙顶），y=height-1 → y=0（墙底）
                    // 这样站在墙前看（默认朝 -Z 看 +Z），图就是"正"的
                    BlueprintMode.WALL -> (height - 1 - y) * W * D + 0 * W + x
                    // 平铺地面：image y=0 → region z=depth-1（north / 远端）
                    // 默认玩家视角（朝 -Z 看 +Z），图 y=0 在远端 → 看起来就是正的
                    BlueprintMode.FLAT -> 0 * W * D + (height - 1 - y) * W + x
                }
                raw[rawIdx] = idx
            }
        }

        val region = BlockPrintRegion(
            "main",
            W,
            H,
            D,
            Position(0, ORIGIN_Y, 0),
            palette,
            raw,
        )
        return BlockPrintDocument(
            minecraftDataVersion = DATA_VERSION_1_21,
            version = FORMAT_VERSION_6,
            name = name,
            author = author,
            description = description,
            regions = listOf(region),
            format = format,
        )
    }

    /**
     * 用 `BlockPrintConverter` 把 [doc] 序列化为目标 schematic 字节流。
     */
    fun encode(doc: BlockPrintDocument): ByteArray =
        BlockPrintConverter.convert(doc, doc.format)

    /** BlockPaint 直出：从逐格 blockId 列表构建 grid，不做颜色匹配。 */
    fun blockIdsToGrid(blockIds: List<String>, width: Int, height: Int): Array<Array<Block?>> {
        val grid = Array(height) { arrayOfNulls<Block>(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = x + y * width
                val id = blockIds.getOrNull(idx)?.takeIf { it.isNotEmpty() } ?: continue
                val block = BlockPalette.blocks.firstOrNull { it.name == "minecraft:$id" || it.name == id }
                if (block != null) grid[y][x] = block
            }
        }
        return grid
    }

    /**
     * 已经是后处理（dither / brightness / saturation / transparency）的最终结果，
     * 采样 + 最近色匹配能恢复出等价的 grid，无需回传 source。
     */
    fun bitmapToGrid(bitmap: Bitmap): Array<Array<Block?>> {
        val width = bitmap.width
        val height = bitmap.height
        val palette = BlockPalette.blocks
        val grid = Array(height) { arrayOfNulls<Block>(width) }
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val px = pixels[y * width + x]
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                grid[y][x] = findNearestBlock(r, g, b, palette)
            }
        }
        return grid
    }

    /**
     * 同色相邻 cell 在一行内合并成一个 fill。用于单元测试与高层 fill API。
     *
     * @param originY 仅 WALL 模式生效；FLAT 模式固定 0。
     */
    fun fillRegions(
        grid: Array<Array<Block?>>,
        width: Int,
        height: Int,
        mode: BlueprintMode,
        originX: Int = 0,
        originY: Int = ORIGIN_Y,
        originZ: Int = 0,
    ): List<FillRegion> {
        val fills = ArrayList<FillRegion>()
        val processed = Array(height) { BooleanArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (processed[y][x]) continue
                val block = grid[y][x]
                if (block == null) {
                    processed[y][x] = true
                    continue
                }
                var x2 = x
                while (x2 + 1 < width &&
                    grid[y][x2 + 1] != null &&
                    grid[y][x2 + 1]!!.name == block.name &&
                    !processed[y][x2 + 1]
                ) x2++
                for (px in x..x2) processed[y][px] = true

                val regionFill = when (mode) {
                    BlueprintMode.WALL -> FillRegion(
                        x1 = originX + x, y1 = originY + y, z1 = originZ,
                        x2 = originX + x2, y2 = originY + y, z2 = originZ,
                        blockName = MC_PREFIX + block.name,
                    )
                    BlueprintMode.FLAT -> FillRegion(
                        x1 = originX + x, y1 = originY, z1 = originZ + (height - 1 - y),
                        x2 = originX + x2, y2 = originY, z2 = originZ + (height - 1 - y),
                        blockName = MC_PREFIX + block.name,
                    )
                }
                fills.add(regionFill)
            }
        }
        return fills
    }

    private fun findNearestBlock(r: Int, g: Int, b: Int, candidates: List<Block>): Block {
        var best: Block? = null
        var bestDist = Double.MAX_VALUE
        val target = Triple(r, g, b)
        for (candidate in candidates) {
            val dist = ColorUtils.weightedRgbDistance(target, candidate.rgb)
            if (dist < bestDist) {
                bestDist = dist
                best = candidate
            }
        }
        return best ?: error("BlockPalette is empty")
    }
}