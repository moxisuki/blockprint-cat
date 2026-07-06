package io.github.moxisuki.pixelart

/**
 * 抖动执行引擎，支持误差扩散与有序抖动两大类算法。
 *
 * 误差扩散：将当前像素的量化误差按权重分配到邻域像素，
 *           适合照片（过渡平滑）。
 * 有序抖动：用固定阈值矩阵与像素亮度比较，无误差传播，
 *           适合图标/Logo（边界清晰）。
 */
object Dither {

    /**
     * 对单通道灰度图 [pixels] 执行指定算法的抖动处理。
     * 修改是原地进行的。
     */
    fun applyDither(
        pixels: Array<FloatArray>,
        width: Int,
        height: Int,
        method: DitherMethod
    ) {
        when (method) {
            DitherMethod.NONE -> { /* 无需处理 */ }
            DitherMethod.FLOYD_STEINBERG,
            DitherMethod.MIN_AVG_ERR,
            DitherMethod.BURKES,
            DitherMethod.SIERRA_LITE,
            DitherMethod.STUCKI,
            DitherMethod.ATKINSON -> errorDiffusion(pixels, width, height, method)
            DitherMethod.BAYER_4X4,
            DitherMethod.BAYER_2X2,
            DitherMethod.ORDERED_3X3 -> orderedDither(pixels, width, height, method)
        }
    }

    private fun errorDiffusion(
        pixels: Array<FloatArray>,
        width: Int,
        height: Int,
        method: DitherMethod
    ) {
        val matrix = DitherConfig.matrices[method] ?: return
        val matWidth = matrix.width
        val matHeight = matrix.height
        val divisor = matrix.divisor.toFloat()
        val originX = 2 // 误差扩散的"当前像素"列，矩阵中心
        val originY = 0

        val buf = Array(height) { y -> FloatArray(width) { x -> pixels[y][x] } }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val oldPixel = buf[y][x]
                val newPixel = if (oldPixel < 128f) 0f else 255f
                val error = oldPixel - newPixel
                buf[y][x] = newPixel

                for (my in 0 until matHeight) {
                    for (mx in 0 until matWidth) {
                        val weight = matrix.matrix[my][mx]
                        if (weight == 0) continue
                        val dx = mx - originX
                        val dy = my - originY
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until width && ny in 0 until height) {
                            buf[ny][nx] += error * weight / divisor
                        }
                    }
                }
            }
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y][x] = buf[y][x].coerceIn(0f, 255f)
            }
        }
    }

    private fun orderedDither(
        pixels: Array<FloatArray>,
        width: Int,
        height: Int,
        method: DitherMethod
    ) {
        val matrix = DitherConfig.matrices[method] ?: return
        val matWidth = matrix.width
        val matHeight = matrix.height
        val divisor = matrix.divisor.toFloat()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val threshold = matrix.matrix[y % matHeight][x % matWidth].toFloat() / divisor
                val old = pixels[y][x] / 255f
                pixels[y][x] = if (old > threshold) 255f else 0f
            }
        }
    }
}
