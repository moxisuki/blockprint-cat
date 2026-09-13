package io.github.moxisuki.blockprint.cat.app.feature.community.data

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class McsCommunityRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val apiBaseUrl = McsApiBaseUrl.toHttpUrl()

    suspend fun blueprints(
        page: Int,
        query: String,
        category: String?,
    ): McsCommunityPage =
        McsCommunityParser.parsePage(
            getJson(
                path = "/blueprints",
                query = buildMap {
                    put("page", page.toString())
                    put("limit", McsApiPageSize.toString())
                    put("sort", "latest")
                    put("extra", "none")
                    if (query.isNotBlank()) put("q", query)
                    if (!category.isNullOrBlank()) put("category", category)
                },
            ),
        )

    suspend fun categories(): List<McsCommunityCategory> =
        McsCommunityParser.parseCategories(getJson("/categories", emptyMap()))

    suspend fun detail(id: String): McsBlueprintDetail =
        McsCommunityParser.parseDetail(getJson("/blueprints/$id", emptyMap()))

    suspend fun download(
        id: String,
        version: Int,
        onProgress: suspend (bytes: Long, total: Long) -> Unit,
    ): McsDownload = withContext(Dispatchers.IO) {
        val response = execute(
            path = "/blueprints/$id/versions/$version/download",
            query = emptyMap(),
            accept = "application/octet-stream, */*",
        )
        response.use {
            val body = it.body ?: throw McsCommunityException("MCS 下载响应为空")
            val total = body.contentLength().takeIf { length -> length > 0L } ?: -1L
            val output = ByteArrayOutputStream()
            body.byteStream().use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var copied = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    copied += read
                    onProgress(copied, total)
                }
            }
            val raw = output.toByteArray()
            McsDownload(
                bytes = if (raw.startsWith(GZIP_MAGIC)) {
                    GZIPInputStream(raw.inputStream()).use { gzip -> gzip.readBytes() }
                } else {
                    raw
                },
                fileName = parseFileName(it.header("Content-Disposition")),
            )
        }
    }

    private suspend fun getJson(
        path: String,
        query: Map<String, String>,
    ): String = withContext(Dispatchers.IO) {
        execute(path, query, "application/json, text/plain, */*").use { response ->
            response.body?.string()
                ?: throw McsCommunityException("MCS 响应为空")
        }
    }

    private fun execute(
        path: String,
        query: Map<String, String>,
        accept: String,
    ): okhttp3.Response {
        val url = buildUrl(path, query)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", McsBrowserUserAgent)
            .header("Accept", accept)
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .get()
            .build()
        val response = runCatching { okHttpClient.newCall(request).execute() }
            .getOrElse { error ->
                throw McsCommunityException("MCS 网络请求失败: ${error.message ?: error::class.java.simpleName}")
            }
        if (!response.isSuccessful) {
            val detail = response.body?.string()?.let(::responseError).orEmpty()
            response.close()
            throw McsCommunityException(
                "MCS HTTP ${response.code}: ${detail.ifBlank { response.message }}",
            )
        }
        Log.d("McsCommunityApi", "${request.method} ${url.redactQuery()} -> ${response.code}")
        return response
    }

    private fun buildUrl(
        path: String,
        query: Map<String, String>,
    ): HttpUrl {
        val builder = apiBaseUrl.newBuilder()
        path.trim('/')
            .split('/')
            .filter { it.isNotBlank() }
            .forEach { builder.addPathSegment(it) }
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build()
    }
}

@Immutable
internal data class McsDownload(
    val bytes: ByteArray,
    val fileName: String?,
)

private const val McsApiPageSize = 50
private val GZIP_MAGIC = byteArrayOf(0x1f, 0x8b.toByte())

private fun responseError(body: String): String =
    runCatching {
        val json = JSONObject(body)
        json.optString("detail")
            .ifBlank { json.optString("message") }
            .ifBlank { body.take(160) }
    }.getOrDefault(body.take(160))

private fun parseFileName(contentDisposition: String?): String? {
    if (contentDisposition.isNullOrBlank()) return null
    val encoded = Regex("""filename\*\s*=\s*UTF-8''([^;]+)""", RegexOption.IGNORE_CASE)
        .find(contentDisposition)
        ?.groupValues
        ?.getOrNull(1)
    if (encoded != null) return URLDecoder.decode(encoded, Charsets.UTF_8.name())
    return Regex("""filename\s*=\s*"?([^";]+)""", RegexOption.IGNORE_CASE)
        .find(contentDisposition)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
    size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

private fun HttpUrl.redactQuery(): String =
    if (querySize == 0) toString() else newBuilder().apply {
        queryParameterNames.forEach { name -> setQueryParameter(name, "***") }
    }.build().toString()
