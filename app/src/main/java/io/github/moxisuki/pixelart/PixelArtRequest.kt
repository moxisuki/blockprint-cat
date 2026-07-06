package io.github.moxisuki.pixelart.api

import android.graphics.Bitmap
import io.github.moxisuki.pixelart.DitherMethod

data class CustomBlockEntry(
    val name: String,
    val r: Int,
    val g: Int,
    val b: Int,
    val group: String = "custom"
)

data class PixelArtRequest(
    val image: Bitmap,
    val targetWidth: Int = 128,
    val ditherMethod: DitherMethod = DitherMethod.FLOYD_STEINBERG,
    val blockGroups: Set<String> = setOf("wool", "concrete", "terracotta", "stone", "wood"),
    val brightness: Int = 100,
    val contrast: Int = 100,
    val saturation: Int = 100,
    val transparencyEnabled: Boolean = false,
    val transparencyTolerance: Int = 128,
    val staircaseMode: Int = 0,
    val customPalette: List<CustomBlockEntry> = emptyList(),
    val enableDither: Boolean = true,
    val generateCommands: Boolean = false,
    val generateCsv: Boolean = false,
    val generateSchematicData: Boolean = false,
    val minecraftVersion: String = "1.21",
    val weDirection: String = "north",
    val supportBlock: String = "cobblestone",
    val ignoreHeightLimit: Boolean = false
) {
    class Builder {
        private var image: Bitmap? = null
        private var targetWidth: Int = 128
        private var ditherMethod: DitherMethod = DitherMethod.FLOYD_STEINBERG
        private var blockGroups: Set<String> = setOf("wool", "concrete", "terracotta", "stone", "wood")
        private var brightness: Int = 100
        private var contrast: Int = 100
        private var saturation: Int = 100
        private var transparencyEnabled: Boolean = false
        private var transparencyTolerance: Int = 128
        private var staircaseMode: Int = 0
        private var customPalette: MutableList<CustomBlockEntry> = mutableListOf()
        private var enableDither: Boolean = true
        private var generateCommands: Boolean = false
        private var generateCsv: Boolean = false
        private var generateSchematicData: Boolean = false
        private var minecraftVersion: String = "1.21"
        private var weDirection: String = "north"
        private var supportBlock: String = "cobblestone"
        private var ignoreHeightLimit: Boolean = false

        fun image(image: Bitmap) = apply { this.image = image }
        fun width(width: Int) = apply { this.targetWidth = width.coerceIn(16, 2048) }
        fun dither(method: DitherMethod) = apply { this.ditherMethod = method }
        fun noDither() = apply { this.enableDither = false }
        fun groups(vararg groups: String) = apply { this.blockGroups = groups.toSet() }
        fun groups(groups: Set<String>) = apply { this.blockGroups = groups }
        fun allGroups() = apply { this.blockGroups = setOf("wool", "concrete", "terracotta", "stone", "wood", "soil", "jewel") }
        fun concreteOnly() = apply { this.blockGroups = setOf("concrete") }
        fun woolOnly() = apply { this.blockGroups = setOf("wool") }
        fun terracottaOnly() = apply { this.blockGroups = setOf("terracotta") }
        fun brightness(value: Int) = apply { this.brightness = value.coerceIn(0, 300) }
        fun contrast(value: Int) = apply { this.contrast = value.coerceIn(0, 300) }
        fun saturation(value: Int) = apply { this.saturation = value.coerceIn(0, 300) }
        fun preprocessing(brightness: Int, contrast: Int, saturation: Int) = apply {
            this.brightness = brightness.coerceIn(0, 300)
            this.contrast = contrast.coerceIn(0, 300)
            this.saturation = saturation.coerceIn(0, 300)
        }
        fun transparency(tolerance: Int = 128) = apply {
            this.transparencyEnabled = true
            this.transparencyTolerance = tolerance.coerceIn(0, 255)
        }
        fun staircase2D() = apply { this.staircaseMode = 0 }
        fun staircase3DClassic() = apply { this.staircaseMode = 1 }
        fun staircase3DValley() = apply { this.staircaseMode = 2 }
        fun customBlock(name: String, r: Int, g: Int, b: Int, group: String = "custom") = apply {
            this.customPalette.add(CustomBlockEntry(name, r, g, b, group))
        }
        fun customPalette(blocks: List<CustomBlockEntry>) = apply { this.customPalette.addAll(blocks) }
        fun withCommands() = apply { this.generateCommands = true }
        fun withCsv() = apply { this.generateCsv = true }
        fun withSchematicData() = apply { this.generateSchematicData = true }
        fun withAllExports() = apply { generateCommands = true; generateCsv = true; generateSchematicData = true }
        fun minecraftVersion(version: String) = apply { this.minecraftVersion = version }
        fun direction(direction: String) = apply { this.weDirection = direction }
        fun supportBlock(block: String) = apply { this.supportBlock = block }
        fun ignoreHeightLimit() = apply { this.ignoreHeightLimit = true }

        fun build(): PixelArtRequest {
            val img = image ?: throw IllegalStateException("Image must be set")
            return PixelArtRequest(
                image = img, targetWidth = targetWidth, ditherMethod = ditherMethod,
                blockGroups = blockGroups, brightness = brightness, contrast = contrast,
                saturation = saturation, transparencyEnabled = transparencyEnabled,
                transparencyTolerance = transparencyTolerance, staircaseMode = staircaseMode,
                customPalette = customPalette.toList(), enableDither = enableDither,
                generateCommands = generateCommands, generateCsv = generateCsv,
                generateSchematicData = generateSchematicData, minecraftVersion = minecraftVersion,
                weDirection = weDirection, supportBlock = supportBlock, ignoreHeightLimit = ignoreHeightLimit
            )
        }
    }

    companion object {
        @JvmStatic fun builder(): Builder = Builder()
        @JvmStatic fun quick(image: Bitmap, width: Int = 128): PixelArtRequest = Builder().image(image).width(width).build()
        @JvmStatic fun fastConcrete(image: Bitmap, width: Int = 128): PixelArtRequest = Builder().image(image).width(width).concreteOnly().noDither().build()
    }
}
