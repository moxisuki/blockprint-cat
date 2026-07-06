package io.github.moxisuki.pixelart

typealias BlockUseFlag = Map<String, Boolean>
typealias GroupButtonFlag = Map<String, Boolean?>

/**
 * 方块选择器，管理分组开关与过滤器状态。
 *
 * 初始状态默认启用 [BlockGroups.DEFAULT] 中的 5 个分组。
 * 提供 5 种过滤器对标在线工具的行标签页。
 *
 * 使用示例：
 * ```kotlin
 * val selector = BlockSelector()
 *     .selectGroups(setOf("concrete", "wool"))
 *     .survivalOnly()
 * val result = PixelArtConverter.convert(image, options, selector)
 * ```
 */
class BlockSelector(
    private val allBlocks: List<Block> = BlockPalette.blocks
) {
    private val blockUseFlag: MutableMap<String, Boolean> = mutableMapOf()
    private val groupButtonFlag: MutableMap<String, Boolean?> = mutableMapOf()

    init { selectGroups(BlockGroups.DEFAULT) }

    /** 当前启用的方块列表 */
    val selectedBlocks: List<Block> get() = allBlocks.filter { blockUseFlag[it.name] == true }

    /** 完全启用的分组名 */
    val enabledGroupNames: Set<String> get() = groupButtonFlag.filter { it.value == true }.keys

    /** 各分组已启用方块数 */
    val blockCountByGroup: Map<String, Int> get() = selectedBlocks.groupingBy { it.group }.eachCount()

    /** 按分组名初始化，选中分组内全部方块 */
    fun selectGroups(groups: Collection<String>): BlockSelector {
        for (b in allBlocks) if (b.group != "air") { blockUseFlag[b.name] = b.group in groups; groupButtonFlag[b.group] = b.group in groups }
        return this
    }

    /** 切换整个分组的启用状态 */
    fun toggleGroup(group: String): BlockSelector {
        val v = (groupButtonFlag[group] ?: false) xor true; groupButtonFlag[group] = v
        for (b in allBlocks) if (b.group == group) blockUseFlag[b.name] = v
        return this
    }

    /** 切换单个方块 */
    fun toggleBlock(blockName: String): BlockSelector { blockUseFlag[blockName] = (blockUseFlag[blockName] ?: false) xor true; updateGroupFlag(blockName); return this }

    /** 设置单个方块启用状态 */
    fun setBlockEnabled(blockName: String, enabled: Boolean): BlockSelector { blockUseFlag[blockName] = enabled; updateGroupFlag(blockName); return this }

    /**
     * 应用过滤器，对标网页 5 个筛选复选框。
     *
     * survival/luminance/redstone 为反转逻辑（勾选=隐藏非目标方块），
     * falling/transparent 为正向逻辑（勾选=仅显示目标方块）。
     */
    fun applyFilter(filter: BlockFilter, checked: Boolean): BlockSelector {
        for (b in allBlocks) {
            if (b.group == "air") continue
            val matched = when (filter) {
                BlockFilter.FALLING -> b.falling
                BlockFilter.TRANSPARENT -> b.group == "glass" || b.name.contains("ice") || b.name.contains("glass")
                BlockFilter.SURVIVAL -> !b.survivalObtainable
                BlockFilter.LUMINANCE -> b.luminance
                BlockFilter.REDSTONE -> b.redstone
            }
            if (matched) blockUseFlag[b.name] = when (filter) {
                BlockFilter.SURVIVAL, BlockFilter.LUMINANCE, BlockFilter.REDSTONE -> !checked; else -> checked
            }
        }
        refreshGroupFlags(); return this
    }

    fun survivalOnly(enabled: Boolean = true) = applyFilter(BlockFilter.SURVIVAL, enabled)

    fun excludeFalling(enabled: Boolean = true) = applyFilter(BlockFilter.FALLING, enabled)

    fun transparentOnly(enabled: Boolean = true) = applyFilter(BlockFilter.TRANSPARENT, enabled)

    fun luminanceOnly(enabled: Boolean = true) = applyFilter(BlockFilter.LUMINANCE, enabled)

    fun redstoneOnly(enabled: Boolean = true) = applyFilter(BlockFilter.REDSTONE, enabled)

    fun isBlockEnabled(blockName: String): Boolean = blockUseFlag[blockName] ?: false

    fun isGroupEnabled(group: String): Boolean = groupButtonFlag[group] == true

    fun getBlockUseFlag(): BlockUseFlag = blockUseFlag.toMap()

    fun getGroupButtonFlag(): GroupButtonFlag = groupButtonFlag.toMap()

    /** 深拷贝当前选择状态 */
    fun snapshot(): BlockSelector = BlockSelector(allBlocks).also { it.blockUseFlag.putAll(blockUseFlag); it.groupButtonFlag.putAll(groupButtonFlag) }

    private fun updateGroupFlag(blockName: String) {
        val block = allBlocks.find { it.name == blockName } ?: return
        val gb = allBlocks.filter { it.group == block.group }
        val all = gb.all { blockUseFlag[it.name] == true }; val any = gb.any { blockUseFlag[it.name] == true }
        groupButtonFlag[block.group] = if (all) true else if (!any) false else null
    }

    private fun refreshGroupFlags() {
        for ((g, gb) in allBlocks.filter { it.group != "air" }.groupBy { it.group }) {
            val all = gb.all { blockUseFlag[it.name] == true }; val any = gb.any { blockUseFlag[it.name] == true }
            if (all) groupButtonFlag[g] = true else if (!any) groupButtonFlag[g] = false
        }
    }
}
