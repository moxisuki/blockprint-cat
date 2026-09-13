package io.github.moxisuki.blockprint.cat.app.feature.community.data

import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintRepository
import javax.inject.Inject
import javax.inject.Singleton

internal const val McsPageSize = 50

@Singleton
internal class McsCommunityRepository @Inject constructor(
    private val remoteDataSource: McsCommunityRemoteDataSource,
    private val blueprintRepository: BlueprintRepository,
) {
    suspend fun loadPage(
        begin: Int,
        filter: String,
        category: String?,
    ): McsCommunityPage =
        remoteDataSource.blueprints(
            page = begin / McsPageSize + 1,
            query = filter,
            category = category,
        )

    suspend fun loadCategories(): List<McsCommunityCategory> =
        remoteDataSource.categories()

    suspend fun loadDetail(id: String): McsBlueprintDetail =
        remoteDataSource.detail(id)

    suspend fun downloadToLocalBlueprints(
        id: String,
        title: String,
        format: BlueprintFormat,
        version: Int,
        onProgress: suspend (bytes: Long, total: Long) -> Unit,
    ) {
        val download = remoteDataSource.download(
            id = id,
            version = version,
            onProgress = onProgress,
        )
        val fileName = download.fileName
            ?.takeIf { it.isSupportedCommunityBlueprintName() }
            ?: "${title.ifBlank { id }.toCommunityFileName()}${format.communityExtension()}"
        blueprintRepository.importDownloadedBlueprint(
            fileName = fileName,
            bytes = download.bytes,
        )
    }
}

internal fun mcsHasMore(
    page: McsCommunityPage,
    loaded: Int,
): Boolean =
    when {
        page.totalPages > 0 -> page.page < page.totalPages
        page.totalCount > 0 -> loaded < page.totalCount
        else -> page.items.size >= McsPageSize
    }

private fun BlueprintFormat.communityExtension(): String =
    when (this) {
        BlueprintFormat.Litematica -> ".litematic"
        BlueprintFormat.Schematic -> ".schem"
        BlueprintFormat.Nbt -> ".nbt"
        BlueprintFormat.BuildingHelper -> ".json"
        BlueprintFormat.Unknown -> ".litematic"
    }

private fun String.isSupportedCommunityBlueprintName(): Boolean =
    substringAfterLast('.', missingDelimiterValue = "").lowercase() in
        setOf("litematic", "schem", "schematic", "nbt", "json")

private fun String.toCommunityFileName(): String =
    trim()
        .replace('\\', '_')
        .replace('/', '_')
        .replace(':', '_')
        .replace('*', '_')
        .replace('?', '_')
        .replace('"', '_')
        .replace('<', '_')
        .replace('>', '_')
        .replace('|', '_')
        .take(80)
        .ifBlank { "community_blueprint" }
