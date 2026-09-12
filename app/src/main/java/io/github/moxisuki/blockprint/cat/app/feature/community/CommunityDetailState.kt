package io.github.moxisuki.blockprint.cat.app.feature.community

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialItem

@Immutable
internal data class CommunityDetailSeed(
    val source: String,
    val blueprintId: String,
    val title: String,
    val author: String,
    val format: BlueprintFormat,
    val description: String,
    val heat: Int?,
    val downloads: Int?,
    val dimensions: String?,
    val sizeText: String?,
    val stress: String?,
    val updateTime: String,
    val coverUrl: String?,
    val tags: List<String>,
    val downloadable: Boolean,
    val webUrl: String?,
)

@Immutable
internal data class CommunityDetailPayload(
    val markdown: String = "",
    val materials: List<BlueprintDetailMaterialItem> = emptyList(),
    val coverUrl: String? = null,
    val downloadable: Boolean? = null,
    val dimensions: String? = null,
    val sizeText: String? = null,
    val stress: String? = null,
)

@Immutable
internal data class CommunityDetailState(
    val seed: CommunityDetailSeed = CommunityDetailSeed(
        source = "",
        blueprintId = "",
        title = "",
        author = "",
        format = BlueprintFormat.Unknown,
        description = "",
        heat = null,
        downloads = null,
        dimensions = null,
        sizeText = null,
        stress = null,
        updateTime = "",
        coverUrl = null,
        tags = emptyList(),
        downloadable = true,
        webUrl = null,
    ),
    val payload: CommunityDetailPayload = CommunityDetailPayload(),
    val isLoadingDetail: Boolean = false,
    val detailErrorMessage: String? = null,
    val downloadState: CommunityDownloadState = CommunityDownloadState.Idle,
    val downloadProgress: Float? = null,
    val downloadMessage: String? = null,
) {
    val description: String
        get() = payload.markdown.ifBlank { seed.description }

    val materials: List<BlueprintDetailMaterialItem>
        get() = payload.materials
}

internal enum class CommunityDownloadState {
    Idle,
    Downloading,
    Downloaded,
    Failed,
}
