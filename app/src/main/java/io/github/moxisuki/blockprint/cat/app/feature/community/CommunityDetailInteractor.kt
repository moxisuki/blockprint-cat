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
                uuid = seed.blueprintId,
                title = seed.title,
                format = seed.format,
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
        val requirements = mcsRepository.loadRequirements(uuid)
        val markdown = runCatching {
            mcsRepository.loadMarkdown(uuid)
        }.getOrDefault("")
        return CommunityDetailPayload(
            markdown = markdown,
            materials = requirements
                .sortedByDescending { it.count }
                .take(10)
                .map { requirement ->
                    BlueprintDetailMaterialItem(
                        name = requirement.blockId,
                        count = requirement.count,
                        iconUrls = iconIndexResolver.getIconUrls(requirement.blockId),
                        displayName = assetLocator.loadDisplayName(
                            blockId = requirement.blockId,
                            locales = languageManager.resourcePackLocaleCandidates(),
                        ),
                    )
                },
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
