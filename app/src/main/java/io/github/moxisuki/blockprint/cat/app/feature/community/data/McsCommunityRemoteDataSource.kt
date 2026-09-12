package io.github.moxisuki.blockprint.cat.app.feature.community.data

import android.util.Log
import io.github.moxisuki.blockprint.cat.app.core.persistence.AppSettingsRepository
import io.github.moxisuki.blockprint.cat.app.core.persistence.McsAuthCookies
import io.github.moxisuki.blockprint.cat.app.feature.community.McsLoginLogTag
import io.github.moxisuki.blockprint.cat.app.feature.community.toDebugSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class McsCommunityRemoteDataSource @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val settingsRepository: AppSettingsRepository,
) {
    private val baseUrl = McsBaseUrl.toHttpUrl()

    suspend fun loginStatus(cookies: McsAuthCookies? = null): McsLoginStatus {
        Log.d(
            McsLoginLogTag,
            "Remote loginStatus start: cookieSource=${if (cookies == null) "datastore" else "candidate"}, cookies=${cookies?.toDebugSummary() ?: "datastore"}",
        )
        return McsCommunityParser.parseLoginStatus(
            getString(
                path = "/api/loginStatus",
                query = mapOf("t" to timestamp()),
                cookies = cookies,
            ),
        ).also { status ->
            Log.d(
                McsLoginLogTag,
                "Remote loginStatus parsed: uuid=${status.uuid.ifBlank { "-" }}, authority=${status.authority}, permissions=${status.permissions}, message=${status.message.ifBlank { "-" }}",
            )
        }
    }

    suspend fun schematicCount(filter: String): Int =
        getString(
            path = "/api/schematicNum",
            query = mapOf(
                "filter" to filter,
                "type" to "0",
                "t" to timestamp(),
            ),
        ).trim().toIntOrNull() ?: 0

    suspend fun schematics(
        begin: Int,
        filter: String,
        heatSort: Boolean,
        type: Int = 0,
    ): List<McsSchematic> =
        McsCommunityParser.parseSchematics(
            getString(
                path = "/api/schematics",
                query = mapOf(
                    "begin" to begin.toString(),
                    "filter" to filter,
                    "heatSort" to heatSort.toString(),
                    "type" to type.toString(),
                    "t" to timestamp(),
                ),
            ),
        )

    suspend fun tags(begin: Int = 0): List<McsCommunityTag> =
        McsCommunityParser.parseTags(
            getString(
                path = "/api/tagList",
                query = mapOf(
                    "begin" to begin.toString(),
                    "t" to timestamp(),
                ),
            ),
        )

    suspend fun requirements(uuid: String): List<McsSchematicRequirement> =
        McsCommunityParser.parseRequirements(
            getString(
                path = "/api/requirements",
                query = mapOf(
                    "uuid" to uuid,
                    "t" to timestamp(),
                ),
            ),
        )

    suspend fun markdown(uuid: String): String =
        getString(
            path = "/api/markdown",
            query = mapOf(
                "uuid" to uuid,
                "t" to timestamp(),
            ),
        ).toMarkdownText()

    suspend fun downloadSchematic(
        uuid: String,
        onProgress: suspend (bytes: Long, total: Long) -> Unit,
    ): ByteArray = withContext(Dispatchers.IO) {
        val url = buildUrl(
            path = "/api/schematicFile",
            query = mapOf("uuid" to uuid),
        )
        val cookieHeader = settingsRepository.mcsAuthCookies.first().toHeaderValue()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", McsBrowserUserAgent)
            .header("Accept", "application/octet-stream, */*")
            .apply {
                if (cookieHeader.isNotBlank()) {
                    header("Cookie", cookieHeader)
                }
            }
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) {
                throw McsCommunityException("MCS HTTP ${response.code}: ${response.message}")
            }
            val total = body.contentLength().takeIf { it > 0 } ?: -1L
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
                output.toByteArray()
            }
        }
    }

    private suspend fun getString(
        path: String,
        query: Map<String, String>,
        cookies: McsAuthCookies? = null,
    ): String = withContext(Dispatchers.IO) {
        val url = buildUrl(path, query)
        val cookieHeader = (cookies ?: settingsRepository.mcsAuthCookies.first())
            .toHeaderValue()
        Log.d(
            McsLoginLogTag,
            "Remote GET start: path=$path, url=${url.redactQueryForLog()}, hasCookie=${cookieHeader.isNotBlank()}, cookieSource=${if (cookies == null) "datastore" else "candidate"}",
        )
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", McsBrowserUserAgent)
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            .apply {
                if (cookieHeader.isNotBlank()) {
                    header("Cookie", cookieHeader)
                }
            }
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
            Log.d(
                McsLoginLogTag,
                "Remote GET response: path=$path, code=${response.code}, successful=${response.isSuccessful}, bodyLength=${body?.length ?: -1}",
            )
            if (!response.isSuccessful || body == null) {
                Log.w(
                    McsLoginLogTag,
                    "Remote GET failed: path=$path, code=${response.code}, message=${response.message}",
                )
                throw McsCommunityException("MCS HTTP ${response.code}: ${response.message}")
            }
            body
        }
    }

    private fun buildUrl(
        path: String,
        query: Map<String, String>,
    ): HttpUrl {
        val builder = baseUrl.newBuilder()
        path.trim('/')
            .split('/')
            .filter { it.isNotBlank() }
            .forEach { builder.addPathSegment(it) }
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build()
    }

    private fun timestamp(): String = System.currentTimeMillis().toString()
}

private fun HttpUrl.redactQueryForLog(): String {
    if (querySize == 0) return toString()
    val builder = newBuilder()
    queryParameterNames.forEach { name ->
        builder.setQueryParameter(name, "***")
    }
    return builder.build().toString()
}

private fun String.toMarkdownText(): String {
    val body = trim()
    return runCatching {
        JSONTokener(body).nextValue() as? String
    }.getOrNull()?.trim() ?: body.trim('"')
}
