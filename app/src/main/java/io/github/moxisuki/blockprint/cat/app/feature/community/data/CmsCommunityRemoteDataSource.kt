package io.github.moxisuki.blockprint.cat.app.feature.community.data

import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

private const val CmsPageSize = 15

@Singleton
internal class CmsCommunityRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val baseUrl = CmsBaseUrl.toHttpUrl()
    private val cookies = linkedMapOf<String, String>()
    private var csrfToken: String? = null

    suspend fun search(
        query: String,
        heatSort: Boolean,
        page: Int,
    ): CmsSearchPage = withContext(Dispatchers.IO) {
        val csrf = ensureCsrf()
        val body = FormBody.Builder()
            .add("csrfmiddlewaretoken", csrf)
            .add("search_type", "t")
            .add("q", query.take(100))
            .add("loader_type", "any")
            .add("mc_type", "any")
            .add("create_type", "any")
            .add("sort", if (heatSort) "heat" else "time")
            .add("order", "down")
            .add("grid_col", "1")
            .add("page", page.toString())
            .build()
        val html = executeText(
            requestBuilder("${CmsBaseUrl}/search/")
                .header("Referer", "${CmsBaseUrl}/tree/")
                .post(body)
                .build(),
        )
        CmsCommunityParser.parseSearchHtml(html, page)
    }

    suspend fun detail(detailId: Int): CmsDetail = withContext(Dispatchers.IO) {
        val csrf = ensureCsrf()
        val url = "${CmsBaseUrl}/detail/$detailId/?csrfmiddlewaretoken=${
            URLEncoder.encode(csrf, Charsets.UTF_8.name())
        }"
        CmsCommunityParser.parseDetailHtml(
            html = executeText(
                requestBuilder(url)
                    .header("Referer", "${CmsBaseUrl}/tree/")
                    .build(),
            ),
            detailId = detailId,
        )
    }

    suspend fun resolveDownloadInfo(downloadId: Int): CmsDownloadInfo = withContext(Dispatchers.IO) {
        val html = executeText(
            requestBuilder("${CmsBaseUrl}/download/$downloadId/")
                .header("Referer", "${CmsBaseUrl}/tree/")
                .build(),
        )
        CmsCommunityParser.parseDownloadHtml(html, downloadId)
    }

    suspend fun downloadFile(
        fileUrl: String,
        onProgress: suspend (bytes: Long, total: Long) -> Unit,
    ): ByteArray = withContext(Dispatchers.IO) {
        executeBytes(
            requestBuilder(fileUrl.toAbsoluteCmsUrl())
                .header("Referer", "${CmsBaseUrl}/tree/")
                .build(),
            onProgress = onProgress,
        )
    }

    private fun ensureCsrf(): String {
        csrfToken?.let { return it }
        val html = executeText(
            requestBuilder("${CmsBaseUrl}/tree/")
                .header("Referer", "${CmsBaseUrl}/")
                .build(),
        )
        val token = Regex("""csrfmiddlewaretoken"\s+value="([^"]+)"""")
            .find(html)
            ?.groupValues
            ?.get(1)
            ?.takeIf { it.isNotBlank() }
            ?: throw CmsCommunityException("未找到 CMS CSRF token")
        csrfToken = token
        return token
    }

    private fun executeText(request: Request): String {
        okHttpClient.newCall(request).execute().use { response ->
            captureSetCookies(response)
            val body = response.body?.string().orEmpty()
            if (response.code == 403 && body.isCmsChallenge()) {
                csrfToken = null
                throw CmsCloudflareException()
            }
            if (!response.isSuccessful) {
                csrfToken = null
                throw CmsCommunityException("CMS HTTP ${response.code}: ${response.message}")
            }
            return body
        }
    }

    private suspend fun executeBytes(
        request: Request,
        onProgress: suspend (bytes: Long, total: Long) -> Unit,
    ): ByteArray {
        okHttpClient.newCall(request).execute().use { response ->
            captureSetCookies(response)
            val body = response.body
            if (!response.isSuccessful || body == null) {
                throw CmsCommunityException("CMS HTTP ${response.code}: ${response.message}")
            }
            val total = body.contentLength().takeIf { it > 0L } ?: -1L
            body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    copied += read
                    onProgress(copied, total)
                }
                return output.toByteArray()
            }
        }
    }

    private fun requestBuilder(url: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("User-Agent", McsBrowserUserAgent)
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .apply {
                val cookieHeader = cookies.entries.joinToString("; ") { (key, value) -> "$key=$value" }
                if (cookieHeader.isNotBlank()) {
                    header("Cookie", cookieHeader)
                }
            }

    private fun captureSetCookies(response: Response) {
        response.headers("Set-Cookie").forEach { setCookie ->
            val parts = setCookie
                .substringBefore(';')
                .split("=", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) {
                cookies[parts[0].trim()] = parts[1].trim()
            }
        }
    }
}

internal fun cmsHasMore(latestPageSize: Int): Boolean =
    latestPageSize >= CmsPageSize

internal fun String.toAbsoluteCmsUrl(): String =
    when {
        startsWith("http://") || startsWith("https://") -> this
        startsWith("/") -> "$CmsBaseUrl$this"
        else -> "$CmsBaseUrl/$this"
    }

private fun String.isCmsChallenge(): Boolean =
    contains("Just a moment", ignoreCase = true) ||
        contains("cf-challenge", ignoreCase = true) ||
        contains("Cloudflare", ignoreCase = true)
