package io.github.moxisuki.blockprint.cat.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Mcschematic"

@Singleton
class McschematicClient @Inject constructor(
    private val cookieStore: McschematicCookieStore,
    private val okHttpClient: OkHttpClient,
    private val baseUrl: String = "https://www.mcschematic.top",
) {

    private suspend fun getString(path: String, query: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val url = buildUrl(path, query)
            val req = mkReq(url)
            val resp = okHttpClient.newCall(req).execute()
            if (resp.code !in 200..299) throw McschematicException("HTTP ${resp.code} for $url")
            resp.body?.string() ?: throw McschematicException("Empty body for $url")
        }

    private suspend fun getBytes(path: String, query: Map<String, String> = emptyMap()): ByteArray =
        withContext(Dispatchers.IO) {
            val url = buildUrl(path, query)
            val req = mkReq(url)
            val resp = okHttpClient.newCall(req).execute()
            if (resp.code !in 200..299) throw McschematicException("HTTP ${resp.code} for $url")
            resp.body?.bytes() ?: throw McschematicException("Empty body for $url")
        }

    private fun buildUrl(path: String, query: Map<String, String>): String {
        val qs = if (query.isEmpty()) "" else "?" + query.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
        return baseUrl + path + qs
    }

    private fun mkReq(url: String): Request {
        val b = Request.Builder().url(url)
            .header("User-Agent", io.github.moxisuki.blockprint.cat.data.community.BROWSER_UA)
        val cookieHeader = cookieStore.cookies().toHeaderValue()
        if (cookieHeader.isNotEmpty()) b.header("Cookie", cookieHeader)
        return b.build()
    }

    private fun ts(): String = System.currentTimeMillis().toString()

    companion object {
        fun suffixForSchematicType(type: Int): String = when (type) {
            1 -> ".litematic"
            2 -> ".schem"
            3 -> ".schematic"
            else -> ".nbt"
        }
    }

    suspend fun loginStatus(): LoginStatus? {
        val body = getString("/api/loginStatus", mapOf("t" to ts()))
        return try {
            McschematicParser.parseLoginStatus(JSONObject(body))
        } catch (e: Exception) {
            Log.w(TAG, "loginStatus parse failed: ${e.message}")
            null
        }
    }

    suspend fun schematicNum(filter: String = ""): Int {
        val body = getString(
            "/api/schematicNum",
            mapOf("filter" to filter, "type" to "0", "t" to ts()),
        )
        return body.trim().toIntOrNull() ?: 0
    }

    suspend fun listSchematics(
        begin: Int = 0,
        filter: String = "",
        heatSort: Boolean = false,
        type: Int = 0,
    ): List<Schematic> {
        val body = getString(
            "/api/schematics",
            mapOf(
                "begin" to begin.toString(),
                "filter" to filter,
                "heatSort" to heatSort.toString(),
                "type" to type.toString(),
                "t" to ts(),
            ),
        )
        val arr = JSONArray(body)
        return McschematicParser.parseSchematics(arr)
    }

    suspend fun comments(uuid: String, begin: Int = 0, heatSort: Boolean = false): List<SchematicComment> {
        val body = getString(
            "/api/schematicComments",
            mapOf(
                "uuid" to uuid,
                "begin" to begin.toString(),
                "heatSort" to heatSort.toString(),
                "t" to ts(),
            ),
        )
        return McschematicParser.parseComments(JSONArray(body))
    }

    suspend fun requirements(uuid: String): List<SchematicRequirement> {
        val body = getString("/api/requirements", mapOf("uuid" to uuid, "t" to ts()))
        return McschematicParser.parseRequirements(JSONArray(body))
    }

    suspend fun markdown(uuid: String): String {
        return getString("/api/markdown", mapOf("uuid" to uuid, "t" to ts()))
    }

    suspend fun downloadSchematic(uuid: String): ByteArray {
        return getBytes("/api/schematicFile", mapOf("uuid" to uuid))
    }

    suspend fun downloadPreview(uuid: String): ByteArray? = withContext(Dispatchers.IO) {
        val url = buildUrl("/api/preview/uuid/$uuid", mapOf("v" to System.currentTimeMillis().toString()))
        val req = mkReq(url)
        val resp = okHttpClient.newCall(req).execute()
        when {
            resp.code == 404 -> { Log.d(TAG, "downloadPreview: 404 for $uuid"); null }
            resp.code in 500..599 -> throw McschematicException("服务器错误 (HTTP ${resp.code})")
            resp.code in 200..299 -> resp.body?.bytes()
            else -> throw McschematicException("HTTP ${resp.code} for preview $uuid")
        }
    }

    suspend fun downloadSchematicToFile(
        context: Context,
        uuid: String,
        targetDir: File,
        schematicType: Int = 1,
        onProgress: (suspend (bytes: Long, total: Long) -> Unit)? = null,
    ): File = withContext(Dispatchers.IO) {
        if (!targetDir.exists()) targetDir.mkdirs()
        val tempFile = File(targetDir, "$uuid.download")
        val url = buildUrl("/api/schematicFile", mapOf("uuid" to uuid))
        val req = mkReq(url)
        val resp = okHttpClient.newCall(req).execute()
        val code = resp.code
        if (code == 429) throw McschematicException("服务器限流 (HTTP 429)，请稍后再试")
        if (code in 500..599) throw McschematicException("服务器错误 (HTTP $code)，请稍后再试")
        if (code !in 200..299) throw McschematicException("HTTP $code for download $uuid")
        val body = resp.body ?: throw McschematicException("Empty body for $url")
        val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L
        body.byteStream().use { input ->
            FileOutputStream(tempFile).use { output ->
                val buf = ByteArray(64 * 1024)
                var cumulative = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    output.write(buf, 0, n)
                    cumulative += n
                    onProgress?.invoke(cumulative, totalBytes)
                }
            }
        }
        val suffix = suffixForSchematicType(schematicType)
        val finalFile = File(targetDir, "$uuid$suffix")
        if (finalFile.exists()) finalFile.delete()
        if (!tempFile.renameTo(finalFile)) {
            tempFile.copyTo(finalFile, overwrite = true)
            tempFile.delete()
        }
        Log.d(TAG, "downloaded $uuid → ${finalFile.absolutePath} (${finalFile.length()}B)")
        finalFile
    }
}

class McschematicException(message: String) : RuntimeException(message)
