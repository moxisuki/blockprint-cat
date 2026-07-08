package io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon

import android.net.Uri
import androidx.annotation.DrawableRes
import io.github.moxisuki.blockprint.cat.R

/**
 * 方块库与分组模型 + UI 预览模式枚举。
 *
 * 抽到这里是为了让「图片转蓝图画」「文字转蓝图画」共用同一份：
 *   - 11 个 BlockGroup
 *   - 140+ 个 BlockEntry（含 drawableResId）
 *   - 1 个 PreviewMode（Source / Result，决定 Hero 怎么 Crossfade）
 *
 * 渲染细节（亮度/对比度/饱和度/抖动）仍归各工具的 ViewModel / State 自己持有，
 * 因为 ITB/TTB 的可调参数并不完全一致。
 */
enum class DitherMethod(val id: Int, val labelRes: Int) {
    NONE(0, R.string.itb_dither_none),
    FLOYD_STEINBERG(1, R.string.itb_dither_floyd_steinberg),
    BAYER_4X4(2, R.string.itb_dither_bayer_4x4),
    BAYER_2X2(3, R.string.itb_dither_bayer_2x2),
    ORDERED_3X3(4, R.string.itb_dither_ordered_3x3),
    MIN_AVG_ERR(5, R.string.itb_dither_min_avg_err),
    BURKES(6, R.string.itb_dither_burkes),
    SIERRA_LITE(7, R.string.itb_dither_sierra_lite),
    STUCKI(8, R.string.itb_dither_stucki),
    ATKINSON(9, R.string.itb_dither_atkinson);

    companion object {
        val DEFAULT = FLOYD_STEINBERG
    }
}

enum class BlockGroup(val key: String, val labelRes: Int) {
    WOOL("wool", R.string.itb_group_wool),
    CONCRETE("concrete", R.string.itb_group_concrete),
    TERRACOTTA("terracotta", R.string.itb_group_terracotta),
    STONE("stone", R.string.itb_group_stone),
    SOIL("soil", R.string.itb_group_soil),
    WOOD("wood", R.string.itb_group_wood),
    JEWEL("jewel", R.string.itb_group_jewel),
    GLASS("glass", R.string.itb_group_glass),
    GLAZED("glazed", R.string.itb_group_glazed),
    LIGHT("light", R.string.itb_group_light),
    ORE("ore", R.string.itb_group_ore);
}

data class BlockEntry(
    val id: String,
    val group: BlockGroup,
    @field:DrawableRes val drawableResId: Int,
) {
    val displayName: String get() = id.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
}

