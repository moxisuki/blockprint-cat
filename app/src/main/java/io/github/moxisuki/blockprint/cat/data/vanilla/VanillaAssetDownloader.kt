package io.github.moxisuki.blockprint.cat.data.vanilla

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VanillaAssetDownloader"
private const val BMC_API = "https://bmclapi2.bangbang93.com"

// Mirror Mojang URLs through BMCLAPI for faster access in China
private fun mirrorUrl(original: String): String = original
    .replace("https://launchermeta.mojang.com/", "$BMC_API/")
    .replace("https://launcher.mojang.com/", "$BMC_API/")
    .replace("https://piston-meta.mojang.com/", "$BMC_API/")
    .replace("https://piston-data.mojang.com/", "$BMC_API/")
    .replace("https://libraries.minecraft.net/", "$BMC_API/maven/")
    .replace("https://resources.download.minecraft.net/", "$BMC_API/assets/")
    .replace("http://resources.download.minecraft.net/", "$BMC_API/assets/")

@Singleton
class VanillaAssetDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val statusDao: VanillaAssetStatusDao,
) {
    sealed class DownloadState {
        data object Idle : DownloadState()
        data object FetchingManifest : DownloadState()
        data class DownloadingJar(val progress: Float, val fileName: String) : DownloadState()
        data class Extracting(val current: String, val extracted: Int) : DownloadState()
        data class Installed(val version: String, val fileCount: Int, val totalSize: Long) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val assetsDir: File
        get() = File(context.filesDir, "blockprintcat/render_assets").also { it.mkdirs() }

    // Extract models + blockstates + textures + lang from client.jar
    private val jarPrefixes = listOf(
        "assets/minecraft/models/",
        "assets/minecraft/blockstates/",
        "assets/minecraft/textures/",
        "assets/minecraft/lang/",
    )

    private fun wantedFromJar(path: String): Boolean = jarPrefixes.any { path.startsWith(it) }

    init {
        // 同步读取 Room，保证 isAssetsAvailable() 首次调用就返回正确值
        kotlinx.coroutines.runBlocking {
            val existing = statusDao.get()
            _downloadState.value = if (existing != null)
                DownloadState.Installed(existing.version, existing.fileCount, existing.totalSize)
            else DownloadState.Idle
        }
    }

    fun startDownload() {
        if (job?.isActive == true) return
        job = scope.launch {
            try {
                _downloadState.value = DownloadState.FetchingManifest

                // 1. Get version manifest → latest release
                val manifest = fetchJson("$BMC_API/mc/game/version_manifest.json")
                val latestVersion = manifest.getJSONObject("latest").getString("release")
                val versions = manifest.getJSONArray("versions")
                var versionUrl = ""
                for (i in 0 until versions.length()) {
                    val v = versions.getJSONObject(i)
                    if (v.getString("id") == latestVersion) { versionUrl = mirrorUrl(v.getString("url")); break }
                }
                Log.i(TAG, "Latest: $latestVersion, url=$versionUrl")

                // 2. Get version JSON → client.jar URL (via BMCLAPI mirror)
                val versionJson = fetchJson(versionUrl)
                val rawJarUrl = versionJson.getJSONObject("downloads").getJSONObject("client").getString("url")
                val clientJarUrl = mirrorUrl(rawJarUrl)
                val jarSize = versionJson.getJSONObject("downloads").getJSONObject("client").optLong("size", -1)
                Log.i(TAG, "client.jar: $clientJarUrl (${jarSize} bytes)")

                // 3. Download JAR to temp file
                val tmpJar = File(context.cacheDir, "client.jar.tmp")
                val request = Request.Builder().url(clientJarUrl).build()
                val response = okHttpClient.newCall(request).execute()
                val body = response.body ?: throw IllegalStateException("Empty JAR response")
                val totalBytes = body.contentLength()
                var readBytes = 0L
                val buffer = ByteArray(8192)
                body.byteStream().use { input ->
                    tmpJar.outputStream().use { output ->
                        var n: Int
                        while (input.read(buffer).also { n = it } != -1) {
                            output.write(buffer, 0, n)
                            readBytes += n
                            _downloadState.value = DownloadState.DownloadingJar(
                                progress = if (totalBytes > 0) readBytes.toFloat() / totalBytes else 0f,
                                fileName = "client.jar (${"%.1f".format(readBytes / 1048576.0)} / ${"%.1f".format(totalBytes / 1048576.0)} MB)",
                            )
                        }
                    }
                }
                Log.i(TAG, "JAR downloaded: ${tmpJar.length()} bytes")

                // 4. Extract wanted assets from JAR
                _downloadState.value = DownloadState.Extracting("", 0)
                var extracted = 0
                ZipInputStream(tmpJar.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val path = entry.name
                        if (!entry.isDirectory && wantedFromJar(path)) {
                            val destFile = File(assetsDir, path.removePrefix("assets/"))
                            destFile.parentFile?.mkdirs()
                            destFile.outputStream().use { zis.copyTo(it) }
                            extracted++
                            _downloadState.value = DownloadState.Extracting(path, extracted)
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
                tmpJar.delete()

                // 5. Download missing lang files from asset index (jar may only contain en_us)
                val locale = java.util.Locale.getDefault()
                val langCountry = locale.country.takeIf { it.isNotBlank() }?.lowercase()
                val langCandidates = listOfNotNull(
                    if (langCountry != null) "minecraft/lang/${locale.language}_$langCountry.json" else null,
                    "minecraft/lang/${locale.language}.json",
                    "minecraft/lang/en_us.json",
                ).distinct()
                Log.i(TAG, "Lang candidates: $langCandidates (locale=$locale)")
                val assetIndexUrl = versionJson.getJSONObject("assetIndex").getString("url")
                val indexJson = fetchJson(mirrorUrl(assetIndexUrl))
                val objects = indexJson.getJSONObject("objects")
                for (langKey in langCandidates) {
                    val destFile = File(assetsDir, langKey)
                    if (destFile.isFile) continue // already extracted from JAR
                    val obj = objects.optJSONObject(langKey) ?: continue
                    val hash = obj.getString("hash")
                    val url = "$BMC_API/assets/${hash.substring(0, 2)}/$hash"
                    try {
                        val req = Request.Builder().url(url).build()
                        val resp = okHttpClient.newCall(req).execute()
                        resp.body?.byteStream()?.use { s ->
                            destFile.parentFile?.mkdirs()
                            destFile.outputStream().use { s.copyTo(it) }
                        }
                        Log.i(TAG, "Lang downloaded: $langKey")
                    } catch (e: Exception) {
                        Log.w(TAG, "Lang skip: $langKey — ${e.message}")
                    }
                }

                // 6. Save status
                val totalSize = assetsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                val entity = VanillaAssetStatusEntity(
                    version = latestVersion,
                    fileCount = extracted,
                    totalSize = totalSize,
                    installedAt = System.currentTimeMillis(),
                )
                statusDao.upsert(entity)
                _downloadState.value = DownloadState.Installed(latestVersion, extracted, totalSize)
                Log.i(TAG, "Extracted: $extracted files, $totalSize bytes")
            } catch (e: CancellationException) {
                _downloadState.value = DownloadState.Idle
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                _downloadState.value = DownloadState.Error(e.message ?: "下载失败")
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    fun deleteAssets() {
        scope.launch {
            assetsDir.deleteRecursively()
            statusDao.clear()
            _downloadState.value = DownloadState.Idle
            Log.i(TAG, "Assets deleted")
        }
    }

    fun isAssetsAvailable(): Boolean = _downloadState.value is DownloadState.Installed

    fun installedVersion(): String? = (_downloadState.value as? DownloadState.Installed)?.version

    private suspend fun fetchJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw IllegalStateException("Empty response: $url")
        if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}: ${body.take(200)}")
        try {
            JSONObject(body)
        } catch (e: Exception) {
            throw IllegalStateException("Invalid JSON from $url: ${body.take(200)}")
        }
    }
}
