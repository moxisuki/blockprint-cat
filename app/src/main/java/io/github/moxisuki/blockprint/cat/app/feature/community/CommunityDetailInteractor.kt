package io.github.moxisuki.blockprint.cat.app.feature.community

import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.IconIndexResolver
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguageManager
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackAssetLocator
import io.github.moxisuki.blockprint.cat.app.feature.community.data.CmsCommunityRepository
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsCommunityRepository
import io.github.moxisuki.blockprint.cat.app.feature.community.data.toAbsoluteCmsUrl
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialItem
import javax.inject.Inject

internal class CommunityDetailInteractor @Inject constructor(
    private val mcsRepository: McsCommunityRepository,
    private val cmsRepository: CmsCommunityRepository,
    private val iconIndexResolver: IconIndexResolver,
    private val assetLocator: ResourcePackAssetLocator,
    private val languageManager: AppLanguageManager,
) {
    suspend fun load(seed: CommunityDetailSeed): CommunityDetailPayload =
        when (seed.source) {
            CommunitySourceUi.MCS.displayName -> loadMcs(seed.blueprintId)
            CommunitySourceUi.CMS.displayName -> loadCms(seed.blueprintId)
            else -> CommunityDetailPayload()
        }

    suspend fun download(
        seed: CommunityDetailSeed,
        onProgress: suspend (bytes: Long, total: Long) -> Unit,
    ) {
        when (seed.source) {
            CommunitySourceUi.MCS.displayName -> mcsRepository.downloadToLocalBlueprints(
                id = seed.blueprintId,
                title = seed.title,
                format = seed.format,
                version = seed.versionNumber,
                onProgress = onProgress,
            )
            CommunitySourceUi.CMS.displayName -> {
                val detail = cmsRepository.loadDetail(seed.blueprintId)
                cmsRepository.downloadToLocalBlueprints(
                    detailId = seed.blueprintId,
                    title = seed.title,
                    downloadId = detail.downloadId,
                    onProgress = onProgress,
                )
            }
            else -> error("Unsupported community source: ${seed.source}")
        }
    }

    private suspend fun loadMcs(uuid: String): CommunityDetailPayload {
        if (uuid.isBlank()) return CommunityDetailPayload()
        iconIndexResolver.ensureLoaded()
        val detail = mcsRepository.loadDetail(uuid)
        val summary = detail.summary
        val analysis = detail.analysis
        return CommunityDetailPayload(
            markdown = detail.contentMarkdown,
            materials = analysis.materials.map { material ->
                    BlueprintDetailMaterialItem(
                        name = material.blockId,
                        count = material.count,
                        iconUrls = iconIndexResolver.getIconUrls(material.blockId),
                        displayName = assetLocator.loadDisplayName(
                            blockId = material.blockId,
                            locales = languageManager.resourcePackLocaleCandidates(),
                        ),
                    )
            },
            coverUrl = detail.previewImages.firstOrNull() ?: summary.previewUrl,
            downloadable = detail.downloadUrl != null && analysis.transportAvailable,
            dimensions = analysis.dimensions?.toString(),
            categoryName = summary.category?.name,
            namespaces = analysis.namespaces.ifEmpty { summary.namespaces },
            gameVersion = analysis.gameVersion.takeIf { it.isNotBlank() },
            blockCount = analysis.sourceBlockCount.takeIf { it > 0 },
            visibleBlockCount = analysis.visibleBlockCount.takeIf { it > 0 },
            paletteSize = analysis.paletteSize.takeIf { it > 0 },
            tileEntityCount = analysis.tileEntityCount.takeIf { it > 0 },
            entityCount = analysis.entityCount.takeIf { it > 0 },
            materialKindCount = analysis.materialKindCount.takeIf { it > 0 },
            sourceFormat = summary.currentVersion.sourceFormat.takeIf { it.isNotBlank() },
            viewerSourceFormat = detail.viewerSourceFormat.takeIf { it.isNotBlank() },
            validationState = detail.validationState,
            fileSizeBytes = detail.originalSourceByteSize,
            viewerFileSizeBytes = detail.viewerSourceByteSize,
            viewCount = summary.engagement.viewCount,
            downloadCount = summary.engagement.downloadCount,
            likeCount = summary.engagement.likeCount,
            favouriteCount = summary.engagement.favouriteCount,
            attribution = summary.attribution.takeIf { it.isNotBlank() },
        )
    }

    private suspend fun loadCms(id: String): CommunityDetailPayload {
        if (id.isBlank()) return CommunityDetailPayload()
        iconIndexResolver.ensureLoaded()
        val detail = cmsRepository.loadDetail(id)
        return CommunityDetailPayload(
            markdown = detail.description,
            materials = detail.materials
                .sortedByDescending { it.count }
                .take(10)
                .map { material ->
                    val cmsIconUrl = material.iconUrl?.toAbsoluteCmsUrl()
                    BlueprintDetailMaterialItem(
                        name = material.blockId,
                        count = material.count,
                        iconUrls = listOfNotNull(cmsIconUrl) +
                            iconIndexResolver.getIconUrls(material.blockId),
                        displayName = assetLocator.loadDisplayName(
                            blockId = material.blockId,
                            locales = languageManager.resourcePackLocaleCandidates(),
                        ),
                    )
                },
            coverUrl = detail.coverUrl?.toAbsoluteCmsUrl(),
            downloadable = detail.downloadId != null,
            dimensions = detail.size?.let { "${it.first} x ${it.second} x ${it.third}" },
            stress = detail.stress,
        )
    }
}
