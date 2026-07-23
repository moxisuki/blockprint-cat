package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
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

@Singleton
class VanillaInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: AppHttpClient,
    private val manifest: MojangVersionManifestSource,
    private val extractor: AssetExtractor,
) {
    fun install(): Flow<PackProgress> = channelFlow {
        try {
            send(PackProgress.FetchingManifest("Mojang manifest"))
            val latest = (manifest.latestRelease() as? AppNetworkResult.Success)?.value
                ?: throw IllegalStateException("manifest unavailable")
            val versionUrl = "${AssetMirrors.BMC_API}/mc/game/$latest.json"
            val versionBody = (http.getString(versionUrl) as? AppNetworkResult.Success)?.value
                ?: throw IllegalStateException("version json unavailable")
            val versionJson = JSONObject(versionBody)
            val client = versionJson.getJSONObject("downloads").getJSONObject("client")
            val jarUrl = AssetMirrors.mojang(client.getString("url"))
            send(PackProgress.Downloading("client.jar", 0f))
            val jarBytes = (http.getBytes(jarUrl, userAgent = AssetMirrors.BROWSER_UA) { fraction ->
                trySend(PackProgress.Downloading("client.jar", fraction))
            } as? AppNetworkResult.Success)?.value
                ?: throw IllegalStateException("client.jar unavailable")
            send(PackProgress.Downloading("client.jar", 1f))
            val tmp = withContext(Dispatchers.IO) {
                File(context.cacheDir, "client.jar.tmp").apply { writeBytes(jarBytes) }
            }
            send(PackProgress.Extracting("", 0))
            val assetsDir = File(context.filesDir, "blockprintcat/render_assets").apply { mkdirs() }
            val result = extractor.extractFromJar(tmp, assetsDir) { it == "minecraft" }
            // lang fallback via asset index
            val indexUrl = AssetMirrors.mojang(versionJson.getJSONObject("assetIndex").getString("url"))
            val index = (http.getJson(indexUrl) as? AppNetworkResult.Success)?.value
            runCatching {
                if (index != null) {
                    extractor.downloadLangFromIndex(index, "zh_cn", assetsDir, http)
                    extractor.downloadLangFromIndex(index, "en_us", assetsDir, http)
                }
            }
            val totalSize = assetsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val entry = ResourcePackEntry(
                id = ResourcePackId.Vanilla,
                kind = ResourcePackEntry.Kind.VANILLA,
                displayName = "Minecraft 原版",
                version = latest,
                mcVersion = null,
                fileCount = result.fileCount,
                totalSize = totalSize,
                installedAt = System.currentTimeMillis(),
                namespaces = result.namespaces,
                hasAssets = result.fileCount > 0,
            )
            tmp.delete()
            send(PackProgress.Done(entry))
        } catch (t: Throwable) {
            send(PackProgress.Failed(t.message ?: "install failed"))
        }
    }.flowOn(Dispatchers.IO)
}