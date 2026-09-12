package io.github.moxisuki.blockprint.cat.app.feature.tools

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.moxisuki.blockprint.cat.R

internal enum class ToolDestination {
    ImageToBlueprint,
    TextToBlueprint,
    BlockPaint,
}

internal enum class BlockPaintTool {
    Paint,
    Erase,
}

internal enum class ToolPreviewMode {
    Source,
    Result,
}

internal enum class ToolDitherMethod {
    None,
    FloydSteinberg,
    Bayer2x2,
    Bayer4x4,
}

@Immutable
internal data class ToolEntryUi(
    val destination: ToolDestination,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val accent: Color,
)

internal val ToolEntries = listOf(
    ToolEntryUi(
        destination = ToolDestination.ImageToBlueprint,
        titleRes = R.string.tool_image_to_blueprint,
        subtitleRes = R.string.tool_image_to_blueprint_subtitle,
        accent = Color(0xFF3F8CFF),
    ),
    ToolEntryUi(
        destination = ToolDestination.TextToBlueprint,
        titleRes = R.string.tool_text_to_blueprint,
        subtitleRes = R.string.tool_text_to_blueprint_subtitle,
        accent = Color(0xFF13A891),
    ),
    ToolEntryUi(
        destination = ToolDestination.BlockPaint,
        titleRes = R.string.tool_block_paint,
        subtitleRes = R.string.tool_block_paint_subtitle,
        accent = Color(0xFFE09A28),
    ),
)

@Immutable
internal data class ToolBlock(
    val id: String,
    val namespaceId: String,
    val color: Color,
    @DrawableRes val drawableResId: Int,
) {
    val displayName: String
        get() = id.split("_").joinToString(" ") { part ->
            part.replaceFirstChar { it.uppercaseChar() }
        }
}

@Immutable
internal data class ToolBlockGroup(
    @StringRes val labelRes: Int,
    val blocks: List<ToolBlock>,
)

@Immutable
internal data class ToolGrid(
    val width: Int,
    val height: Int,
    val cells: List<String?>,
) {
    fun cellAt(x: Int, y: Int): String? =
        if (x in 0 until width && y in 0 until height) cells[y * width + x] else null

    val paintedCount: Int
        get() = cells.count { it != null }

    val materialCounts: List<ToolMaterialCount>
        get() = cells.filterNotNull()
            .groupingBy { it }
            .eachCount()
            .map { (id, count) -> ToolMaterialCount(id, count) }
            .sortedByDescending { it.count }

    fun withCell(x: Int, y: Int, blockId: String?): ToolGrid {
        if (x !in 0 until width || y !in 0 until height) return this
        val index = y * width + x
        if (cells[index] == blockId) return this
        return copy(cells = cells.toMutableList().also { it[index] = blockId })
    }

    companion object {
        fun empty(width: Int, height: Int): ToolGrid =
            ToolGrid(
                width = width.coerceIn(1, 256),
                height = height.coerceIn(1, 256),
                cells = List(width.coerceIn(1, 256) * height.coerceIn(1, 256)) { null },
            )
    }
}

@Immutable
internal data class ToolMaterialCount(
    val blockId: String,
    val count: Int,
)

