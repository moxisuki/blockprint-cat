package io.github.moxisuki.pixelart.api

/**
 * Represents a Minecraft block state with optional properties.
 *
 * @property id The block identifier (e.g. "minecraft:stone").
 * @property properties Optional key-value properties describing the block state.
 */
data class BlockState(
    val id: String,
    val properties: Map<String, String>? = null
) {
    /**
     * Converts this block state to its NBT string representation.
     *
     * @return The block ID with properties in bracket notation, or just the ID if no properties.
     */
    fun toNbtString(): String {
        if (properties.isNullOrEmpty()) return id
        val props = properties.entries.joinToString(",") { "${it.key}=${it.value}" }
        return "$id[$props]"
    }

    /**
     * Checks whether this block state represents air.
     *
     * @return `true` if the block is air, `false` otherwise.
     */
    fun isAir(): Boolean = id == "minecraft:air" || id == "air"

    companion object {
        /** A constant [BlockState] representing minecraft air. */
        val AIR = BlockState("minecraft:air")
    }
}

/**
 * Represents the placement of a single block at a specific world coordinate.
 *
 * @property x The X coordinate.
 * @property y The Y coordinate.
 * @property z The Z coordinate.
 * @property state The [BlockState] to place.
 */
data class BlockPlacement(
    val x: Int,
    val y: Int,
    val z: Int,
    val state: BlockState
)

/**
 * Represents a rectangular region to be filled with a single block state.
 *
 * @property x1 The starting X coordinate.
 * @property y1 The starting Y coordinate.
 * @property z1 The starting Z coordinate.
 * @property x2 The ending X coordinate.
 * @property y2 The ending Y coordinate.
 * @property z2 The ending Z coordinate.
 * @property state The [BlockState] to fill the region with.
 */
data class FillRegion(
    val x1: Int, val y1: Int, val z1: Int,
    val x2: Int, val y2: Int, val z2: Int,
    val state: BlockState
)
