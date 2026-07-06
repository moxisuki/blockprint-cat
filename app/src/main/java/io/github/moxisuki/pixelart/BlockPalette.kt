package io.github.moxisuki.pixelart

/**
 * 内置方块色板，包含 167 种常用方块的 RGB 颜色。
 *
 * 属性标签（survivalObtainable / luminance / redstone / falling / transparent）
 * 通过 [blocks] 的 lazy 初始化自动推断，无需手动指定。
 */
object BlockPalette {

    private val rawBlocks: List<Block> = listOf(
        // Wool
        Block("white_wool", Triple(233, 236, 237), "wool"),
        Block("light_gray_wool", Triple(143, 142, 125), "wool"),
        Block("gray_wool", Triple(63, 68, 72), "wool"),
        Block("black_wool", Triple(25, 23, 28), "wool"),
        Block("brown_wool", Triple(115, 76, 45), "wool"),
        Block("red_wool", Triple(162, 47, 38), "wool"),
        Block("orange_wool", Triple(242, 131, 36), "wool"),
        Block("yellow_wool", Triple(250, 219, 36), "wool"),
        Block("lime_wool", Triple(123, 200, 43), "wool"),
        Block("green_wool", Triple(84, 110, 27), "wool"),
        Block("cyan_wool", Triple(30, 140, 166), "wool"),
        Block("light_blue_wool", Triple(102, 152, 224), "wool"),
        Block("blue_wool", Triple(53, 57, 160), "wool"),
        Block("purple_wool", Triple(126, 53, 153), "wool"),
        Block("magenta_wool", Triple(196, 85, 183), "wool"),
        Block("pink_wool", Triple(245, 156, 168), "wool"),

        // Concrete
        Block("white_concrete", Triple(208, 214, 215), "concrete"),
        Block("light_gray_concrete", Triple(125, 125, 115), "concrete"),
        Block("gray_concrete", Triple(55, 59, 62), "concrete"),
        Block("black_concrete", Triple(8, 10, 15), "concrete"),
        Block("brown_concrete", Triple(97, 61, 34), "concrete"),
        Block("red_concrete", Triple(143, 33, 33), "concrete"),
        Block("orange_concrete", Triple(226, 99, 2), "concrete"),
        Block("yellow_concrete", Triple(242, 176, 21), "concrete"),
        Block("lime_concrete", Triple(95, 170, 25), "concrete"),
        Block("green_concrete", Triple(74, 92, 36), "concrete"),
        Block("cyan_concrete", Triple(21, 120, 136), "concrete"),
        Block("light_blue_concrete", Triple(36, 137, 199), "concrete"),
        Block("blue_concrete", Triple(46, 48, 144), "concrete"),
        Block("purple_concrete", Triple(102, 32, 156), "concrete"),
        Block("magenta_concrete", Triple(171, 50, 162), "concrete"),
        Block("pink_concrete", Triple(215, 102, 133), "concrete"),

        // Terracotta
        Block("white_terracotta", Triple(210, 179, 163), "terracotta"),
        Block("light_gray_terracotta", Triple(135, 107, 98), "terracotta"),
        Block("gray_terracotta", Triple(58, 42, 36), "terracotta"),
        Block("black_terracotta", Triple(38, 23, 16), "terracotta"),
        Block("brown_terracotta", Triple(78, 52, 36), "terracotta"),
        Block("red_terracotta", Triple(143, 62, 46), "terracotta"),
        Block("orange_terracotta", Triple(165, 84, 47), "terracotta"),
        Block("yellow_terracotta", Triple(186, 134, 35), "terracotta"),
        Block("lime_terracotta", Triple(104, 118, 53), "terracotta"),
        Block("green_terracotta", Triple(77, 83, 50), "terracotta"),
        Block("cyan_terracotta", Triple(87, 91, 91), "terracotta"),
        Block("light_blue_terracotta", Triple(114, 109, 138), "terracotta"),
        Block("blue_terracotta", Triple(75, 60, 92), "terracotta"),
        Block("purple_terracotta", Triple(120, 70, 87), "terracotta"),
        Block("magenta_terracotta", Triple(151, 89, 108), "terracotta"),
        Block("pink_terracotta", Triple(163, 113, 102), "terracotta"),

        // Concrete Powder
        Block("white_concrete_powder", Triple(207, 213, 214), "concrete"),
        Block("light_gray_concrete_powder", Triple(155, 154, 148), "concrete"),
        Block("gray_concrete_powder", Triple(78, 80, 81), "concrete"),
        Block("black_concrete_powder", Triple(26, 27, 32), "concrete"),
        Block("brown_concrete_powder", Triple(128, 87, 56), "concrete"),
        Block("red_concrete_powder", Triple(170, 53, 48), "concrete"),
        Block("orange_concrete_powder", Triple(235, 135, 32), "concrete"),
        Block("yellow_concrete_powder", Triple(235, 204, 72), "concrete"),
        Block("lime_concrete_powder", Triple(127, 188, 64), "concrete"),
        Block("green_concrete_powder", Triple(93, 118, 48), "concrete"),
        Block("cyan_concrete_powder", Triple(49, 139, 158), "concrete"),
        Block("light_blue_concrete_powder", Triple(108, 163, 219), "concrete"),
        Block("blue_concrete_powder", Triple(68, 72, 163), "concrete"),
        Block("purple_concrete_powder", Triple(142, 62, 166), "concrete"),
        Block("magenta_concrete_powder", Triple(202, 99, 188), "concrete"),
        Block("pink_concrete_powder", Triple(230, 155, 165), "concrete"),

        // Stone variants
        Block("stone", Triple(125, 125, 125), "stone"),
        Block("cobblestone", Triple(124, 123, 123), "stone"),
        Block("andesite", Triple(136, 136, 134), "stone"),
        Block("polished_andesite", Triple(132, 134, 133), "stone"),
        Block("diorite", Triple(181, 180, 178), "stone"),
        Block("polished_diorite", Triple(194, 193, 189), "stone"),
        Block("granite", Triple(153, 115, 95), "stone"),
        Block("polished_granite", Triple(156, 115, 93), "stone"),
        Block("stone_bricks", Triple(123, 121, 120), "stone"),
        Block("gravel", Triple(129, 126, 124), "stone"),
        Block("sandstone", Triple(221, 208, 165), "stone"),
        Block("smooth_sandstone", Triple(224, 211, 168), "stone"),
        Block("red_sandstone", Triple(186, 99, 29), "stone"),
        Block("bedrock", Triple(85, 85, 85), "stone"),

        // Wood / Organic
        Block("oak_planks", Triple(164, 133, 82), "wood"),
        Block("spruce_planks", Triple(110, 82, 49), "wood"),
        Block("birch_planks", Triple(198, 186, 120), "wood"),
        Block("jungle_planks", Triple(162, 130, 98), "wood"),
        Block("acacia_planks", Triple(171, 96, 54), "wood"),
        Block("dark_oak_planks", Triple(67, 43, 21), "wood"),
        Block("crimson_planks", Triple(103, 54, 84), "wood"),
        Block("warped_planks", Triple(52, 98, 107), "wood"),
        Block("mangrove_planks", Triple(117, 53, 49), "wood"),
        Block("cherry_planks", Triple(201, 158, 143), "wood"),
        Block("bamboo_planks", Triple(213, 202, 116), "wood"),
        Block("oak_log", Triple(113, 90, 57), "wood"),
        Block("spruce_log", Triple(52, 35, 29), "wood"),
        Block("birch_log", Triple(220, 224, 218), "wood"),
        Block("jungle_log", Triple(97, 73, 40), "wood"),
        Block("acacia_log", Triple(108, 108, 103), "wood"),
        Block("dark_oak_log", Triple(55, 39, 18), "wood"),
        Block("crimson_stem", Triple(69, 30, 68), "wood"),
        Block("warped_stem", Triple(44, 89, 80), "wood"),
        Block("stripped_oak_log", Triple(180, 148, 77), "wood"),
        Block("stripped_spruce_log", Triple(114, 87, 51), "wood"),
        Block("stripped_birch_log", Triple(199, 175, 89), "wood"),
        Block("stripped_jungle_log", Triple(174, 140, 92), "wood"),
        Block("stripped_acacia_log", Triple(178, 103, 61), "wood"),
        Block("stripped_dark_oak_log", Triple(73, 51, 29), "wood"),
        Block("stripped_crimson_stem", Triple(141, 72, 116), "wood"),
        Block("stripped_warped_stem", Triple(59, 126, 119), "wood"),

        // Extra colors
        Block("snow_block", Triple(245, 250, 251), "wool"),
        Block("ice", Triple(147, 186, 249), "wool"),
        Block("packed_ice", Triple(159, 187, 249), "wool"),
        Block("blue_ice", Triple(120, 158, 253), "wool"),
        Block("clay", Triple(164, 172, 186), "terracotta"),
        Block("dirt", Triple(138, 106, 68), "soil"),
        Block("coarse_dirt", Triple(112, 83, 51), "soil"),
        Block("podzol", Triple(106, 72, 23), "soil"),
        Block("rooted_dirt", Triple(134, 98, 58), "soil"),
        Block("mud", Triple(56, 52, 47), "soil"),
        Block("packed_mud", Triple(140, 131, 112), "soil"),
        Block("mud_bricks", Triple(143, 125, 88), "soil"),
        Block("soul_sand", Triple(80, 60, 43), "soil"),
        Block("soul_soil", Triple(69, 52, 37), "soil"),
        Block("netherrack", Triple(110, 39, 39), "soil"),
        Block("crimson_nylium", Triple(126, 49, 44), "soil"),
        Block("warped_nylium", Triple(49, 81, 65), "soil"),
        Block("basalt", Triple(74, 75, 80), "stone"),
        Block("polished_basalt", Triple(74, 76, 79), "stone"),
        Block("smooth_basalt", Triple(73, 75, 78), "stone"),
        Block("tuff", Triple(107, 107, 99), "stone"),
        Block("calcite", Triple(224, 227, 223), "stone"),
        Block("dripstone_block", Triple(145, 107, 79), "stone"),
        Block("deepslate", Triple(79, 79, 84), "stone"),
        Block("cobbled_deepslate", Triple(78, 79, 84), "stone"),
        Block("polished_deepslate", Triple(77, 77, 82), "stone"),
        Block("deepslate_bricks", Triple(74, 74, 78), "stone"),
        Block("blackstone", Triple(44, 38, 41), "stone"),
        Block("polished_blackstone", Triple(47, 44, 49), "stone"),
        Block("gilded_blackstone", Triple(60, 49, 37), "stone"),
        Block("end_stone", Triple(221, 224, 166), "stone"),
        Block("end_stone_bricks", Triple(225, 227, 170), "stone"),
        Block("purpur_block", Triple(171, 127, 172), "stone"),
        Block("purpur_pillar", Triple(172, 126, 171), "stone"),
        Block("prismarine", Triple(98, 148, 117), "stone"),
        Block("prismarine_bricks", Triple(97, 175, 159), "stone"),
        Block("dark_prismarine", Triple(61, 98, 69), "stone"),
        Block("obsidian", Triple(21, 20, 31), "stone"),
        Block("crying_obsidian", Triple(35, 24, 54), "stone"),
        Block("nether_bricks", Triple(47, 23, 27), "stone"),
        Block("red_nether_bricks", Triple(72, 17, 12), "stone"),
        Block("quartz_block", Triple(237, 233, 229), "stone"),
        Block("smooth_quartz", Triple(237, 233, 229), "stone"),
        Block("quartz_bricks", Triple(237, 234, 229), "stone"),
        Block("bricks", Triple(159, 83, 54), "stone"),
        Block("coal_block", Triple(21, 21, 25), "jewel"),
        Block("iron_block", Triple(218, 220, 214), "jewel"),
        Block("gold_block", Triple(250, 239, 78), "jewel"),
        Block("diamond_block", Triple(100, 216, 207), "jewel"),
        Block("emerald_block", Triple(85, 215, 55), "jewel"),
        Block("redstone_block", Triple(184, 26, 11), "jewel"),
        Block("lapis_block", Triple(28, 66, 162), "jewel"),
        Block("copper_block", Triple(192, 112, 73), "jewel"),
        Block("exposed_copper", Triple(149, 123, 104), "jewel"),
        Block("weathered_copper", Triple(107, 142, 112), "jewel"),
        Block("oxidized_copper", Triple(96, 164, 136), "jewel"),
        Block("amethyst_block", Triple(152, 103, 193), "jewel"),
        Block("raw_iron_block", Triple(176, 167, 151), "stone"),
        Block("raw_copper_block", Triple(186, 116, 60), "stone"),
        Block("raw_gold_block", Triple(228, 197, 65), "stone"),
        Block("ancient_debris", Triple(111, 80, 72), "stone"),
        Block("netherite_block", Triple(67, 62, 64), "jewel"),

        // Glass
        Block("glass", Triple(207, 213, 226), "glass"),
        Block("tinted_glass", Triple(65, 65, 76), "glass"),
        Block("white_stained_glass", Triple(240, 240, 240), "glass"),
        Block("red_stained_glass", Triple(160, 40, 40), "glass"),
        Block("orange_stained_glass", Triple(220, 120, 20), "glass"),
        Block("yellow_stained_glass", Triple(230, 220, 30), "glass"),
        Block("lime_stained_glass", Triple(120, 200, 40), "glass"),
        Block("green_stained_glass", Triple(80, 110, 30), "glass"),
        Block("cyan_stained_glass", Triple(30, 140, 170), "glass"),
        Block("light_blue_stained_glass", Triple(100, 150, 220), "glass"),
        Block("blue_stained_glass", Triple(50, 55, 160), "glass"),
        Block("purple_stained_glass", Triple(125, 50, 150), "glass"),
        Block("magenta_stained_glass", Triple(195, 80, 180), "glass"),
        Block("pink_stained_glass", Triple(230, 140, 160), "glass"),
        Block("gray_stained_glass", Triple(75, 75, 75), "glass"),
        Block("light_gray_stained_glass", Triple(150, 150, 150), "glass"),
        Block("black_stained_glass", Triple(25, 25, 30), "glass"),
        Block("brown_stained_glass", Triple(100, 60, 40), "glass"),

        // Light
        Block("glowstone", Triple(158, 131, 97), "light"),
        Block("sea_lantern", Triple(179, 199, 202), "light"),
        Block("shroomlight", Triple(249, 154, 55), "light"),
        Block("redstone_lamp", Triple(108, 68, 53), "light"),

        // Ore
        Block("coal_ore", Triple(48, 48, 48), "ore"),
        Block("iron_ore", Triple(148, 125, 110), "ore"),
        Block("gold_ore", Triple(166, 140, 57), "ore"),
        Block("diamond_ore", Triple(119, 176, 192), "ore"),
        Block("emerald_ore", Triple(58, 152, 92), "ore"),
        Block("redstone_ore", Triple(142, 37, 41), "ore"),
        Block("lapis_ore", Triple(39, 66, 117), "ore"),
        Block("copper_ore", Triple(130, 106, 88), "ore"),
        Block("nether_quartz_ore", Triple(133, 71, 81), "ore"),
        Block("nether_gold_ore", Triple(133, 68, 38), "ore"),
        Block("deepslate_coal_ore", Triple(42, 42, 43), "ore"),
        Block("deepslate_iron_ore", Triple(122, 107, 97), "ore"),
        Block("deepslate_gold_ore", Triple(138, 118, 56), "ore"),
        Block("deepslate_diamond_ore", Triple(103, 148, 160), "ore"),
        Block("deepslate_emerald_ore", Triple(53, 131, 83), "ore"),
        Block("deepslate_redstone_ore", Triple(120, 36, 40), "ore"),
        Block("deepslate_lapis_ore", Triple(37, 60, 100), "ore"),
        Block("deepslate_copper_ore", Triple(110, 92, 78), "ore"),

        // Glazed Terracotta
        Block("white_glazed_terracotta", Triple(198, 231, 239), "terracotta"),
        Block("red_glazed_terracotta", Triple(208, 57, 40), "terracotta"),
        Block("orange_glazed_terracotta", Triple(79, 153, 217), "terracotta"),
        Block("yellow_glazed_terracotta", Triple(217, 195, 61), "terracotta"),
        Block("lime_glazed_terracotta", Triple(155, 209, 64), "terracotta"),
        Block("green_glazed_terracotta", Triple(116, 183, 65), "terracotta"),
        Block("cyan_glazed_terracotta", Triple(52, 140, 151), "terracotta"),
        Block("blue_glazed_terracotta", Triple(64, 89, 178), "terracotta"),
        Block("purple_glazed_terracotta", Triple(135, 64, 184), "terracotta"),
        Block("magenta_glazed_terracotta", Triple(208, 105, 186), "terracotta"),
        Block("pink_glazed_terracotta", Triple(223, 137, 150), "terracotta"),
        Block("gray_glazed_terracotta", Triple(90, 98, 103), "terracotta"),
        Block("light_gray_glazed_terracotta", Triple(131, 149, 153), "terracotta"),
        Block("black_glazed_terracotta", Triple(51, 43, 41), "terracotta"),
        Block("brown_glazed_terracotta", Triple(130, 87, 67), "terracotta"),
        Block("light_blue_glazed_terracotta", Triple(113, 157, 206), "terracotta"),
    )

    fun filterByGroups(groups: Set<String>): List<Block> =
        blocks.filter { it.group in groups }

    fun getBlock(name: String): Block? =
        blocks.find { it.name == name }

    val blocks: List<Block> by lazy {
        rawBlocks.map { block ->
            block.copy(
                transparent = block.group == "glass" ||
                    block.name.contains("ice") || block.name.contains("glass"),
                falling = block.name.contains("sand") ||
                    block.name.contains("gravel") ||
                    block.name.contains("concrete_powder") ||
                    block.name.contains("anvil"),
                survivalObtainable = block.group !in setOf("jewel") &&
                    !block.name.contains("ancient_debris") &&
                    !block.name.contains("netherite"),
                luminance = block.name.contains("glow") ||
                    block.name.contains("lamp") ||
                    block.name in setOf("sea_lantern", "shroomlight", "jack_o_lantern"),
                redstone = block.name.contains("redstone") ||
                    block.name.contains("piston") ||
                    block.name.contains("dispenser") ||
                    block.name.contains("observer") ||
                    block.name.contains("comparator") ||
                    block.name.contains("repeater"),
                version = block.version
            )
        }
    }
}
