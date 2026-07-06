package io.github.moxisuki.pixelart.api

/**
 * Constructs a [BlueprintDoc] by defining regions, block placements, and fill operations
 * for a pixel art layout.
 */
class BlueprintBuilder {
    private var _name: String = ""
    private var _author: String = ""
    private var _description: String = ""
    private var _dataVersion: Int = 3953
    private var _version: Int = 6
    private val _regions = mutableListOf<RegionSpec>()
    private val _globals = mutableMapOf<String, String>()

    /**
     * Specification for a single region within the blueprint.
     *
     * @property name The region name.
     * @property width The X dimension of the region.
     * @property height The Y dimension of the region.
     * @property length The Z dimension of the region.
     * @property originX The world X origin of the region.
     * @property originY The world Y origin of the region.
     * @property originZ The world Z origin of the region.
     * @property placements Individual block placements in this region.
     * @property fills Fill regions in this region.
     */
    data class RegionSpec(
        val name: String,
        val width: Int,
        val height: Int,
        val length: Int,
        val originX: Int = 0,
        val originY: Int = 0,
        val originZ: Int = 0,
        val placements: MutableList<BlockPlacement> = mutableListOf(),
        val fills: MutableList<FillRegion> = mutableListOf()
    )

    /** Sets the blueprint name. */
    fun name(name: String) = apply { _name = name }
    /** Sets the blueprint author. */
    fun author(author: String) = apply { _author = author }
    /** Sets the blueprint description. */
    fun description(desc: String) = apply { _description = desc }
    /** Sets the data version. */
    fun dataVersion(v: Int) = apply { _dataVersion = v }
    /** Sets the version number. */
    fun version(v: Int) = apply { _version = v }
    /** Sets a global key-value pair. */
    fun global(key: String, value: String) = apply { _globals[key] = value }

    /**
     * Adds a new region to the blueprint, built via the [RegionScope] DSL.
     *
     * @param name The region name.
     * @param width The X size of the region.
     * @param height The Y size of the region.
     * @param length The Z size of the region.
     * @param block Builder lambda scoped to [RegionScope].
     * @return This builder for chaining.
     */
    fun region(name: String, width: Int, height: Int, length: Int, block: RegionScope.() -> Unit): BlueprintBuilder {
        val scope = RegionScope(name, width, height, length)
        scope.block()
        _regions.add(scope.toSpec())
        return this
    }

    /**
     * Consumes a [PixelArtResponse] and adds its fill regions to the blueprint.
     *
     * @param response The conversion result to consume.
     * @param regionName The name for the created region.
     * @param originX The world X origin of the region.
     * @param originY The world Y origin of the region.
     * @param originZ The world Z origin of the region.
     * @param direction The facing direction.
     * @return This builder for chaining.
     */
    fun consume(response: PixelArtResponse, regionName: String = "pixel_art",
                originX: Int = 0, originY: Int = 64, originZ: Int = 0,
                direction: String = "north"): BlueprintBuilder {
        val w = response.width
        val h = response.height
        val spec = RegionSpec(regionName, w, 1, h, originX, originY, originZ)
        // Use optimized fills, fall back to individual placements
        response.fillRegions(originX, originY, originZ, direction).forEach { spec.fills.add(it) }
        _regions.add(spec)
        return this
    }

    /**
     * Consumes a [PixelArtResponse] and adds individual block placements (no fill optimization).
     *
     * @param response The conversion result to consume.
     * @param regionName The name for the created region.
     * @param originX The world X origin of the region.
     * @param originY The world Y origin of the region.
     * @param originZ The world Z origin of the region.
     * @param direction The facing direction.
     * @return This builder for chaining.
     */
    fun consumeSingle(response: PixelArtResponse, regionName: String = "pixel_art",
                      originX: Int = 0, originY: Int = 64, originZ: Int = 0,
                      direction: String = "north"): BlueprintBuilder {
        val w = response.width
        val h = response.height
        val spec = RegionSpec(regionName, w, 1, h, originX, originY, originZ)
        response.blockPlacements(originX, originY, originZ, direction).forEach { spec.placements.add(it) }
        _regions.add(spec)
        return this
    }

    /**
     * Builds the final [BlueprintDoc] from the configured builder state.
     *
     * @return A new [BlueprintDoc] instance.
     */
    fun build(): BlueprintDoc = BlueprintDoc(
        name = _name,
        author = _author,
        description = _description,
        dataVersion = _dataVersion,
        version = _version,
        regions = _regions.toList(),
        globals = _globals.toMap()
    )

