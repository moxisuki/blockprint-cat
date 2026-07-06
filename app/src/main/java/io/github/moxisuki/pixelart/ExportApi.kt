package io.github.moxisuki.pixelart.api

import io.github.moxisuki.pixelart.Block

/**
 * Generates Minecraft command strings, CSV material lists, and schematic data from pixel art results.
 */
object ExportApi {

    /**
     * 6 placement modes matching the web tool's command orientation options.
     * ES/WS/EN/WN = horizontal floor placement; EU/NU = vertical wall placement.
     */
    enum class CommandDirection(val id: String, val dxX: Int, val dzX: Int, val dyY: Int, val dzY: Int, val horizontal: Boolean) {
        ES("ES", 1, 0, 0, 1, true),
        WS("WS", -1, 0, 0, 1, true),
        EN("EN", 1, 0, 0, -1, true),
        WN("WN", -1, 0, 0, -1, true),
        EU("EU", 1, 0, 1, 0, false),
        NU("NU", 0, -1, 1, 0, false)
    }

    private val directionOffsets = mapOf(
        "north" to Pair(1, 0),
        "south" to Pair(-1, 0),
        "east" to Pair(0, 1),
        "west" to Pair(0, -1)
    )

    /**
     * Shortcut: generates commands using the 6 placement mode system.
     *
     * @param mode The placement direction mode (ES/WS/EN/WN/EU/NU).
     * @param baseY The Y coordinate for horizontal modes or the base column for vertical modes.
     */
    fun generateCommandsWithDirection(
        blocks: Array<Array<Block?>>,
        width: Int,
        height: Int,
        mode: CommandDirection = CommandDirection.ES,
        baseX: Int = 0,
        baseY: Int = 64,
        baseZ: Int = 0,
        useFill: Boolean = true
    ): CommandSet {
        val setblockCommands = mutableListOf<String>()
        val fillCommands = mutableListOf<String>()
        val processed = Array(height) { BooleanArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (processed[y][x]) continue
                val block = blocks[y][x]
                if (block == null) { processed[y][x] = true; continue }

                if (useFill) {
                    val rect = findRectangle(blocks, processed, x, y, width, height, block)
                    val worldX1 = baseX + x * mode.dxX + (height - 1 - y) * mode.dzX
                    val worldZ1 = baseZ + x * mode.dzY + (height - 1 - y) * mode.dyY
                    val worldY1 = if (mode.horizontal) baseY else baseY + (height - 1 - y) * mode.dyY
                    val worldX2 = baseX + rect.x2 * mode.dxX + (height - 1 - rect.y2) * mode.dzX
                    val worldZ2 = baseZ + rect.x2 * mode.dzY + (height - 1 - rect.y2) * mode.dyY
                    val worldY2 = if (mode.horizontal) baseY else baseY + (height - 1 - rect.y2) * mode.dyY

                    if (rect.x1 == rect.x2 && rect.y1 == rect.y2) {
                        setblockCommands.add("/setblock $worldX1 $worldY1 $worldZ1 ${block.name}")
                    } else {
                        fillCommands.add("/fill $worldX1 $worldY1 $worldZ1 $worldX2 $worldY2 $worldZ2 ${block.name}")
                    }
                } else {
                    val worldX = baseX + x * mode.dxX + (height - 1 - y) * mode.dzX
                    val worldZ = baseZ + x * mode.dzY + (height - 1 - y) * mode.dyY
                    val worldY = if (mode.horizontal) baseY else baseY + (height - 1 - y) * mode.dyY
                    setblockCommands.add("/setblock $worldX $worldY $worldZ ${block.name}")
                    processed[y][x] = true
                }
            }
        }