object BlockCatalog {
    val all: List<BlockEntry> = listOf(
        // Wool
        BlockEntry("white_wool", BlockGroup.WOOL, R.drawable.white_wool),
        BlockEntry("orange_wool", BlockGroup.WOOL, R.drawable.orange_wool),
        BlockEntry("magenta_wool", BlockGroup.WOOL, R.drawable.magenta_wool),
        BlockEntry("light_blue_wool", BlockGroup.WOOL, R.drawable.light_blue_wool),
        BlockEntry("yellow_wool", BlockGroup.WOOL, R.drawable.yellow_wool),
        BlockEntry("lime_wool", BlockGroup.WOOL, R.drawable.lime_wool),
        BlockEntry("pink_wool", BlockGroup.WOOL, R.drawable.pink_wool),
        BlockEntry("gray_wool", BlockGroup.WOOL, R.drawable.gray_wool),
        BlockEntry("light_gray_wool", BlockGroup.WOOL, R.drawable.light_gray_wool),
        BlockEntry("cyan_wool", BlockGroup.WOOL, R.drawable.cyan_wool),
        BlockEntry("purple_wool", BlockGroup.WOOL, R.drawable.purple_wool),
        BlockEntry("blue_wool", BlockGroup.WOOL, R.drawable.blue_wool),
        BlockEntry("brown_wool", BlockGroup.WOOL, R.drawable.brown_wool),
        BlockEntry("green_wool", BlockGroup.WOOL, R.drawable.green_wool),
        BlockEntry("red_wool", BlockGroup.WOOL, R.drawable.red_wool),
        BlockEntry("black_wool", BlockGroup.WOOL, R.drawable.black_wool),

        // Concrete
        BlockEntry("white_concrete", BlockGroup.CONCRETE, R.drawable.white_concrete),
        BlockEntry("orange_concrete", BlockGroup.CONCRETE, R.drawable.orange_concrete),
        BlockEntry("magenta_concrete", BlockGroup.CONCRETE, R.drawable.magenta_concrete),
        BlockEntry("light_blue_concrete", BlockGroup.CONCRETE, R.drawable.light_blue_concrete),
        BlockEntry("yellow_concrete", BlockGroup.CONCRETE, R.drawable.yellow_concrete),
        BlockEntry("lime_concrete", BlockGroup.CONCRETE, R.drawable.lime_concrete),
        BlockEntry("pink_concrete", BlockGroup.CONCRETE, R.drawable.pink_concrete),
        BlockEntry("gray_concrete", BlockGroup.CONCRETE, R.drawable.gray_concrete),
        BlockEntry("light_gray_concrete", BlockGroup.CONCRETE, R.drawable.light_gray_concrete),
        BlockEntry("cyan_concrete", BlockGroup.CONCRETE, R.drawable.cyan_concrete),
        BlockEntry("purple_concrete", BlockGroup.CONCRETE, R.drawable.purple_concrete),
        BlockEntry("blue_concrete", BlockGroup.CONCRETE, R.drawable.blue_concrete),
        BlockEntry("brown_concrete", BlockGroup.CONCRETE, R.drawable.brown_concrete),
        BlockEntry("green_concrete", BlockGroup.CONCRETE, R.drawable.green_concrete),
        BlockEntry("red_concrete", BlockGroup.CONCRETE, R.drawable.red_concrete),
        BlockEntry("black_concrete", BlockGroup.CONCRETE, R.drawable.black_concrete),

        // Terracotta (stained)
        BlockEntry("white_terracotta", BlockGroup.TERRACOTTA, R.drawable.white_terracotta),
        BlockEntry("orange_terracotta", BlockGroup.TERRACOTTA, R.drawable.orange_terracotta),
        BlockEntry("magenta_terracotta", BlockGroup.TERRACOTTA, R.drawable.magenta_terracotta),
        BlockEntry("light_blue_terracotta", BlockGroup.TERRACOTTA, R.drawable.light_blue_terracotta),
        BlockEntry("yellow_terracotta", BlockGroup.TERRACOTTA, R.drawable.yellow_terracotta),
        BlockEntry("lime_terracotta", BlockGroup.TERRACOTTA, R.drawable.lime_terracotta),
        BlockEntry("pink_terracotta", BlockGroup.TERRACOTTA, R.drawable.pink_terracotta),
        BlockEntry("gray_terracotta", BlockGroup.TERRACOTTA, R.drawable.gray_terracotta),
        BlockEntry("light_gray_terracotta", BlockGroup.TERRACOTTA, R.drawable.light_gray_terracotta),
        BlockEntry("cyan_terracotta", BlockGroup.TERRACOTTA, R.drawable.cyan_terracotta),
        BlockEntry("purple_terracotta", BlockGroup.TERRACOTTA, R.drawable.purple_terracotta),
        BlockEntry("blue_terracotta", BlockGroup.TERRACOTTA, R.drawable.blue_terracotta),
        BlockEntry("brown_terracotta", BlockGroup.TERRACOTTA, R.drawable.brown_terracotta),
        BlockEntry("green_terracotta", BlockGroup.TERRACOTTA, R.drawable.green_terracotta),
        BlockEntry("red_terracotta", BlockGroup.TERRACOTTA, R.drawable.red_terracotta),
        BlockEntry("black_terracotta", BlockGroup.TERRACOTTA, R.drawable.black_terracotta),
        BlockEntry("clay", BlockGroup.TERRACOTTA, R.drawable.clay),

        // Glazed Terracotta
        BlockEntry("white_glazed_terracotta", BlockGroup.GLAZED, R.drawable.white_glazed_terracotta),
        BlockEntry("orange_glazed_terracotta", BlockGroup.GLAZED, R.drawable.orange_glazed_terracotta),
        BlockEntry("magenta_glazed_terracotta", BlockGroup.GLAZED, R.drawable.magenta_glazed_terracotta),
        BlockEntry("light_blue_glazed_terracotta", BlockGroup.GLAZED, R.drawable.light_blue_glazed_terracotta),
        BlockEntry("yellow_glazed_terracotta", BlockGroup.GLAZED, R.drawable.yellow_glazed_terracotta),
        BlockEntry("lime_glazed_terracotta", BlockGroup.GLAZED, R.drawable.lime_glazed_terracotta),
        BlockEntry("pink_glazed_terracotta", BlockGroup.GLAZED, R.drawable.pink_glazed_terracotta),
        BlockEntry("gray_glazed_terracotta", BlockGroup.GLAZED, R.drawable.gray_glazed_terracotta),
        BlockEntry("light_gray_glazed_terracotta", BlockGroup.GLAZED, R.drawable.light_gray_glazed_terracotta),
        BlockEntry("cyan_glazed_terracotta", BlockGroup.GLAZED, R.drawable.cyan_glazed_terracotta),
        BlockEntry("purple_glazed_terracotta", BlockGroup.GLAZED, R.drawable.purple_glazed_terracotta),
        BlockEntry("blue_glazed_terracotta", BlockGroup.GLAZED, R.drawable.blue_glazed_terracotta),
        BlockEntry("brown_glazed_terracotta", BlockGroup.GLAZED, R.drawable.brown_glazed_terracotta),
        BlockEntry("green_glazed_terracotta", BlockGroup.GLAZED, R.drawable.green_glazed_terracotta),
        BlockEntry("red_glazed_terracotta", BlockGroup.GLAZED, R.drawable.red_glazed_terracotta),
        BlockEntry("black_glazed_terracotta", BlockGroup.GLAZED, R.drawable.black_glazed_terracotta),

        // Stone
        BlockEntry("stone", BlockGroup.STONE, R.drawable.stone),
        BlockEntry("cobblestone", BlockGroup.STONE, R.drawable.cobblestone),
        BlockEntry("mossy_cobblestone", BlockGroup.STONE, R.drawable.mossy_cobblestone),
        BlockEntry("andesite", BlockGroup.STONE, R.drawable.andesite),
        BlockEntry("polished_andesite", BlockGroup.STONE, R.drawable.polished_andesite),
        BlockEntry("diorite", BlockGroup.STONE, R.drawable.diorite),
        BlockEntry("polished_diorite", BlockGroup.STONE, R.drawable.polished_diorite),
        BlockEntry("granite", BlockGroup.STONE, R.drawable.granite),
        BlockEntry("polished_granite", BlockGroup.STONE, R.drawable.polished_granite),
        BlockEntry("stone_bricks", BlockGroup.STONE, R.drawable.stone_bricks),
        BlockEntry("mossy_stone_bricks", BlockGroup.STONE, R.drawable.mossy_stone_bricks),
        BlockEntry("chiseled_stone_bricks", BlockGroup.STONE, R.drawable.chiseled_stone_bricks),
        BlockEntry("sandstone", BlockGroup.STONE, R.drawable.sandstone),
        BlockEntry("smooth_sandstone", BlockGroup.STONE, R.drawable.smooth_sandstone),
        BlockEntry("nether_bricks", BlockGroup.STONE, R.drawable.nether_bricks),
        BlockEntry("obsidian", BlockGroup.STONE, R.drawable.obsidian),
        BlockEntry("quartz_block", BlockGroup.STONE, R.drawable.quartz_block),

        // Wood
        BlockEntry("oak_planks", BlockGroup.WOOD, R.drawable.oak_planks),
        BlockEntry("spruce_planks", BlockGroup.WOOD, R.drawable.spruce_planks),
        BlockEntry("birch_planks", BlockGroup.WOOD, R.drawable.birch_planks),
        BlockEntry("jungle_planks", BlockGroup.WOOD, R.drawable.jungle_planks),
        BlockEntry("acacia_planks", BlockGroup.WOOD, R.drawable.acacia_planks),
        BlockEntry("dark_oak_planks", BlockGroup.WOOD, R.drawable.dark_oak_planks),
        BlockEntry("oak_log", BlockGroup.WOOD, R.drawable.oak_log),
        BlockEntry("spruce_log", BlockGroup.WOOD, R.drawable.spruce_log),
        BlockEntry("birch_log", BlockGroup.WOOD, R.drawable.birch_log),
        BlockEntry("jungle_log", BlockGroup.WOOD, R.drawable.jungle_log),
        BlockEntry("acacia_log", BlockGroup.WOOD, R.drawable.acacia_log),
        BlockEntry("dark_oak_log", BlockGroup.WOOD, R.drawable.dark_oak_log),
        BlockEntry("crimson_planks", BlockGroup.WOOD, R.drawable.crimson_planks),
        BlockEntry("warped_planks", BlockGroup.WOOD, R.drawable.warped_planks),

        // Soil
        BlockEntry("dirt", BlockGroup.SOIL, R.drawable.dirt),
        BlockEntry("netherrack", BlockGroup.SOIL, R.drawable.netherrack),
        BlockEntry("prismarine_bricks", BlockGroup.SOIL, R.drawable.prismarine_bricks),
        BlockEntry("dark_prismarine", BlockGroup.SOIL, R.drawable.dark_prismarine),

        // Jewel
        BlockEntry("diamond_block", BlockGroup.JEWEL, R.drawable.diamond_block),
        BlockEntry("emerald_block", BlockGroup.JEWEL, R.drawable.emerald_block),
        BlockEntry("gold_block", BlockGroup.JEWEL, R.drawable.gold_block),
        BlockEntry("lapis_block", BlockGroup.JEWEL, R.drawable.lapis_block),
        BlockEntry("redstone_block", BlockGroup.JEWEL, R.drawable.redstone_block),
        BlockEntry("quartz_block_jewel", BlockGroup.JEWEL, R.drawable.quartz_block),
        BlockEntry("obsidian_jewel", BlockGroup.JEWEL, R.drawable.obsidian),

        // Light
        BlockEntry("glowstone", BlockGroup.LIGHT, R.drawable.glowstone),
        BlockEntry("sea_lantern", BlockGroup.LIGHT, R.drawable.sea_lantern),
        BlockEntry("shroomlight", BlockGroup.LIGHT, R.drawable.shroomlight),
        BlockEntry("jack_o_lantern", BlockGroup.LIGHT, R.drawable.jack_o_lantern),

        // Ore
        BlockEntry("coal_ore", BlockGroup.ORE, R.drawable.coal_ore),
        BlockEntry("iron_ore", BlockGroup.ORE, R.drawable.iron_ore),
        BlockEntry("gold_ore", BlockGroup.ORE, R.drawable.gold_ore),
        BlockEntry("diamond_ore", BlockGroup.ORE, R.drawable.diamond_ore),
        BlockEntry("emerald_ore", BlockGroup.ORE, R.drawable.emerald_ore),
        BlockEntry("redstone_ore", BlockGroup.ORE, R.drawable.redstone_ore),
        BlockEntry("lapis_ore", BlockGroup.ORE, R.drawable.lapis_ore),
        BlockEntry("copper_ore", BlockGroup.ORE, R.drawable.copper_ore),
        BlockEntry("quartz_ore", BlockGroup.ORE, R.drawable.quartz_ore),
        BlockEntry("deepslate_coal_ore", BlockGroup.ORE, R.drawable.deepslate_coal_ore),
        BlockEntry("deepslate_iron_ore", BlockGroup.ORE, R.drawable.deepslate_iron_ore),
        BlockEntry("deepslate_gold_ore", BlockGroup.ORE, R.drawable.deepslate_gold_ore),
        BlockEntry("deepslate_diamond_ore", BlockGroup.ORE, R.drawable.deepslate_diamond_ore),
        BlockEntry("deepslate_emerald_ore", BlockGroup.ORE, R.drawable.deepslate_emerald_ore),
        BlockEntry("deepslate_redstone_ore", BlockGroup.ORE, R.drawable.deepslate_redstone_ore),
        BlockEntry("deepslate_lapis_ore", BlockGroup.ORE, R.drawable.deepslate_lapis_ore),

        // Glass
        BlockEntry("glass", BlockGroup.GLASS, R.drawable.glass),
        BlockEntry("white_stained_glass", BlockGroup.GLASS, R.drawable.white_stained_glass),
        BlockEntry("orange_stained_glass", BlockGroup.GLASS, R.drawable.orange_stained_glass),
        BlockEntry("magenta_stained_glass", BlockGroup.GLASS, R.drawable.magenta_stained_glass),
        BlockEntry("light_blue_stained_glass", BlockGroup.GLASS, R.drawable.light_blue_stained_glass),
        BlockEntry("yellow_stained_glass", BlockGroup.GLASS, R.drawable.yellow_stained_glass),
        BlockEntry("lime_stained_glass", BlockGroup.GLASS, R.drawable.lime_stained_glass),
        BlockEntry("pink_stained_glass", BlockGroup.GLASS, R.drawable.pink_stained_glass),
        BlockEntry("gray_stained_glass", BlockGroup.GLASS, R.drawable.gray_stained_glass),
        BlockEntry("light_gray_stained_glass", BlockGroup.GLASS, R.drawable.light_gray_stained_glass),
        BlockEntry("cyan_stained_glass", BlockGroup.GLASS, R.drawable.cyan_stained_glass),
        BlockEntry("purple_stained_glass", BlockGroup.GLASS, R.drawable.purple_stained_glass),
        BlockEntry("blue_stained_glass", BlockGroup.GLASS, R.drawable.blue_stained_glass),
        BlockEntry("brown_stained_glass", BlockGroup.GLASS, R.drawable.brown_stained_glass),
        BlockEntry("green_stained_glass", BlockGroup.GLASS, R.drawable.green_stained_glass),
        BlockEntry("red_stained_glass", BlockGroup.GLASS, R.drawable.red_stained_glass),
        BlockEntry("black_stained_glass", BlockGroup.GLASS, R.drawable.black_stained_glass),
    )