    /**
     * DSL scope for defining block placements and fills within a single region.
     */
    class RegionScope internal constructor(
        private val name: String,
        private val width: Int,
        private val height: Int,
        private val length: Int
    ) {
        private var _ox: Int = 0; private var _oy: Int = 0; private var _oz: Int = 0
        private val _placements = mutableListOf<BlockPlacement>()
        private val _fills = mutableListOf<FillRegion>()

        /** Sets the origin position for this region. */
        fun position(x: Int, y: Int, z: Int) { _ox = x; _oy = y; _oz = z }

        /**
         * Places a single block at a relative position within the region.
         *
         * @param x Relative X coordinate.
         * @param y Relative Y coordinate.
         * @param z Relative Z coordinate.
         * @param block The block identifier string.
         */
        fun set(x: Int, y: Int, z: Int, block: String) {
            val bs = parseBlockState(block)
            _placements.add(BlockPlacement(_ox + x, _oy + y, _oz + z, bs))
        }

        /**
         * Places a single block at a relative position within the region.
         *
         * @param x Relative X coordinate.
         * @param y Relative Y coordinate.
         * @param z Relative Z coordinate.
         * @param state The [BlockState] to place.
         */
        fun set(x: Int, y: Int, z: Int, state: BlockState) {
            _placements.add(BlockPlacement(_ox + x, _oy + y, _oz + z, state))
        }

        /**
         * Fills a rectangular volume with a single block type.
         *
         * @param x1 Starting X (relative).
         * @param y1 Starting Y (relative).
         * @param z1 Starting Z (relative).
         * @param x2 Ending X (relative).
         * @param y2 Ending Y (relative).
         * @param z2 Ending Z (relative).
         * @param block The block identifier string.
         */
        fun fill(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int, block: String) {
            val bs = parseBlockState(block)
            _fills.add(FillRegion(_ox + x1, _oy + y1, _oz + z1, _ox + x2, _oy + y2, _oz + z2, bs))
        }

        /** Places an air block at a relative position. */
        fun air(x: Int, y: Int, z: Int) {
            _placements.add(BlockPlacement(_ox + x, _oy + y, _oz + z, BlockState.AIR))
        }

        /**
         * Consumes a [PixelArtResponse] and adds its fill regions to this region scope.
         *
         * @param response The conversion result to consume.
         * @param resizeH Reserved for 3D staircase height; ignored.
         */
        fun consume(response: PixelArtResponse, resizeH: Int = 1) {
            // For 3D staircase, use height; for 2D flat, just 1 layer
            response.fillRegions(_ox, _oy, _oz).forEach { _fills.add(it) }
        }

        internal fun toSpec() = RegionSpec(
            name = name, width = width, height = height, length = length,
            originX = _ox, originY = _oy, originZ = _oz,
            placements = _placements, fills = _fills
        )

        private fun parseBlockState(raw: String): BlockState {
            val bracket = raw.indexOf('[')
            return if (bracket < 0) {
                BlockState(raw)
            } else {
                val id = raw.substring(0, bracket)
                val propsRaw = raw.substring(bracket + 1, raw.length - 1)
                val props = propsRaw.split(",").associate {
                    val (k, v) = it.split("=")
                    k.trim() to v.trim()
                }
                BlockState(id, props)
            }
        }
    }
}

/**
 * The fully assembled blueprint document containing regions, metadata, and global settings.
 *
 * @property name The blueprint name.
 * @property author The blueprint author.
 * @property description A description of the blueprint.
 * @property dataVersion The Minecraft data version.
 * @property version The blueprint format version.
 * @property regions The list of region specifications.
 * @property globals Global key-value settings.
 */
data class BlueprintDoc(
    val name: String,
    val author: String,
    val description: String,
    val dataVersion: Int,
    val version: Int,
    val regions: List<BlueprintBuilder.RegionSpec>,
    val globals: Map<String, String>
) {
    /**
     * Returns a human-readable summary of the blueprint document.
     *
     * @return A formatted multi-line summary string.
     */
    fun summary(): String = buildString {
        appendLine("Blueprint: $name")
        appendLine("  Author: ${author.ifEmpty { "(none)" }}")
        appendLine("  Regions: ${regions.size}")
        regions.forEach { r ->
            val totalPlacements = r.placements.size + r.fills.sumOf { (it.x2 - it.x1 + 1) * (it.y2 - it.y1 + 1) * (it.z2 - it.z1 + 1) }
            appendLine("    ${r.name}: ${r.width}x${r.height}x${r.length} at (${r.originX},${r.originY},${r.originZ})")
            appendLine("      ${r.placements.size} placements, ${r.fills.size} fills (~$totalPlacements blocks)")
        }
    }
}
