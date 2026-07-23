package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
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

@Singleton
class ModInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: AppHttpClient,
    private val extractor: AssetExtractor,
) {
    fun install(hit: ModSearchHit, version: ModVersionInfo): Flow<PackProgress> = channelFlow {
        try {
            send(PackProgress.Downloading(version.fileName, 0f))
            val result = http.getBytes(version.fileUrl, userAgent = AssetMirrors.BROWSER_UA) { fraction ->
                trySend(PackProgress.Downloading(version.fileName, fraction))
            }
            val bytes = (result as? AppNetworkResult.Success)?.value
                ?: throw IllegalStateException("jar unavailable")
            val tmp = withContext(Dispatchers.IO) {
                File(context.cacheDir, version.fileName).apply { writeBytes(bytes) }
            }
            send(PackProgress.Downloading(version.fileName, 1f))
            val meta = readZipEntry(tmp, "META-INF/jarjar/metadata.json")
            val embedded = if (meta != null) {
                runCatching {
                    val arr = org.json.JSONObject(String(meta)).getJSONArray("jars")
                    (0 until arr.length()).map { arr.getJSONObject(it).getString("path") }
                }.getOrElse { emptyList() }
            } else emptyList()
            val assetsDir = File(context.filesDir, "blockprintcat/render_assets").apply { mkdirs() }
            var fileCount = 0
            val allNs = mutableSetOf<String>()
            for (embeddedPath in embedded) {
                val embeddedData = readZipEntry(tmp, embeddedPath) ?: continue
                val embeddedFile = File(context.cacheDir, embeddedPath.substringAfterLast('/'))
                    .apply { writeBytes(embeddedData) }
                send(PackProgress.Extracting(embeddedPath, ++fileCount))
                val r = extractor.extractFromModJar(embeddedFile, assetsDir)
                allNs.addAll(r.namespaces)
            }
            send(PackProgress.Extracting(version.fileName, ++fileCount))
            val r = extractor.extractFromModJar(tmp, assetsDir)
            allNs.addAll(r.namespaces)
            tmp.delete()
            val totalSize = assetsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val entry = ResourcePackEntry(
                id = ResourcePackId.mod(hit.slug),
                kind = ResourcePackEntry.Kind.MOD,
                displayName = hit.title,
                version = version.name,
                mcVersion = version.gameVersions.firstOrNull(),
                fileCount = fileCount,
                totalSize = totalSize,
                installedAt = System.currentTimeMillis(),
                namespaces = allNs,
                hasAssets = fileCount > 0,
            )
            send(PackProgress.Done(entry))
        } catch (t: Throwable) {
            send(PackProgress.Failed(t.message ?: "install failed"))
        }
    }.flowOn(Dispatchers.IO)

    private fun readZipEntry(jar: File, path: String): ByteArray? = try {
        java.util.zip.ZipInputStream(jar.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == path && !entry.isDirectory) return zis.readBytes()
                zis.closeEntry()
                entry = zis.nextEntry
            }
            null
        }
    } catch (_: Exception) { null }
}