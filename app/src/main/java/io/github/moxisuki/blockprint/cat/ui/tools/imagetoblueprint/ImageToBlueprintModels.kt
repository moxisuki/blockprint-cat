@file:Suppress("unused")

package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

import android.net.Uri
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlueprintUiDefaults
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockGroup

/**
 * 向后兼容 shim：原本在 ITB 私有实现的枚举/数据类已迁移到
 * [io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon]，这里只做 typealias
 * 转发，让旧 import（以及测试）继续工作。
 *
 * 数值常量（MIN_WIDTH/MAX_ADJUST 等）请改用 [io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlueprintUiDefaults]。
 *
 * [ImageToBlueprintState] 仍保留在 ITB 私有（TTB 有自己的状态结构），默认值已切换到 BlueprintUiDefaults。
 */
typealias DitherMethod = io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.DitherMethod
typealias BlockGroup = io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockGroup
typealias BlockEntry = io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockEntry
typealias BlockCatalog = io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockCatalog
typealias PreviewMode = io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.PreviewMode

/**
 * ITB 专用 UI 状态。TTB 有自己的状态（[io.github.moxisuki.blockprint.cat.ui.tools.texttoblueprint.TextToBlueprintState]），
 * 因为 TTB 没有透明度/亮度/对比度/饱和度，多了 text/fontSize。
 */
data class ImageToBlueprintState(
    val imageUri: Uri? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val targetWidth: Int = BlueprintUiDefaults.DEFAULT_WIDTH,
    val ditherMethod: DitherMethod = DitherMethod.DEFAULT,
    val brightness: Int = BlueprintUiDefaults.DEFAULT_ADJUST,
    val contrast: Int = BlueprintUiDefaults.DEFAULT_ADJUST,
    val saturation: Int = BlueprintUiDefaults.DEFAULT_ADJUST,
    val transparencyEnabled: Boolean = false,
    val transparencyTolerance: Int = BlueprintUiDefaults.DEFAULT_TOLERANCE,
    val selectedGroups: Set<BlockGroup> = BlueprintUiDefaults.DEFAULT_GROUPS,
    val previewMode: PreviewMode = PreviewMode.Source,
    val isUpdating: Boolean = false,
    val lastUpdatedAt: Long = 0L,
    val resultBitmap: android.graphics.Bitmap? = null,
    val resultWidth: Int = 0,
    val resultHeight: Int = 0,
    val resultTotalBlocks: Int = 0,
    val resultMaterialCounts: Map<String, Int> = emptyMap(),
    val errorMessage: String? = null,
    val commandDirection: io.github.moxisuki.pixelart.api.ExportApi.CommandDirection = io.github.moxisuki.pixelart.api.ExportApi.CommandDirection.ES,
    val commandsText: String = "",
) {
    companion object {
        const val MIN_ADJUST = BlueprintUiDefaults.MIN_ADJUST
        const val MAX_ADJUST = BlueprintUiDefaults.MAX_ADJUST
        const val DEFAULT_ADJUST = BlueprintUiDefaults.DEFAULT_ADJUST
        const val MIN_WIDTH = BlueprintUiDefaults.MIN_WIDTH
        const val MAX_WIDTH = BlueprintUiDefaults.MAX_WIDTH
        const val DEFAULT_WIDTH = BlueprintUiDefaults.DEFAULT_WIDTH
        const val MIN_TOLERANCE = BlueprintUiDefaults.MIN_TOLERANCE
        const val MAX_TOLERANCE = BlueprintUiDefaults.MAX_TOLERANCE
        const val DEFAULT_TOLERANCE = BlueprintUiDefaults.DEFAULT_TOLERANCE
    }
}