internal object ToolBlockPalette {
    val blocks = listOf(
        ToolBlock("white_wool", "minecraft:white_wool", Color(0xFFF1F1E6), R.drawable.white_wool),
        ToolBlock("light_gray_wool", "minecraft:light_gray_wool", Color(0xFF9D9D97), R.drawable.light_gray_wool),
        ToolBlock("gray_wool", "minecraft:gray_wool", Color(0xFF414141), R.drawable.gray_wool),
        ToolBlock("black_wool", "minecraft:black_wool", Color(0xFF171616), R.drawable.black_wool),
        ToolBlock("red_wool", "minecraft:red_wool", Color(0xFFA92C22), R.drawable.red_wool),
        ToolBlock("orange_wool", "minecraft:orange_wool", Color(0xFFF07613), R.drawable.orange_wool),
        ToolBlock("yellow_wool", "minecraft:yellow_wool", Color(0xFFF8C627), R.drawable.yellow_wool),
        ToolBlock("lime_wool", "minecraft:lime_wool", Color(0xFF70B919), R.drawable.lime_wool),
        ToolBlock("green_wool", "minecraft:green_wool", Color(0xFF556E1E), R.drawable.green_wool),
        ToolBlock("cyan_wool", "minecraft:cyan_wool", Color(0xFF158991), R.drawable.cyan_wool),
        ToolBlock("light_blue_wool", "minecraft:light_blue_wool", Color(0xFF3AAFD9), R.drawable.light_blue_wool),
        ToolBlock("blue_wool", "minecraft:blue_wool", Color(0xFF2E388D), R.drawable.blue_wool),
        ToolBlock("purple_wool", "minecraft:purple_wool", Color(0xFF7E3DB5), R.drawable.purple_wool),
        ToolBlock("magenta_wool", "minecraft:magenta_wool", Color(0xFFC74EBD), R.drawable.magenta_wool),
        ToolBlock("pink_wool", "minecraft:pink_wool", Color(0xFFED8DAC), R.drawable.pink_wool),
        ToolBlock("brown_wool", "minecraft:brown_wool", Color(0xFF724728), R.drawable.brown_wool),
        ToolBlock("stone", "minecraft:stone", Color(0xFF7E7E7E), R.drawable.stone),
        ToolBlock("cobblestone", "minecraft:cobblestone", Color(0xFF747474), R.drawable.cobblestone),
        ToolBlock("stone_bricks", "minecraft:stone_bricks", Color(0xFF777876), R.drawable.stone_bricks),
        ToolBlock("oak_planks", "minecraft:oak_planks", Color(0xFFA77A43), R.drawable.oak_planks),
        ToolBlock("spruce_planks", "minecraft:spruce_planks", Color(0xFF6B4E2A), R.drawable.spruce_planks),
        ToolBlock("birch_planks", "minecraft:birch_planks", Color(0xFFC7B27A), R.drawable.birch_planks),
        ToolBlock("glass", "minecraft:glass", Color(0xFFA8D9D9), R.drawable.glass),
        ToolBlock("glowstone", "minecraft:glowstone", Color(0xFFC7A74A), R.drawable.glowstone),
        ToolBlock("sea_lantern", "minecraft:sea_lantern", Color(0xFFB8D6CC), R.drawable.sea_lantern),
        ToolBlock("gold_block", "minecraft:gold_block", Color(0xFFF7D34B), R.drawable.gold_block),
        ToolBlock("diamond_block", "minecraft:diamond_block", Color(0xFF61D7D5), R.drawable.diamond_block),
        ToolBlock("emerald_block", "minecraft:emerald_block", Color(0xFF2FB65A), R.drawable.emerald_block),
        ToolBlock("lapis_block", "minecraft:lapis_block", Color(0xFF2148A8), R.drawable.lapis_block),
        ToolBlock("redstone_block", "minecraft:redstone_block", Color(0xFFB51D16), R.drawable.redstone_block),
    )

    val defaultBlockId = "white_wool"

    val groups: List<ToolBlockGroup> = listOf(
        ToolBlockGroup(R.string.itb_group_wool, blocks.filter { it.id.endsWith("_wool") }),
        ToolBlockGroup(
            R.string.itb_group_stone,
            blocks.filter { it.id in setOf("stone", "cobblestone", "stone_bricks") },
        ),
        ToolBlockGroup(
            R.string.itb_group_wood,
            blocks.filter { it.id.endsWith("_planks") },
        ),
        ToolBlockGroup(
            R.string.itb_group_glass,
            blocks.filter { it.id == "glass" },
        ),
        ToolBlockGroup(
            R.string.itb_group_light,
            blocks.filter { it.id in setOf("glowstone", "sea_lantern") },
        ),
        ToolBlockGroup(
            R.string.itb_group_jewel,
            blocks.filter { it.id.endsWith("_block") && it.id !in setOf("redstone_block") } +
                blocks.filter { it.id == "redstone_block" },
        ),
    ).filter { it.blocks.isNotEmpty() }

    private val blocksById: Map<String, ToolBlock> by lazy { blocks.associateBy { it.id } }

    private val colorComponents: FloatArray by lazy {
        FloatArray(blocks.size * 3).also { components ->
            blocks.forEachIndexed { index, block ->
                components[index * 3] = block.color.red
                components[index * 3 + 1] = block.color.green
                components[index * 3 + 2] = block.color.blue
            }
        }
    }

    fun block(id: String?): ToolBlock? = id?.let(blocksById::get)

    fun nearestIndex(red: Float, green: Float, blue: Float): Int {
        val components = colorComponents
        var bestIndex = 0
        var bestDistance = Float.POSITIVE_INFINITY
        for (i in blocks.indices) {
            val base = i * 3
            val dr = red - components[base]
            val dg = green - components[base + 1]
            val db = blue - components[base + 2]
            val distance = dr * dr + dg * dg + db * db
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = i
            }
        }
        return bestIndex
    }

    fun nearest(color: Color): ToolBlock =
        blocks[nearestIndex(color.red, color.green, color.blue)]
}
