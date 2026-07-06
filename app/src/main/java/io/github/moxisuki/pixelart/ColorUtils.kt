package io.github.moxisuki.pixelart

import kotlin.math.pow
import kotlin.math.sqrt

/** 颜色空间转换与色差计算工具 */
object ColorUtils {

    /** CIE L*a*b* 颜色空间坐标 */
    data class Lab(val l: Double, val a: Double, val b: Double)

    /**
     * RGB → CIE L*a*b* 转换。
     * 先将 sRGB 线性化，再经 XYZ 中转至 Lab。
     */
    fun rgbToLab(red: Int, green: Int, blue: Int): Lab {
        var r = red / 255.0
        var g = green / 255.0
        var b = blue / 255.0

        r = if (r > 0.04045) ((r + 0.055) / 1.055).pow(2.4) else r / 12.92
        g = if (g > 0.04045) ((g + 0.055) / 1.055).pow(2.4) else g / 12.92
        b = if (b > 0.04045) ((b + 0.055) / 1.055).pow(2.4) else b / 12.92

        var x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
        var y = (r * 0.2126 + g * 0.7152 + b * 0.0722) / 1.00000
        var z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883

        x = if (x > 0.008856) x.pow(1.0 / 3.0) else (7.787 * x) + (16.0 / 116.0)
        y = if (y > 0.008856) y.pow(1.0 / 3.0) else (7.787 * y) + (16.0 / 116.0)
        z = if (z > 0.008856) z.pow(1.0 / 3.0) else (7.787 * z) + (16.0 / 116.0)

        return Lab(
            l = (116.0 * y) - 16.0,
            a = 500.0 * (x - y),
            b = 200.0 * (y - z)
        )
    }

    /** CIE76 色差公式，值越小越接近 */
    fun deltaE76(rgb1: Triple<Int, Int, Int>, rgb2: Triple<Int, Int, Int>): Double {
        val lab1 = rgbToLab(rgb1.first, rgb1.second, rgb1.third)
        val lab2 = rgbToLab(rgb2.first, rgb2.second, rgb2.third)
        return sqrt((lab1.l - lab2.l).pow(2) + (lab1.a - lab2.a).pow(2) + (lab1.b - lab2.b).pow(2))
    }

    /**
     * 加权 RGB 欧几里得距离，对红色分量权重更高以模拟人眼感知。
     * 计算速度快，适合大量像素的颜色匹配。
     */
    fun weightedRgbDistance(rgb1: Triple<Int, Int, Int>, rgb2: Triple<Int, Int, Int>): Double {
        val rMean = (rgb1.first + rgb2.first) / 2.0
        val dr = (rgb1.first - rgb2.first).toDouble()
        val dg = (rgb1.second - rgb2.second).toDouble()
        val db = (rgb1.third - rgb2.third).toDouble()
        val rWeight = 2.0 + rMean / 256.0
        val gWeight = 4.0
        val bWeight = 2.0 + (255.0 - rMean) / 256.0
        return sqrt(rWeight * dr * dr + gWeight * dg * dg + bWeight * db * db)
    }

    /** 整数值裁剪到 [0, 255] */
    fun clamp(value: Int, min: Int = 0, max: Int = 255): Int =
        value.coerceIn(min, max)

    /** 浮点值裁剪到 [0, 255] */
    fun clampFloat(value: Float, min: Float = 0f, max: Float = 255f): Float =
        value.coerceIn(min, max)
}
