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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val TAG = "VanillaInstaller"

@Singleton
class VanillaInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: AppHttpClient,
    private val manifest: MojangVersionManifestSource,
    private val extractor: AssetExtractor,
) {
    fun install(): Flow<PackProgress> = channelFlow {
        try {
            Log.i(TAG, "vanilla install starting")
            send(PackProgress.FetchingManifest("Mojang manifest"))

            // Step 1: latest release + its official Mojang URL
            val versionInfo = (manifest.latestVersionInfo() as? AppNetworkResult.Success)?.value
                ?: throw IllegalStateException("manifest unavailable")
            Log.i(TAG, "step 1 ok: release=${versionInfo.release}")

            // Step 2: fetch version metadata JSON. versionJson() applies AssetMirrors.mojang()
            val versionJson = (manifest.versionJson(versionInfo.officialVersionUrl) as? AppNetworkResult.Success)?.value
                ?: throw IllegalStateException("version json unavailable")
            Log.i(TAG, "step 2 ok: ${versionJson.optString("id")}")

            // Step 3: download client.jar (URL comes from the version JSON; mirror it)
            val client = versionJson.getJSONObject("downloads").getJSONObject("client")
            val jarOfficialUrl = client.getString("url")
            val jarUrl = AssetMirrors.mojang(jarOfficialUrl)
            Log.i(TAG, "step 3 downloading $jarUrl (size=${client.optLong("size", -1)})")
            send(PackProgress.Downloading("client.jar", 0f))
            val jarBytes = (http.getBytes(jarUrl, userAgent = AssetMirrors.BROWSER_UA) { fraction ->
                trySend(PackProgress.Downloading("client.jar", fraction))
            } as? AppNetworkResult.Success)?.value
                ?: throw IllegalStateException("client.jar unavailable")
            Log.i(TAG, "step 3 ok: ${jarBytes.size} bytes")
            send(PackProgress.Downloading("client.jar", 1f))

            // Step 4: write to cache + extract assets
            val tmp = withContext(Dispatchers.IO) {
                File(context.cacheDir, "client.jar.tmp").apply { writeBytes(jarBytes) }
            }
            send(PackProgress.Extracting("", 0))
            val assetsDir = File(context.filesDir, "blockprintcat/render_assets").apply { mkdirs() }
            val result = extractor.extractFromJar(tmp, assetsDir) { it == "minecraft" }
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

            val totalSize = assetsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val entry = ResourcePackEntry(
                id = ResourcePackId.Vanilla,
                kind = ResourcePackEntry.Kind.VANILLA,
                displayName = "Minecraft 原版",
                version = versionInfo.release,
                mcVersion = null,
                fileCount = result.fileCount,
                totalSize = totalSize,
                installedAt = System.currentTimeMillis(),
                namespaces = result.namespaces,
                hasAssets = result.fileCount > 0,
            )
            tmp.delete()
            Log.i(TAG, "vanilla install done: ${entry.fileCount} files, ${totalSize}B")
            send(PackProgress.Done(entry))
        } catch (t: Throwable) {
            Log.e(TAG, "vanilla install failed", t)
            send(PackProgress.Failed(t.message ?: "install failed"))
        }
    }.flowOn(Dispatchers.IO)
}
