package io.github.moxisuki.blockprint.cat.data.community

import android.content.Context
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CmsRepository @Inject constructor(
    private val client: CmsClient,
) : CommunityRepository {

    override val source = CommunitySource.CMS

    override suspend fun isAvailable(): Boolean = try {
        android.util.Log.i("CmsRepository", "checking CMS availability...")
        client.ensureCsrf()
        val ok = client.testSearch()
        android.util.Log.i("CmsRepository", "CMS search test: $ok")
        ok
    } catch (e: CmsCloudflareException) {
        android.util.Log.w("CmsRepository", "CMS blocked by Cloudflare, needs browser")
        false
    } catch (e: Exception) {
        android.util.Log.e("CmsRepository", "CMS not available: ${e.message}", e)
        false
    }

    override suspend fun listCount(filter: String): Int = try {
        // CMS has ~15 items per page, 879 total → ~59 pages
        client.search(filter, "time", 1).let { if (it.items.isNotEmpty()) 999 else 0 }
    } catch (e: Exception) { -1 }

    override suspend fun list(begin: Int, filter: String, heatSort: Boolean): List<UnifiedSchematic> {
        val page = begin / CmsClient.PAGE_SIZE + 1
        val sort = if (heatSort) "heat" else "time"
        val res = client.search(query = filter, sort = sort, page = page)
        return res.items.map { item ->
            UnifiedSchematic(
                source = CommunitySource.CMS,
                id = item.detailId.toString(),
                name = item.title,
                author = item.author,
                sizeText = item.size,
                description = item.description,
                updateTime = item.displayDate,
                downloads = item.downloads,
                stress = item.stress,
            )
        }
    }

    override suspend fun loadDetail(id: String): UnifiedDetail {
        val detailId = id.toIntOrNull()
            ?: return UnifiedDetail(source = CommunitySource.CMS, id = id, error = io.github.moxisuki.blockprint.cat.data.error.AppError.Unknown("无效 ID"), loading = false)
        android.util.Log.i("CmsRepository", "loadDetail id=$id detailId=$detailId")
        val detail = client.fetchDetail(detailId)
        android.util.Log.i("CmsRepository", "loaded: downloadId=${detail.downloadId}, materials=${detail.materials.size}, production=${detail.production.size}")
        return UnifiedDetail(
            source = CommunitySource.CMS,
            id = id,
            coverUrl = detail.coverUrl,
            requirements = detail.materials,
            production = detail.production,
            dependencies = detail.dependencies,
            stress = detail.stress,
            cmsDownloadId = detail.downloadId,
            downloadable = detail.downloadId != null,
            loading = false,
        )
    }

    override suspend fun loadPreview(id: String, detail: UnifiedDetail?): ByteArray? {
        val coverUrl = detail?.coverUrl ?: return null
        return client.fetchBytes(coverUrl)
    }

    override suspend fun downloadToFile(
        context: Context,
        schematic: UnifiedSchematic,
        detail: UnifiedDetail?,
        targetDir: File,
        onProgress: suspend (Long, Long) -> Unit,
    ): File {
        val downloadId = detail?.cmsDownloadId
            ?: schematic.id.toIntOrNull()
            ?: throw IllegalStateException("缺少下载 ID")
        // Step 1: resolve the actual file URL from the download page
        val fileUrl = client.resolveDownloadUrl(downloadId)
        // Step 2: download the file
        val safeName = schematic.name.replace(Regex("""[/\\:*?"<>|]"""), "_").take(80) + ".nbt"
        return client.downloadToFile(
            fileUrl = fileUrl,
            targetDir = targetDir,
            fileName = safeName,
            onProgress = onProgress,
        )
    }

    companion object {
        private const val BASE_URL = "https://www.creativemechanicserver.com"
    }
}
