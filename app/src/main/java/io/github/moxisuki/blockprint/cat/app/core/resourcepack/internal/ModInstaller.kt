package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn

private const val TAG = "ModInstaller"

@Singleton
class ModInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: AppHttpClient,
    private val extractor: AssetExtractor,
) {
    fun install(
        hit: ModSearchHit,
        version: ModVersionInfo,
        assetsDir: File = File(context.filesDir, "blockprintcat/render_assets"),
    ): Flow<PackProgress> = channelFlow {
        val tmp = File.createTempFile("mod_${hit.slug.safeTempPrefix()}_", ".jar", context.cacheDir)
        try {
            Log.i(TAG, "mod install starting slug=${hit.slug} version=${version.name} file=${version.fileName}")
            send(PackProgress.Preparing(version.fileName))
            send(PackProgress.Downloading(version.fileName, 0f, 0L, version.fileSize))
            val progressThrottle = PackProgressThrottle()
            val jar = when (val result = http.downloadTo(version.fileUrl, tmp, userAgent = AssetMirrors.BROWSER_UA) { fraction ->
                val bytesRead = if (version.fileSize > 0L) {
                    (version.fileSize * fraction.coerceIn(0f, 1f)).toLong()
                } else {
                    -1L
                }
                if (progressThrottle.allow(fraction)) {
                    trySend(PackProgress.Downloading(version.fileName, fraction, bytesRead, version.fileSize))
                }
            }) {
                is AppNetworkResult.Success -> result.value
                is AppNetworkResult.Failure -> throw IllegalStateException(result.describe("jar unavailable"))
            }
            Log.i(TAG, "mod jar downloaded slug=${hit.slug} bytes=${jar.length()}")
            send(PackProgress.Downloading(version.fileName, 1f, jar.length(), version.fileSize))
            val meta = readZipEntry(jar, "META-INF/jarjar/metadata.json")
            val embedded = if (meta != null) {
                runCatching {
                    val arr = org.json.JSONObject(String(meta)).getJSONArray("jars")
                    (0 until arr.length()).map { arr.getJSONObject(it).getString("path") }
                }.getOrElse { emptyList() }
            } else emptyList()
            Log.i(TAG, "mod embedded jars slug=${hit.slug} count=${embedded.size}")
            assetsDir.mkdirs()
            var fileCount = 0
            var totalSize = 0L
            val allNs = mutableSetOf<String>()
            val extractionStageCount = embedded.size + 1
            for ((stageIndex, embeddedPath) in embedded.withIndex()) {
                val embeddedFile = File.createTempFile("mod_embedded_", ".jar", context.cacheDir)
                if (!copyZipEntryToFile(jar, embeddedPath, embeddedFile)) {
                    embeddedFile.delete()
                    continue
                }
                send(
                    PackProgress.Installing(
                        label = embeddedPath,
                        fraction = stageIndex.toFloat() / extractionStageCount,
                        installedFiles = fileCount,
                        totalFiles = null,
                    ),
                )
                val r = extractor.extractFromModJar(embeddedFile, assetsDir) { currentPath, extracted, total ->
                    val localFraction = if (total > 0) extracted.toFloat() / total else 0f
                    val fraction = (stageIndex + localFraction) / extractionStageCount
                    if (progressThrottle.allow(fraction)) {
                        trySend(
                            PackProgress.Installing(
                                label = currentPath,
                                fraction = fraction,
                                installedFiles = fileCount + extracted,
                                totalFiles = null,
                            ),
                        )
                    }
                }
                fileCount += r.fileCount
                totalSize += r.totalSize
                allNs.addAll(r.namespaces)
                embeddedFile.delete()
            }
            val mainStageIndex = embedded.size
            send(
                PackProgress.Installing(
                    label = version.fileName,
                    fraction = mainStageIndex.toFloat() / extractionStageCount,
                    installedFiles = fileCount,
                    totalFiles = null,
                ),
            )
            val r = extractor.extractFromModJar(jar, assetsDir) { currentPath, extracted, total ->
                val localFraction = if (total > 0) extracted.toFloat() / total else 0f
                val fraction = (mainStageIndex + localFraction) / extractionStageCount
                if (progressThrottle.allow(fraction)) {
                    trySend(
                        PackProgress.Installing(
                            label = currentPath,
                            fraction = fraction,
                            installedFiles = fileCount + extracted,
                            totalFiles = null,
                        ),
                        )
                    }
            }
            fileCount += r.fileCount
            totalSize += r.totalSize
            allNs.addAll(r.namespaces)
            send(PackProgress.Installing(version.fileName, 1f, fileCount, fileCount))
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
            Log.i(TAG, "mod install done slug=${hit.slug} files=${entry.fileCount} size=${entry.totalSize}")
            send(PackProgress.Done(entry))
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            Log.e(TAG, "mod install failed slug=${hit.slug} version=${version.name}", t)
            send(PackProgress.Failed(t.message ?: "install failed"))
        } finally {
            tmp.delete()
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

    private fun copyZipEntryToFile(jar: File, path: String, destination: File): Boolean = try {
        java.util.zip.ZipFile(jar).use { zip ->
            val entry = zip.getEntry(path)?.takeUnless { it.isDirectory } ?: return false
            destination.outputStream().use { output ->
                zip.getInputStream(entry).use { input ->
                    input.copyTo(output, 64 * 1024)
                }
            }
        }
        true
    } catch (_: Exception) {
        false
    }

    private fun String.safeTempPrefix(): String =
        replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(24)
            .ifBlank { "pack" }

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
}
