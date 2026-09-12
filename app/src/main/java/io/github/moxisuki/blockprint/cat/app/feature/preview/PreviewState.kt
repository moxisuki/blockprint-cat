package io.github.moxisuki.blockprint.cat.app.feature.preview

import io.github.moxisuki.blockprint.cat.app.core.preview.PreviewModel
import io.github.moxisuki.blockprint.cat.app.core.preview.PreviewStage

data class PreviewState(
    val blueprintId: String = "",
    val title: String = "",
    val model: PreviewModel? = null,
    val stage: PreviewStage = PreviewStage.Preparing,
    val progress: Float = 0f,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
