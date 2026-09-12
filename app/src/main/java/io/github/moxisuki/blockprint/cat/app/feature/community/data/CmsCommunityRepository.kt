package io.github.moxisuki.blockprint.cat.app.feature.community.data

import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CmsCommunityRepository @Inject constructor(
    private val remoteDataSource: CmsCommunityRemoteDataSource,
    private val blueprintRepository: BlueprintRepository,
) {
    suspend fun loadPage(
        begin: Int,
        filter: String,
        heatSort: Boolean,
    ): CmsSearchPage {
        val page = begin / 15 + 1
        return remoteDataSource.search(
            query = filter,
            heatSort = heatSort,
            page = page,
        )
    }

    suspend fun loadDetail(id: String): CmsDetail {
        val detailId = id.toIntOrNull()
            ?: throw CmsCommunityException("无效 CMS 蓝图 ID: $id")
        return remoteDataSource.detail(detailId)
    }

    suspend fun downloadToLocalBlueprints(
        detailId: String,
        title: String,
        downloadId: Int?,
        onProgress: suspend (bytes: Long, total: Long) -> Unit,
    ) {
        val targetDownloadId = downloadId
            ?: detailId.toIntOrNull()
            ?: throw CmsCommunityException("缺少 CMS 下载 ID")
        val downloadInfo = remoteDataSource.resolveDownloadInfo(targetDownloadId)
        val fileUrl = downloadInfo.fileUrl
            ?: throw CmsCommunityException("未找到 CMS 下载文件链接")
        val bytes = remoteDataSource.downloadFile(
            fileUrl = fileUrl,
            onProgress = onProgress,
        )
        val fileName = downloadInfo.fileName
            ?.takeIf { it.isSupportedCommunityBlueprintName() }
            ?: "${title.ifBlank { detailId }.toCmsFileName()}.nbt"
        blueprintRepository.importDownloadedBlueprint(
            fileName = fileName,
            bytes = bytes,
        )
    }
}

private fun String.isSupportedCommunityBlueprintName(): Boolean {
    val extension = substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return extension in setOf("litematic", "schem", "schematic", "nbt", "json")
}

private fun String.toCmsFileName(): String =
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
        .ifBlank { "cms_blueprint" }