        return CommandSet(setblockCommands, fillCommands, fillCommands + setblockCommands)
    }

    /**
     * Legacy API: generates commands with simple cardinal direction.
     */
    fun generateCommands(
        blocks: Array<Array<Block?>>,
        width: Int,
        height: Int,
        direction: String = "north",
        baseX: Int = 0,
        baseY: Int = 64,
        baseZ: Int = 0,
        useFill: Boolean = true
    ): CommandSet {
        val setblockCommands = mutableListOf<String>()
        val fillCommands = mutableListOf<String>()
        val (dx, dz) = directionOffsets[direction] ?: Pair(1, 0)

        // Group adjacent same blocks for fill optimization
        val processed = Array(height) { BooleanArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (processed[y][x]) continue
                val block = blocks[y][x]
                if (block == null) {
                    processed[y][x] = true
                    continue
                }

                if (useFill) {
                    val rect = findRectangle(blocks, processed, x, y, width, height, block)
                    val worldX1 = baseX + x * dx
                    val worldZ1 = baseZ + x * dz
                    val worldY = baseY + (height - 1 - y)
                    val worldX2 = baseX + rect.x2 * dx
                    val worldZ2 = baseZ + rect.x2 * dz

                    if (rect.x1 == rect.x2 && rect.y1 == rect.y2) {
                        setblockCommands.add(
                            "/setblock $worldX1 $worldY $worldZ1 ${block.name}"
                        )
                    } else {
                        fillCommands.add(
                            "/fill $worldX1 $worldY $worldZ1 $worldX2 $worldY $worldZ2 ${block.name}"
                        )
                    }
                } else {
                    val worldX = baseX + x * dx
                    val worldZ = baseZ + x * dz
                    val worldY = baseY + (height - 1 - y)
                    setblockCommands.add(
                        "/setblock $worldX $worldY $worldZ ${block.name}"
                    )
                    processed[y][x] = true
                }
            }
        }

        return CommandSet(
            setblockCommands = setblockCommands,
            fillCommands = fillCommands,
            allCommands = fillCommands + setblockCommands
        )
    }

    private data class Rect(val x1: Int, val y1: Int, val x2: Int, val y2: Int)

    private fun findRectangle(
        blocks: Array<Array<Block?>>,
        processed: Array<BooleanArray>,
        startX: Int,
        startY: Int,
        maxWidth: Int,
        maxHeight: Int,
        target: Block
    ): Rect {
        var endX = startX
        var endY = startY

        // Expand right
        while (endX + 1 < maxWidth && blocks[startY][endX + 1] == target && !processed[startY][endX + 1]) {
            endX++
        }

        // Expand down
        var canExpand = true
        while (endY + 1 < maxHeight && canExpand) {
            for (x in startX..endX) {
                if (blocks[endY + 1][x] != target || processed[endY + 1][x]) {
                    canExpand = false
                    break
                }
            }
            if (canExpand) endY++
        }

        // Mark as processed
        for (y in startY..endY) {
            for (x in startX..endX) {
                processed[y][x] = true
            }
        }

        return Rect(startX, startY, endX, endY)
    }

    /**
     * Generates a CSV string from a material list.
     *
     * @param materialList The list of [BlockEntry] to export.
     * @return A CSV-formatted string with header.
     */
    fun generateCsv(materialList: List<BlockEntry>): String = buildString {
        appendLine("block_name,count,group,color")
        materialList.sortedByDescending { it.count }.forEach { entry ->
            appendLine("${entry.name},${entry.count},${entry.group},${entry.color}")
        }
    }

    /**
     * Builds [SchematicData] from a pixel art block grid for .schematic serialization.
     *
     * @param blocks 2D array of blocks indexed by [y][x].
     * @param width The width of the art in blocks.
     * @param height The height of the art in blocks.
     * @param version The Minecraft version string.
     * @return A [SchematicData] instance ready for serialization.
     */
    fun buildSchematicData(
        blocks: Array<Array<Block?>>,
        width: Int,
        height: Int,
        version: String = "1.21"
    ): SchematicData {
        val paletteMap = mutableMapOf<String, Int>()
        val blockIds = Array(height) { ShortArray(width) }
        val blockData = Array(height) { ByteArray(width) }

        var nextId = 1
        paletteMap["minecraft:air"] = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val block = blocks[y][x]
                if (block != null) {
                    val fullId = "minecraft:${block.name}"
                    val id = paletteMap.getOrPut(fullId) { nextId++ }
                    blockIds[y][x] = id.toShort()
                    blockData[y][x] = 0
                } else {
                    blockIds[y][x] = 0
                    blockData[y][x] = 0
                }
            }
        }

        return SchematicData(
            width = width,
            height = 1,
            length = height,
            blockIds = blockIds,
            blockData = blockData,
            palette = paletteMap,
            version = version
        )
    }

    /**
     * Formats a [CommandSet] into a human-readable string with a summary header.
     *
     * @param commands The command set to format.
     * @return A formatted multi-line string.
     */
    fun commandsToString(commands: CommandSet): String = buildString {
        val total = commands.setblockCommands.size + commands.fillCommands.size
        appendLine("# Total commands: $total (${commands.fillCommands.size} fill, ${commands.setblockCommands.size} setblock)")
        appendLine("# Fill commands (optimized):")
        commands.fillCommands.forEach { appendLine(it) }
        appendLine("# Setblock commands:")
        commands.setblockCommands.forEach { appendLine(it) }
    }

    /**
     * Formats a material list into a human-readable table string.
     *
     * @param materialList The list of [BlockEntry] to display.
     * @param totalBlocks The total number of blocks for ratio calculation.
     * @return A formatted multi-line string with columns.
     */
    fun materialListToFormattedString(
        materialList: List<BlockEntry>,
        totalBlocks: Int
    ): String = buildString {
        appendLine("Material List (${materialList.size} types, $totalBlocks total blocks)")
        appendLine("=" .repeat(50))
        appendLine(String.format("%-30s %8s %8s %12s", "Block", "Count", "Ratio", "Group"))
        appendLine("-" .repeat(50))
        materialList.sortedByDescending { it.count }.forEach { entry ->
            val ratio = String.format("%.1f%%", entry.count * 100.0 / totalBlocks)
            appendLine(String.format("%-30s %8d %8s %12s", entry.name, entry.count, ratio, entry.group))
        }
    }
}
