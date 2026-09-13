package io.github.moxisuki.blockprint.cat.app.feature.community

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.feature.community.data.CmsBaseUrl
import io.github.moxisuki.blockprint.cat.app.feature.community.data.CmsListItem
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsBlueprintSummary
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsCommunityCategory
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsBaseUrl
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
internal data class CommunityState(
    val selectedSource: CommunitySourceUi = CommunitySourceUi.MCS,
    val mcs: CommunityMcsState = CommunityMcsState(),
    val cms: CommunityCmsState = CommunityCmsState(),
    val visibleOverviewSources: Set<CommunitySourceUi> = setOf(CommunitySourceUi.MCS),
) {
    val activeContent: CommunitySourceContent
        get() = when (selectedSource) {
            CommunitySourceUi.MCS -> mcs.toSourceContent()
            CommunitySourceUi.CMS -> cms.toSourceContent()
        }
}

@Immutable
internal data class CommunityMcsState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<CommunityBlueprintUiItem> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val filter: String = "",
    val searchDraft: String = "",
    val selectedTopics: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    val categories: List<McsCommunityCategory> = emptyList(),
    val selectedCategorySlug: String? = null,
    val isSearchExpanded: Boolean = false,
    val errorMessage: String? = null,
)

@Immutable
internal data class CommunityCmsState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<CommunityBlueprintUiItem> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val filter: String = "",
    val searchDraft: String = "",
    val selectedTopics: List<String> = emptyList(),
    val topics: List<String> = listOf("Storage", "Farm", "Train", "Factory", "Decoration", "Survival"),
    val isSearchExpanded: Boolean = false,
    val errorMessage: String? = null,
) {
    fun toSourceContent(): CommunitySourceContent = CommunitySourceContent(
        source = CommunitySourceUi.CMS,
        total = total,
        topics = (selectedTopics + topics).distinct().take(12),
        items = items,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        hasMore = hasMore,
        errorMessage = errorMessage,
        filter = filter,
        searchDraft = searchDraft,
        selectedTopics = selectedTopics,
        isSearchExpanded = isSearchExpanded,
    )
}

internal fun CommunityMcsState.toSourceContent(): CommunitySourceContent {
    val visibleTopics = (selectedTopics + topics)
        .distinct()
        .take(12)
    return CommunitySourceContent(
        source = CommunitySourceUi.MCS,
        total = total,
        topics = visibleTopics,
        items = items,
        isLoading = isLoading,
        isRefreshing = isRefreshing,
        hasMore = hasMore,
        errorMessage = errorMessage,
        filter = filter,
        searchDraft = searchDraft,
        selectedTopics = selectedTopics,
        isSearchExpanded = isSearchExpanded,
    )
}

internal fun McsBlueprintSummary.toCommunityItem(index: Int): CommunityBlueprintUiItem =
    CommunityBlueprintUiItem(
        id = id,
        source = CommunitySourceUi.MCS,
        title = title.ifBlank { id },
        author = author.displayName.ifBlank { author.id.ifBlank { "MCS" } },
        downloads = engagement.downloadCount,
        dimensions = null,
        format = currentVersion.sourceFormat.toCommunityBlueprintFormat(),
        formatLabel = currentVersion.sourceFormat.takeIf { it.isNotBlank() },
        tags = namespaces,
        description = description,
        updateTime = updatedAt.ifBlank { createdAt }.toCommunityDisplayTime(),
        gameVersion = gameVersion.release.takeIf { it.isNotBlank() },
        coverUrl = previewUrl,
        downloadable = true,
        webUrl = "$McsBaseUrl/blueprints/$id",
        versionNumber = currentVersion.number,
        categoryName = category?.name,
        categorySlug = category?.slug,
        accentIndex = index,
    )

internal fun CmsListItem.toCommunityItem(index: Int): CommunityBlueprintUiItem =
    CommunityBlueprintUiItem(
        id = detailId.toString(),
        source = CommunitySourceUi.CMS,
        title = title.ifBlank { "CMS #$detailId" },
        author = author.ifBlank { "Creative Mechanic" },
        downloads = downloads,
        sizeText = size.takeIf { it.isNotBlank() },
        format = BlueprintFormat.Nbt,
        tags = emptyList(),
        description = description,
        updateTime = displayDate.ifBlank { datetime.toCommunityDisplayTime() },
        stress = stress.takeIf { it.isNotBlank() },
        coverUrl = coverUrl?.toAbsoluteCmsUrl(),
        downloadable = true,
        webUrl = "$CmsBaseUrl/detail/$detailId/",
        accentIndex = index + 3,
    )

internal fun String.toCommunityBlueprintFormat(): BlueprintFormat =
    when (lowercase()) {
        "litematic", "litematica" -> BlueprintFormat.Litematica
        "schem", "schematic", "sponge" -> BlueprintFormat.Schematic
        "nbt", "structure" -> BlueprintFormat.Nbt
        "json", "buildinghelper", "building_helper" -> BlueprintFormat.BuildingHelper
        else -> BlueprintFormat.Unknown
    }

private fun String.toAbsoluteCmsUrl(): String =
    when {
        startsWith("http://") || startsWith("https://") -> this
        startsWith("/") -> "$CmsBaseUrl$this"
        else -> "$CmsBaseUrl/$this"
    }

internal fun String.toCommunityDisplayTime(): String {
    if (isBlank()) return ""
    val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.getDefault())
    return runCatching {
        Instant.parse(this).atZone(ZoneId.systemDefault()).format(formatter)
    }.recoverCatching {
        LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME).format(formatter)
    }.getOrElse { this }
}