    val byGroup: Map<BlockGroup, List<BlockEntry>> = all.groupBy { it.group }
}

/**
 * UI 预览模式枚举。Hero 组件根据该值 Crossfade 到对应的内容。
 *
 * Source —— 工具的原始输入（图片 / 文字渲染图）。
 * Result —— 经过转换器处理后的方块预览。
 */
enum class PreviewMode { Source, Result }

/**
 * 各工具的 UI 通用常量。最早来自 ITB，TTB 沿用同一组。
 */
object BlueprintUiDefaults {
    const val MIN_ADJUST = 0
    const val MAX_ADJUST = 300
    const val DEFAULT_ADJUST = 100
    const val MIN_WIDTH = 16
    const val MAX_WIDTH = 2048
    const val DEFAULT_WIDTH = 128
    const val MIN_TOLERANCE = 0
    const val MAX_TOLERANCE = 255
    const val DEFAULT_TOLERANCE = 128

    const val MIN_FONT_SIZE = 12f
    const val MAX_FONT_SIZE = 48f
    const val DEFAULT_FONT_SIZE = 24f

    /** 默认勾选的方块组（wool/concrete/terracotta/stone/wood 5 组）。 */
    val DEFAULT_GROUPS: Set<BlockGroup> = setOf(
        BlockGroup.WOOL,
        BlockGroup.CONCRETE,
        BlockGroup.TERRACOTTA,
        BlockGroup.STONE,
        BlockGroup.WOOD,
    )

    /**
     * 文字渲染时把文本塞进画布的"目标行宽"——超过这个像素数会强制换行。
     * 越大单行能塞下更多字符，最终转换方块数也越多。1000 ≈ 单行 ~50 字符。
     */
    const val TEXT_MAX_WIDTH_PX = 1000

    /**
     * TTB 把文字渲染成黑白小图后喂给 engine，背景的 RGB 值。
     * 文字本身用纯黑 (0xFF000000)，背景用纯白 (0xFFFFFFFF)，让 engine 正常识别前景/背景。
     */
    const val TEXT_FOREGROUND_ARGB = 0xFF000000.toInt()
    const val TEXT_BACKGROUND_ARGB = 0xFFFFFFFF.toInt()
}
