package io.github.moxisuki.pixelart

/**
 * 单个方块的完整描述，包含颜色、分类及可选属性标签。
 *
 * @property name 方块标识名（如 "white_wool"）
 * @property rgb 平均颜色 (R, G, B)
 * @property group 所属分类（wool/concrete/terracotta/stone/soil/wood/jewel）
 * @property survivalObtainable 生存模式下是否可获得
 * @property luminance 是否发光
 * @property redstone 是否为红石组件
 * @property falling 是否受重力影响
 * @property transparent 是否透明
 * @property version 最低版本兼容号，默认 99 表示全版本
 */
data class Block(
    val name: String,
    val rgb: Triple<Int, Int, Int>,
    val group: String,
    val survivalObtainable: Boolean = true,
    val luminance: Boolean = false,
    val redstone: Boolean = false,
    val falling: Boolean = false,
    val transparent: Boolean = false,
    val version: Int = 99
)

/**
 * 像素画转换的全部可控参数。
 *
 * @property targetWidth 水平方块数，16~2048，默认 128
 * @property ditherMethod 抖动算法，默认 [DitherMethod.FLOYD_STEINBERG]
 * @property blockGroups 启用的方块分类集合
 * @property brightness 亮度 0~300，默认 100（100=原始）
 * @property contrast 对比度 0~300，默认 100
 * @property saturation 饱和度 0~300，默认 100
 * @property transparencyEnabled 是否启用透明检测
 * @property transparencyTolerance 透明容差 0~255
 * @property staircaseMode 地图画模式：0=2D 平面，1=3D 经典，2=3D 山谷
 */
data class ConversionOptions(
    val targetWidth: Int = 128,
    val ditherMethod: DitherMethod = DitherMethod.FLOYD_STEINBERG,
    val blockGroups: Set<String> = setOf("wool", "concrete", "terracotta", "stone", "wood"),
    val brightness: Int = 100,
    val contrast: Int = 100,
    val saturation: Int = 100,
    val transparencyEnabled: Boolean = true,
    val transparencyTolerance: Int = 128,
    val staircaseMode: Int = 0
)

/**
 * 抖动算法枚举，ID 与网页版保持一致。
 *
 * 误差扩散类适合照片（平滑过渡），有序抖动类适合图标/Logo（边界清晰）。
 */
enum class DitherMethod(val id: Int) {
    NONE(0),
    FLOYD_STEINBERG(1),
    BAYER_4X4(2),
    BAYER_2X2(3),
    ORDERED_3X3(4),
    MIN_AVG_ERR(5),
    BURKES(6),
    SIERRA_LITE(7),
    STUCKI(8),
    ATKINSON(9);

    companion object {
        fun fromId(id: Int): DitherMethod =
            entries.find { it.id == id } ?: FLOYD_STEINBERG
    }
}

/**
 * 方块过滤器，对应网页版 5 个筛选复选框。
 */
enum class BlockFilter(val label: String) {
    FALLING("falling"),
    TRANSPARENT("transparent"),
    SURVIVAL("survival"),
    LUMINANCE("luminance"),
    REDSTONE("redstone")
}

/** 方块分类常量 */
object BlockGroups {
    val ALL = listOf("wool", "concrete", "terracotta", "stone", "soil", "wood", "jewel", "glass", "light", "ore")
    val DEFAULT = listOf("wool", "concrete", "terracotta", "stone", "wood")
}

/**
 * 抖动矩阵定义，用于有序抖动和误差扩散。
 *
 * `matrix` 中心为当前像素，(0,0) 为左上角。
 * `divisor` 是归一化除数。
 */
data class DitherMatrix(
    val matrix: Array<IntArray>,
    val divisor: Int,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DitherMatrix) return false
        return matrix.contentDeepEquals(other.matrix) && divisor == other.divisor
    }

    override fun hashCode(): Int {
        var result = matrix.contentDeepHashCode()
        result = 31 * result + divisor
        return result
    }
}
