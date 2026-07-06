package io.github.moxisuki.pixelart

import android.graphics.Bitmap

data class ConversionResult(
    val width: Int,
    val height: Int,
    val blocks: Array<Array<Block?>>,
    val outputImage: Bitmap
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConversionResult) return false
        return width == other.width && height == other.height && blocks.contentDeepEquals(other.blocks)
    }

    override fun hashCode(): Int {
        var result = width; result = 31 * result + height; result = 31 * result + blocks.contentDeepHashCode(); return result
    }
}

object PixelArtConverter {

    fun convert(image: Bitmap, options: ConversionOptions = ConversionOptions(), customBlocks: List<Block> = emptyList()): ConversionResult {
        val availableBlocks = BlockPalette.filterByGroups(options.blockGroups) + customBlocks
        return convertWithBlocks(image, options, availableBlocks)
    }

    fun convert(image: Bitmap, options: ConversionOptions = ConversionOptions(), selector: BlockSelector): ConversionResult {
        val availableBlocks = selector.selectedBlocks.toList()
        return convertWithBlocks(image, options, availableBlocks)
    }

    private fun convertWithBlocks(image: Bitmap, options: ConversionOptions, availableBlocks: List<Block>): ConversionResult {
        val targetHeight = image.height * options.targetWidth / image.width
        val resized = ImageProcessor.resizeNearestNeighbor(image, options.targetWidth, targetHeight)
        val adjusted = ImageProcessor.adjustColors(resized, options.brightness, options.contrast, options.saturation)
        val (r, g, b) = ImageProcessor.getRgbChannels(adjusted)
        val width = adjusted.width; val height = adjusted.height

        require(availableBlocks.isNotEmpty()) { "No blocks available for conversion" }

        val rD = Array(height) { y -> FloatArray(width) { x -> r[y][x] } }
        val gD = Array(height) { y -> FloatArray(width) { x -> g[y][x] } }
        val bD = Array(height) { y -> FloatArray(width) { x -> b[y][x] } }
        Dither.applyDither(rD, width, height, options.ditherMethod)
        Dither.applyDither(gD, width, height, options.ditherMethod)
        Dither.applyDither(bD, width, height, options.ditherMethod)

        val blockGrid: Array<Array<Block?>> = Array(height) { arrayOfNulls(width) }
        val outR = Array(height) { FloatArray(width) }; val outG = Array(height) { FloatArray(width) }; val outB = Array(height) { FloatArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (options.transparencyEnabled) {
                    if ((adjusted.getPixel(x, y) shr 24 and 0xFF) < options.transparencyTolerance) {
                        blockGrid[y][x] = null; outR[y][x] = 0f; outG[y][x] = 0f; outB[y][x] = 0f; continue
                    }
                }
                val pixelColor = Triple(rD[y][x].toInt().coerceIn(0, 255), gD[y][x].toInt().coerceIn(0, 255), bD[y][x].toInt().coerceIn(0, 255))
                val nearest = findNearestBlock(pixelColor, availableBlocks)
                blockGrid[y][x] = nearest
                outR[y][x] = nearest.rgb.first.toFloat(); outG[y][x] = nearest.rgb.second.toFloat(); outB[y][x] = nearest.rgb.third.toFloat()
            }
        }
        val outputImage = ImageProcessor.pixelsToImage(outR, outG, outB)
        return ConversionResult(width, height, blockGrid, outputImage)
    }

    fun findNearestBlock(pixelColor: Triple<Int, Int, Int>, candidates: List<Block>): Block =
        candidates.minByOrNull { ColorUtils.weightedRgbDistance(pixelColor, it.rgb) } ?: error("No candidate blocks available")

    fun getMaterialList(result: ConversionResult): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        for (y in 0 until result.height)
            for (x in 0 until result.width)
                result.blocks[y][x]?.let { counts[it.name] = counts.getOrDefault(it.name, 0) + 1 }
        return counts.toSortedMap()
    }
}
