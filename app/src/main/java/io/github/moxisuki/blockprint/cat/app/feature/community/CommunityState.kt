package io.github.moxisuki.blockprint.cat.app.feature.community

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.persistence.McsAuthCookies
import io.github.moxisuki.blockprint.cat.app.feature.community.data.CmsBaseUrl
import io.github.moxisuki.blockprint.cat.app.feature.community.data.CmsListItem
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsBaseUrl
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsSchematic
import io.github.moxisuki.blockprint.cat.app.feature.community.data.toAbsoluteCmsUrl
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.absoluteValue

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
    val cookies: McsAuthCookies = McsAuthCookies(),
    val isCheckingLogin: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<CommunityBlueprintUiItem> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val filter: String = "",
    val searchDraft: String = "",
    val selectedTopics: List<String> = emptyList(),
    val topics: List<String> = emptyList(),
    val isSearchExpanded: Boolean = false,
    val errorMessage: String? = null,
) {
    val isLoggedIn: Boolean
        get() = cookies.isLoggedIn
}

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
        status = "CMS · Creative Mechanic Server",
        hint = if (filter.isBlank()) {
            "下拉刷新 CMS 蓝图，支持标题搜索"
        } else {
            "筛选: $filter"
        },
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
    val accountName = cookies.nickname
        .ifBlank { cookies.uuid.take(8) }
        .ifBlank { "MCS" }
    val visibleTopics = (selectedTopics + topics)
        .distinct()
        .take(12)
        .ifEmpty { listOf("litematic", "farm", "factory", "survival") }
    return CommunitySourceContent(
        source = CommunitySourceUi.MCS,
        total = total,
        status = if (isLoggedIn) "已登录 · $accountName" else "MCS · 未登录",
        hint = if (filter.isBlank()) {
            "下拉刷新社区蓝图，支持标签与标题搜索"
        } else {
            "筛选: $filter"
        },
        topics = visibleTopics,
        items = items,
        isLoggedIn = isLoggedIn,
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

internal fun McsSchematic.toCommunityItem(index: Int): CommunityBlueprintUiItem =
    CommunityBlueprintUiItem(
        id = uuid,
        source = CommunitySourceUi.MCS,
        title = name.ifBlank { uuid },
        author = nickName.ifBlank { authorUuid.ifBlank { "-" } },
        heat = heat,
        dimensions = size?.let { "${it.first} x ${it.second} x ${it.third}" },
        format = type.toCommunityBlueprintFormat(),
        tags = tags,
        description = description,
        updateTime = updateTime.ifBlank { uploadTime }.toCommunityDisplayTime(),
        coverUrl = "$McsBaseUrl/api/preview/uuid/$uuid?v=${previewVersion()}",
        downloadable = userPrivate == 0,
        webUrl = "$McsBaseUrl/home/$uuid",
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

private fun McsSchematic.previewVersion(): Int =
    listOf(uuid, updateTime, uploadTime)
        .joinToString(":")
        .hashCode()
        .absoluteValue

private fun Int.toCommunityBlueprintFormat(): BlueprintFormat =
    when (this) {
        0 -> BlueprintFormat.Nbt
        1 -> BlueprintFormat.Litematica
        2, 3 -> BlueprintFormat.Schematic
        else -> BlueprintFormat.Unknown
    }

private fun String.toCommunityDisplayTime(): String {
    if (isBlank()) return ""
    val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.getDefault())
    return runCatching {
        Instant.parse(this).atZone(ZoneId.systemDefault()).format(formatter)
    }.recoverCatching {
        LocalDateTime.parse(this, DateTimeFormatter.ISO_DATE_TIME).format(formatter)
    }.recoverCatching {
        substringBefore('.')
            .replace('T', ' ')
            .takeIf { it.length >= 16 }
            ?.substring(5, 16)
            ?: this
    }.getOrElse { error ->
        if (error is DateTimeParseException) this else this
    }
}
