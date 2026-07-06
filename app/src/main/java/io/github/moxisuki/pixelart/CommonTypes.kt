package io.github.moxisuki.pixelart.api

/**
 * A single entry in a material list with its usage count, color, and group.
 *
 * @property name The block name.
 * @property count The number of times this block appears.
 * @property color The hex color string of the block (e.g. "#ff0000").
 * @property group The material group the block belongs to.
 */
data class BlockEntry(
    val name: String,
    val count: Int,
    val color: String,
    val group: String
)

/**
 * A collection of generated Minecraft commands grouped by type.
 *
 * @property setblockCommands Individual setblock commands.
 * @property fillCommands Optimized fill commands for rectangular regions.
 * @property allCommands Fill commands followed by setblock commands, in execution order.
 */
data class CommandSet(
    val setblockCommands: List<String>,
    val fillCommands: List<String>,
    val allCommands: List<String>
)

/**
 * Data required to serialize a pixel art layout into a Minecraft .schematic file.
 *
 * @property width The X dimension in blocks.
 * @property height The Y dimension in blocks.
 * @property length The Z dimension in blocks.
 * @property blockIds 2D array of palette indices for each (y, x) position.
 * @property blockData 2D array of block data values for each (y, x) position.
 * @property palette Mapping from block identifier to its palette index.
 * @property version The Minecraft version string.
 */
data class SchematicData(
    val width: Int,
    val height: Int,
    val length: Int,
    val blockIds: Array<ShortArray>,
    val blockData: Array<ByteArray>,
    val palette: Map<String, Int>,
    val version: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SchematicData) return false
        return width == other.width && height == other.height && length == other.length &&
                blockIds.contentDeepEquals(other.blockIds) &&
                blockData.contentDeepEquals(other.blockData) &&
                palette == other.palette && version == other.version
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + length
        result = 31 * result + blockIds.contentDeepHashCode()
        result = 31 * result + blockData.contentDeepHashCode()
        result = 31 * result + palette.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}
