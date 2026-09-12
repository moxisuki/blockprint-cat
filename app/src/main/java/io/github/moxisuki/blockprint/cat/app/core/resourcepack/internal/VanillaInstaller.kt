package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.PackProgress
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject

private const val TAG = "VanillaInstaller"

@Singleton
class VanillaInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: AppHttpClient,
    private val manifest: MojangVersionManifestSource,
    private val extractor: AssetExtractor,
) {
    fun install(
        assetsDir: File = File(context.filesDir, "blockprintcat/render_assets"),
    ): Flow<PackProgress> = channelFlow {
        val tmp = File.createTempFile("vanilla_client_", ".jar", context.cacheDir)
        try {
            Log.i(TAG, "vanilla install starting")
            send(PackProgress.Preparing("Mojang manifest"))

            // Step 1: latest release + its official Mojang URL
            val versionInfo = when (val result = manifest.latestVersionInfo()) {
                is AppNetworkResult.Success -> result.value
                is AppNetworkResult.Failure -> throw IllegalStateException(result.describe("manifest unavailable"))
            }
            Log.i(TAG, "step 1 ok: release=${versionInfo.release}")

            // Step 2: fetch version metadata JSON. versionJson() applies AssetMirrors.mojang()
            send(PackProgress.Preparing("version metadata"))
            val versionJson = when (val result = manifest.versionJson(versionInfo.officialVersionUrl)) {
                is AppNetworkResult.Success -> result.value
                is AppNetworkResult.Failure -> throw IllegalStateException(result.describe("version json unavailable"))
            }
            Log.i(TAG, "step 2 ok: ${versionJson.optString("id")}")

            // Step 3: download client.jar (URL comes from the version JSON; mirror it)
            val client = versionJson.getJSONObject("downloads").getJSONObject("client")
            val jarOfficialUrl = client.getString("url")
            val jarUrl = AssetMirrors.mojang(jarOfficialUrl)
            val jarUrls = listOf(jarUrl, jarOfficialUrl).distinct()
            Log.i(TAG, "step 3 downloading ${jarUrls.first()} (size=${client.optLong("size", -1)})")
            val jarSize = client.optLong("size", -1L)
            send(PackProgress.Preparing("client.jar"))
            send(PackProgress.Downloading("client.jar", 0f, 0L, jarSize))
            val progressThrottle = PackProgressThrottle()
            val jar = downloadClientJar(jarUrls, tmp) { fraction ->
                if (progressThrottle.allow(fraction)) {
                    trySend(PackProgress.Downloading("client.jar", fraction, estimatedBytes(jarSize, fraction), jarSize))
                }
            } ?: throw IllegalStateException("client.jar unavailable")
            Log.i(TAG, "step 3 ok: ${jar.length()} bytes")
            send(PackProgress.Downloading("client.jar", 1f, jar.length(), jarSize))

            // Step 4: extract assets
            send(PackProgress.Installing("client.jar", 0f, 0, null))
            assetsDir.mkdirs()
            val result = extractor.extractFromJar(
                jar = jar,
                assetsDir = assetsDir,
                namespaceFilter = { it == "minecraft" },
                onProgress = { currentPath, extracted, total ->
                    val fraction = if (total > 0) extracted.toFloat() / total else null
                    if (progressThrottle.allow(fraction)) trySend(
                        PackProgress.Installing(
                            label = currentPath,
                            fraction = fraction,
                            installedFiles = extracted,
                            totalFiles = total,
                        ),
                    )
                },
            )
            Log.i(TAG, "step 4 extracted ${result.fileCount} files from jar")

            // Step 5: lang fallback via asset index (best-effort)
            val indexUrl = AssetMirrors.mojang(versionJson.getJSONObject("assetIndex").getString("url"))
            val index = (http.getJson(indexUrl) as? AppNetworkResult.Success)?.value
            runCatching {
                if (index != null) {
                    extractor.downloadLangFromIndex(index, "zh_cn", assetsDir, http)
                    extractor.downloadLangFromIndex(index, "en_us", assetsDir, http)
                }
            }.onFailure { Log.w(TAG, "lang fetch skipped", it) }

            val entry = ResourcePackEntry(
                id = ResourcePackId.Vanilla,
                kind = ResourcePackEntry.Kind.VANILLA,
                displayName = "Minecraft",
                version = versionInfo.release,
                mcVersion = null,
                fileCount = result.fileCount,
                totalSize = result.totalSize,
                installedAt = System.currentTimeMillis(),
                namespaces = result.namespaces,
                hasAssets = result.fileCount > 0,
            )
            Log.i(TAG, "vanilla install done: ${entry.fileCount} files, ${entry.totalSize}B")
            send(PackProgress.Done(entry))
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            Log.e(TAG, "vanilla install failed", t)
            send(PackProgress.Failed(t.message ?: "install failed"))
        } finally {
            tmp.delete()
        }
    }.flowOn(Dispatchers.IO)

    private fun AppNetworkResult.Failure.describe(fallback: String): String {
        val parts = listOfNotNull(
            fallback,
            code?.let { "HTTP $it" },
            message?.takeIf { it.isNotBlank() },
            cause?.javaClass?.simpleName,
            cause?.message?.takeIf { it.isNotBlank() },
        )
        return parts.distinct().joinToString(": ")
    }

    private suspend fun downloadClientJar(
        urls: List<String>,
        destination: File,
        onProgress: (Float) -> Unit,
    ): File? {
        var lastFailure: AppNetworkResult.Failure? = null
        for (url in urls) {
            when (val result = http.downloadTo(url, destination, userAgent = AssetMirrors.BROWSER_UA, progress = onProgress)) {
                is AppNetworkResult.Success -> return result.value
                is AppNetworkResult.Failure -> {
                    Log.w(TAG, "client.jar source failed url=$url failure=${result.describe("client.jar unavailable")}")
                    lastFailure = result
                    onProgress(0f)
                }
            }
        }
        throw IllegalStateException(lastFailure?.describe("client.jar unavailable") ?: "client.jar unavailable")
    }

    private fun estimatedBytes(totalBytes: Long, fraction: Float): Long =
        if (totalBytes > 0L) (totalBytes * fraction.coerceIn(0f, 1f)).toLong() else -1L
}
