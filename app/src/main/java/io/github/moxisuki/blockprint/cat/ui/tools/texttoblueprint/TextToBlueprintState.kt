package io.github.moxisuki.blockprint.cat.ui.tools.texttoblueprint

data class TextToBlueprintState(
    val text: String = "",
    val selectedBlockId: String? = DEFAULT_BLOCK,
    val scale: Int = DEFAULT_SCALE,
    val spacing: Int = DEFAULT_SPACING,
    val height: Int = DEFAULT_HEIGHT,
    val useTtf: Boolean = false,
    val grid: Array<Array<String?>> = Array(0) { arrayOfNulls(0) },
    val gridW: Int = 0,
    val gridH: Int = 0,
    val isUpdating: Boolean = false,
    val resultBitmap: android.graphics.Bitmap? = null,
    val resultTotalBlocks: Int = 0,
    val resultMaterialCounts: Map<String, Int> = emptyMap(),
    val exportPayload: String? = null,
) {
    companion object {
        const val DEFAULT_BLOCK = "white_wool"
        const val DEFAULT_SCALE = 1
        const val DEFAULT_SPACING = 1
        const val DEFAULT_HEIGHT = 8
        const val MIN_SCALE = 1; const val MAX_SCALE = 8
        const val MIN_SPACING = 0; const val MAX_SPACING = 8
        const val MIN_HEIGHT = 4; const val MAX_HEIGHT = 32
    }
}
