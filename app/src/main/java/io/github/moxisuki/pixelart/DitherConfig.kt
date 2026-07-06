package io.github.moxisuki.pixelart

/** 9 种抖动算法的矩阵配置 */
object DitherConfig {

    private fun intArrayOf(vararg elements: Int): IntArray = elements

    /** 获取指定抖动方法的矩阵定义 */
    val matrices: Map<DitherMethod, DitherMatrix> = mapOf(
        DitherMethod.NONE to DitherMatrix(
            matrix = arrayOf(intArrayOf(0, 0, 0, 0, 0), intArrayOf(0, 0, 0, 0, 0)),
            divisor = 1, width = 0, height = 0
        ),
        DitherMethod.FLOYD_STEINBERG to DitherMatrix(
            matrix = arrayOf(
                intArrayOf(0, 0, 0, 7, 0),
                intArrayOf(0, 3, 5, 1, 0),
                intArrayOf(0, 0, 0, 0, 0)
            ),
            divisor = 16, width = 5, height = 3
        ),
        DitherMethod.BAYER_4X4 to DitherMatrix(
            matrix = arrayOf(
                intArrayOf(1, 9, 3, 11),
                intArrayOf(13, 5, 15, 7),
                intArrayOf(4, 12, 2, 10),
                intArrayOf(16, 8, 14, 6)
            ),
            divisor = 17, width = 4, height = 4
        ),
        DitherMethod.BAYER_2X2 to DitherMatrix(
            matrix = arrayOf(intArrayOf(1, 3), intArrayOf(4, 2)),
            divisor = 5, width = 2, height = 2
        ),
        DitherMethod.ORDERED_3X3 to DitherMatrix(
            matrix = arrayOf(
                intArrayOf(1, 7, 4),
                intArrayOf(5, 8, 3),
                intArrayOf(6, 2, 9)
            ),
            divisor = 10, width = 3, height = 3
        ),
        DitherMethod.MIN_AVG_ERR to DitherMatrix(
            matrix = arrayOf(
                intArrayOf(0, 0, 0, 7, 5),
                intArrayOf(3, 5, 7, 5, 3),
                intArrayOf(1, 3, 5, 3, 1)
            ),
            divisor = 48, width = 5, height = 3
        ),
        DitherMethod.BURKES to DitherMatrix(
            matrix = arrayOf(
                intArrayOf(0, 0, 0, 8, 4),
                intArrayOf(2, 4, 8, 4, 2),
                intArrayOf(0, 0, 0, 0, 0)
            ),
            divisor = 32, width = 5, height = 3
        ),
        DitherMethod.SIERRA_LITE to DitherMatrix(
            matrix = arrayOf(
                intArrayOf(0, 0, 0, 2, 0),
                intArrayOf(0, 1, 1, 0, 0),
                intArrayOf(0, 0, 0, 0, 0)
            ),
            divisor = 4, width = 5, height = 3
        ),
        DitherMethod.STUCKI to DitherMatrix(
            matrix = arrayOf(
                intArrayOf(0, 0, 0, 8, 4),
                intArrayOf(2, 4, 8, 4, 2),
                intArrayOf(1, 2, 4, 2, 1)
            ),
            divisor = 42, width = 5, height = 3
        ),
        DitherMethod.ATKINSON to DitherMatrix(
            matrix = arrayOf(
                intArrayOf(0, 0, 0, 1, 1),
                intArrayOf(0, 1, 1, 1, 0),
                intArrayOf(0, 0, 1, 0, 0)
            ),
            divisor = 8, width = 5, height = 3
        )
    )
}
