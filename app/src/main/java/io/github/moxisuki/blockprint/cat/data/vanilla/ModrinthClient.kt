package io.github.moxisuki.blockprint.cat.data.vanilla

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ModrinthClient"
private const val API = "https://api.modrinth.com/v2"

@Singleton
class ModrinthClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    data class ModVersion(
        val id: String, val name: String, val gameVersions: List<String>,
        val loaders: List<String>, val fileUrl: String, val fileName: String, val fileSize: Long,
    )

    data class SearchResult(
        val slug: String, val title: String, val description: String,
        val projectId: String, val projectType: String,
    )

    fun searchProject(slug: String): String? {
        val t0 = System.currentTimeMillis()
        val url = "$API/project/$slug"
        Log.i(TAG, "→ GET $url")
        val req = Request.Builder().url(url)
            .header("User-Agent", io.github.moxisuki.blockprint.cat.data.community.BROWSER_UA)
            .build()
        val resp = okHttpClient.newCall(req).execute()
        val ms = System.currentTimeMillis() - t0
        if (!resp.isSuccessful) {
            Log.w(TAG, "← HTTP ${resp.code} ($slug) — ${ms}ms")
            return null
        }
        val id = JSONObject(resp.body!!.string()).optString("id", null)
        Log.i(TAG, "← $slug → $id — ${ms}ms")
        return id
    }

    fun searchMods(query: String): List<SearchResult> {
        val t0 = System.currentTimeMillis()
        val url = "$API/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}&limit=10&index=downloads"
        Log.i(TAG, "→ SEARCH \"$query\"")
        val req = Request.Builder().url(url)
            .header("User-Agent", io.github.moxisuki.blockprint.cat.data.community.BROWSER_UA)
            .build()
        val resp = okHttpClient.newCall(req).execute()
        val ms = System.currentTimeMillis() - t0
        if (!resp.isSuccessful) {
            Log.w(TAG, "← SEARCH HTTP ${resp.code} — ${ms}ms")
            return emptyList()
        }
        val obj = JSONObject(resp.body!!.string())
        val hits = obj.getJSONArray("hits")
        val result = mutableListOf<SearchResult>()
        for (i in 0 until hits.length()) {
            val h = hits.getJSONObject(i)
            result.add(SearchResult(
                slug = h.optString("slug", ""),
                title = h.optString("title", ""),
                description = h.optString("description", "").take(80),
                projectId = h.optString("project_id", ""),
                projectType = h.optString("project_type", ""),
            ))
        }
        Log.i(TAG, "← SEARCH ${result.size} hits — ${ms}ms")
        return result
    }

    fun getVersions(projectId: String, gameVersion: String): List<ModVersion> {
        val url = "$API/project/$projectId/version?game_versions=[\"$gameVersion\"]"
        return fetchVersions(url, "filtered:$gameVersion")
    }

    fun getAllVersions(projectId: String): List<ModVersion> {
        val url = "$API/project/$projectId/version"
        return fetchVersions(url, "all")
    }

    private fun fetchVersions(url: String, label: String): List<ModVersion> {
        val t0 = System.currentTimeMillis()
        Log.i(TAG, "→ GET versions ($label)")
        val req = Request.Builder().url(url)
            .header("User-Agent", io.github.moxisuki.blockprint.cat.data.community.BROWSER_UA)
            .build()
        val resp = okHttpClient.newCall(req).execute()
        val ms = System.currentTimeMillis() - t0
        if (!resp.isSuccessful) {
            Log.w(TAG, "← HTTP ${resp.code} ($label) — ${ms}ms")
            return emptyList()
        }
        val arr = JSONArray(resp.body!!.string())
        val result = mutableListOf<ModVersion>()
        for (i in 0 until arr.length()) {
            val v = arr.getJSONObject(i)
            val files = v.getJSONArray("files")
            var primaryFile: JSONObject? = null
            for (j in 0 until files.length()) {
                val f = files.getJSONObject(j)
                if (f.optBoolean("primary", false)) { primaryFile = f; break }
            }
            if (primaryFile == null && files.length() > 0) primaryFile = files.getJSONObject(0)
            if (primaryFile == null) continue

            val gv = mutableListOf<String>()
            val gva = v.optJSONArray("game_versions") ?: JSONArray()
            for (j in 0 until gva.length()) gv.add(gva.getString(j))
            val ld = mutableListOf<String>()
            val lda = v.optJSONArray("loaders") ?: JSONArray()
            for (j in 0 until lda.length()) ld.add(lda.getString(j))

            result.add(ModVersion(
                id = v.getString("id"),
                name = v.getString("name"),
                gameVersions = gv,
                loaders = ld,
                fileUrl = primaryFile.getString("url"),
                fileName = primaryFile.getString("filename"),
                fileSize = primaryFile.optLong("size", -1),
            ))
        }
        Log.i(TAG, "← ${result.size} versions ($label) — ${ms}ms")
        return result
    }

    data class ExtractResult(val fileCount: Int, val namespaces: Set<String>)

    suspend fun downloadAndExtractAssets(
        fileUrl: String, fileName: String,
        destDir: File, assetNamespace: String,
        onProgress: (Float) -> Unit,
    ): ExtractResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "Downloading $fileName from $fileUrl")
        val req = Request.Builder().url(fileUrl)
            .header("User-Agent", io.github.moxisuki.blockprint.cat.data.community.BROWSER_UA)
            .build()
        val resp = okHttpClient.newCall(req).execute()
        val body = resp.body ?: throw IllegalStateException("Empty body")
        val total = body.contentLength()
        var read = 0L
        val buffer = ByteArray(8192)

        val tmpJar = File(destDir.parentFile, "$fileName.tmp")
        body.byteStream().use { input ->
            tmpJar.outputStream().use { output ->
                var n: Int
                while (input.read(buffer).also { n = it } != -1) {
                    output.write(buffer, 0, n)
                    read += n
                    if (total > 0) onProgress(read.toFloat() / total)
                }
            }
        }

        Log.i(TAG, "Extracting assets from $fileName")

        // 1. Check for jar-in-jar (bundled mods with metadata.json)
        val metadataBytes = readZipEntry(tmpJar, "META-INF/jarjar/metadata.json")
        val embeddedJars = if (metadataBytes != null) {
            Log.i(TAG, "Found META-INF/jarjar/metadata.json, checking embedded JARs")
            try {
                val meta = JSONObject(String(metadataBytes))
                val jars = meta.getJSONArray("jars")
                (0 until jars.length()).map { i ->
                    jars.getJSONObject(i).getString("path")
                }
            } catch (e: Exception) { emptyList() }
        } else emptyList()

        // 2. Extract assets — try embedded JARs first, then main JAR
        var extracted = 0
        val extractedByNamespace = mutableMapOf<String, Int>()
        val namespacesFound = mutableSetOf<String>()

        fun extractFromJar(jar: File, label: String) {
            ZipInputStream(jar.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val path = entry.name
                    if (!entry.isDirectory && path.startsWith("assets/")) {
                        val ns = path.removePrefix("assets/").substringBefore('/')
                        if (ns == "minecraft") { zis.closeEntry(); entry = zis.nextEntry; continue }
                        namespacesFound.add(ns)
                        val destFile = File(destDir, path.removePrefix("assets/"))
                        destFile.parentFile?.mkdirs()
                        destFile.outputStream().use { zis.copyTo(it) }
                        extracted++
                        extractedByNamespace[ns] = (extractedByNamespace[ns] ?: 0) + 1
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        if (embeddedJars.isNotEmpty()) {
            Log.i(TAG, "Extracting ${embeddedJars.size} embedded JARs: ${embeddedJars.joinToString(", ")}")
            for (embeddedPath in embeddedJars) {
                val embeddedData = readZipEntry(tmpJar, embeddedPath)
                if (embeddedData != null) {
                    val embeddedFile = File(tmpJar.parentFile, embeddedPath.substringAfterLast('/'))
                    embeddedFile.writeBytes(embeddedData)
                    extractFromJar(embeddedFile, embeddedPath)
                    embeddedFile.delete()
                }
            }
        }

        // Also extract from main JAR (in case mod has both bundled + own assets)
        extractFromJar(tmpJar, "main")

        Log.i(TAG, "Extracted $extracted files from $fileName, namespaces: $namespacesFound")
        if (extractedByNamespace.isNotEmpty()) {
            for ((ns, count) in extractedByNamespace) {
                Log.i(TAG, "  $ns: $count files")
            }
        }
        tmpJar.delete()
        ExtractResult(extracted, namespacesFound)
    }

    private fun readZipEntry(jar: File, path: String): ByteArray? {
        return try {
            ZipInputStream(jar.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == path && !entry.isDirectory) {
                        return zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
