package io.github.moxisuki.pixelart.api

import io.github.moxisuki.pixelart.Block
import io.github.moxisuki.pixelart.BlockPalette
import io.github.moxisuki.pixelart.ColorUtils

/**
 * Provides access to the block palette used for pixel art color matching.
 */
object PaletteApi {

    /**
     * Returns all available blocks in the palette.
     *
     * @return A list of all [Block] entries.
     */
    fun allBlocks(): List<Block> = BlockPalette.blocks

    /**
     * Returns blocks belonging to the specified group.
     *
     * @param group The material group name (e.g. "wool", "concrete").
     * @return A list of [Block] entries in that group.
     */
    fun blocksByGroup(group: String): List<Block> =
        BlockPalette.blocks.filter { it.group == group }

    /**
     * Returns blocks belonging to any of the specified groups.
     *
     * @param groups A set of material group names.
     * @return A list of [Block] entries matching any of the groups.
     */
    fun blocksByGroups(groups: Set<String>): List<Block> =
        BlockPalette.filterByGroups(groups)

    /**
     * Returns the set of all distinct material group names in the palette.
     *
     * @return A set of group name strings.
     */
    fun availableGroups(): Set<String> =
        BlockPalette.blocks.map { it.group }.toSet()

    /**
     * Looks up a block by its exact name.
     *
     * @param name The block name.
     * @return The matching [Block], or `null` if not found.
     */
    fun getBlock(name: String): Block? =
        BlockPalette.getBlock(name)

    /**
     * Searches for blocks whose name contains the given query (case-insensitive).
     *
     * @param query The search string.
     * @return A list of matching [Block] entries.
     */
    fun searchByName(query: String): List<Block> =
        BlockPalette.blocks.filter { it.name.contains(query, ignoreCase = true) }

    /**
     * Creates a list of custom [Block] entries from user-defined specifications.
     *
     * @param entries A list of [CustomBlockEntry] defining name, RGB, and group.
     * @return A list of constructed [Block] objects.
     */
    fun createCustomPalette(entries: List<CustomBlockEntry>): List<Block> =
        entries.map { Block(it.name, Triple(it.r, it.g, it.b), it.group) }

    /**
     * Merges the palette blocks for the request's groups with any custom blocks.
     *
     * @param request The conversion request containing group and custom palette settings.
     * @return The combined list of [Block] entries.
     */
    fun mergeWithCustom(request: PixelArtRequest): List<Block> {
        val base = BlockPalette.filterByGroups(request.blockGroups)
        if (request.customPalette.isEmpty()) return base
        val custom = createCustomPalette(request.customPalette)
        return base + custom
    }

    /**
     * Finds blocks in the palette most visually similar to the given RGB color.
     *
     * @param r The red component (0-255).
     * @param g The green component (0-255).
     * @param b The blue component (0-255).
     * @param limit The maximum number of results to return.
     * @return A list of pairs of [Block] to their weighted distance score, sorted by similarity.
     */
    fun findSimilarBlocks(r: Int, g: Int, b: Int, limit: Int = 5): List<Pair<Block, Double>> =
        BlockPalette.blocks
            .map { it to ColorUtils.weightedRgbDistance(Triple(r, g, b), it.rgb) }
            .sortedBy { it.second }
            .take(limit)

    /**
     * Finds the single nearest block to the given RGB color among the provided candidates.
     *
     * @param r The red component (0-255).
     * @param g The green component (0-255).
     * @param b The blue component (0-255).
     * @param candidates The list of [Block] candidates to search.
     * @return The closest matching [Block].
     */
    fun nearestBlock(r: Int, g: Int, b: Int, candidates: List<Block>): Block =
        candidates.minByOrNull {
            ColorUtils.weightedRgbDistance(Triple(r, g, b), it.rgb)
        } ?: candidates.first()

    /**
     * Returns a count of blocks per material group in the palette.
     *
     * @return A map of group name to block count.
     */
    fun paletteStats(): Map<String, Int> =
        BlockPalette.blocks.groupingBy { it.group }.eachCount()

    /**
     * Exports the entire palette as a JSON string grouped by material group.
     *
     * @return A formatted JSON string.
     */
    fun exportPaletteJson(): String = buildString {
        appendLine("{")
        val groups = BlockPalette.blocks.groupBy { it.group }
        groups.entries.forEachIndexed { gi, (group, blocks) ->
            appendLine("  \"$group\": [")
            blocks.forEachIndexed { bi, block ->
                val comma = if (bi < blocks.size - 1) "," else ""
                appendLine("    {\"name\": \"${block.name}\", \"r\": ${block.rgb.first}, \"g\": ${block.rgb.second}, \"b\": ${block.rgb.third}}$comma")
            }
            val gcomma = if (gi < groups.size - 1) "," else ""
            appendLine("  ]$gcomma")
        }
        appendLine("}")
    }
}
