package io.github.moxisuki.blockprint.cat.app.feature.tools.texttoblueprint

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolPreviewMode
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolBlockPalette
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolGrid

@Immutable
internal data class TextToBlueprintState(
    val text: String = "",
    val selectedBlockId: String = ToolBlockPalette.defaultBlockId,
    val fontSize: Int = 34,
    val padding: Int = 2,
    val previewMode: ToolPreviewMode = ToolPreviewMode.Result,
    val grid: ToolGrid? = null,
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
)
