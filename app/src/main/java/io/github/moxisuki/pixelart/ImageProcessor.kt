package io.github.moxisuki.pixelart

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.InputStream

object ImageProcessor {

    fun loadImage(path: String): Bitmap = BitmapFactory.decodeFile(path)

    fun loadImage(input: InputStream): Bitmap = BitmapFactory.decodeStream(input)

    fun loadImage(file: File): Bitmap = BitmapFactory.decodeFile(file.absolutePath)

    fun getPixels(image: Bitmap): Array<IntArray> {
        val width = image.width
        val height = image.height
        val pixels = Array(height) { IntArray(width) }
        for (y in 0 until height) for (x in 0 until width) pixels[y][x] = image.getPixel(x, y)
        return pixels
    }

    fun getRgbChannels(image: Bitmap): Triple<Array<FloatArray>, Array<FloatArray>, Array<FloatArray>> {
        val width = image.width
        val height = image.height
        val r = Array(height) { FloatArray(width) }
        val g = Array(height) { FloatArray(width) }
        val b = Array(height) { FloatArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = image.getPixel(x, y)
                r[y][x] = (rgb shr 16 and 0xFF).toFloat()
                g[y][x] = (rgb shr 8 and 0xFF).toFloat()
                b[y][x] = (rgb and 0xFF).toFloat()
            }
        }
        return Triple(r, g, b)
    }

    fun pixelsToImage(r: Array<FloatArray>, g: Array<FloatArray>, b: Array<FloatArray>): Bitmap {
        val height = r.size
        val width = r[0].size
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val cr = ColorUtils.clamp(r[y][x].toInt())
                val cg = ColorUtils.clamp(g[y][x].toInt())
                val cb = ColorUtils.clamp(b[y][x].toInt())
                bitmap.setPixel(x, y, (0xFF shl 24) or (cr shl 16) or (cg shl 8) or cb)
            }
        }
        return bitmap
    }

    fun resizeNearestNeighbor(image: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(image, targetWidth, targetHeight, false)
    }

    fun adjustColors(image: Bitmap, brightness: Int = 100, contrast: Int = 100, saturation: Int = 100): Bitmap {
        if (brightness == 100 && contrast == 100 && saturation == 100) return image
        val width = image.width; val height = image.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bf = brightness / 100f; val cf = contrast / 100f; val sf = saturation / 100f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val rgb = image.getPixel(x, y)
                var r = (rgb shr 16 and 0xFF).toFloat()
                var g = (rgb shr 8 and 0xFF).toFloat()
                var b = (rgb and 0xFF).toFloat()

                r *= bf; g *= bf; b *= bf
                r = (r - 128f) * cf + 128f; g = (g - 128f) * cf + 128f; b = (b - 128f) * cf + 128f
                val max = maxOf(r, g, b); val min = minOf(r, g, b); val l = (max + min) / 2f
                if (max != min) {
                    val d = max - min
                    val s = if (l > 128f) d / (510f - max - min) else d / (max + min)
                    val as2 = s * sf
                    if (as2 != s) {
                        val nm: Float; val nx: Float
                        if (l < 128f) { val t = as2 * l; nm = l + t; nx = l - t }
                        else { val t = as2 * (255f - l); nm = l + t; nx = l - t }
                        val ratio = if (d != 0f) (nm - nx) / d else 1f
                        val mid = (max + min) / 2f
                        r = (r - mid) * ratio + mid; g = (g - mid) * ratio + mid; b = (b - mid) * ratio + mid
                    }
                }
                val cr = ColorUtils.clamp(r.toInt())
                val cg = ColorUtils.clamp(g.toInt())
                val cb = ColorUtils.clamp(b.toInt())
                result.setPixel(x, y, (0xFF shl 24) or (cr shl 16) or (cg shl 8) or cb)
            }
        }
        return result
    }
}
