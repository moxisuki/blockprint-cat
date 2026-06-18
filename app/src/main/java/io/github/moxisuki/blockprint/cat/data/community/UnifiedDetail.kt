package io.github.moxisuki.blockprint.cat.data.community

import androidx.compose.ui.graphics.ImageBitmap
import io.github.moxisuki.blockprint.cat.data.SchematicComment

data class UnifiedDetail(
    val source: CommunitySource,
    val id: String,
    val markdown: String = "",
    val coverUrl: String? = null,
    val previewBitmap: ImageBitmap? = null,
    val previewLoading: Boolean = false,
    val previewMissing: Boolean = false,
    val requirements: List<UnifiedMaterial> = emptyList(),
    val production: List<UnifiedMaterial> = emptyList(),
    val dependencies: List<String> = emptyList(),
    val stress: String? = null,
    val comments: List<SchematicComment> = emptyList(),
    val headerSnapshot: UnifiedSchematic? = null,
    val downloadable: Boolean = true,
    val cmsDownloadId: Int? = null,
    val error: io.github.moxisuki.blockprint.cat.data.error.AppError? = null,
    val loading: Boolean = true,
)
