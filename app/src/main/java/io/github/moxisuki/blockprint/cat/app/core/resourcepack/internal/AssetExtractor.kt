package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipFile
import org.json.JSONObject

object AssetExtractor {

    private val ALLOWED_NAMESPACE_PREFIXES = listOf(
        "models/",
        "blockstates/",
        "textures/",
        "lang/",
    )

    fun extractFromJar(
        jar: File,
        assetsDir: File,
        namespaceFilter: (String) -> Boolean,
        onProgress: ((currentPath: String, extracted: Int, total: Int) -> Unit)? = null,
    ): AssetExtractorResult {
        var count = 0
        var totalSize = 0L
        val ns = mutableSetOf<String>()
        ZipFile(jar).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.startsWith("assets/") }
                .filter { entry ->
                    val relative = entry.name.removePrefix("assets/")
                    val namespace = relative.substringBefore('/')
                    val namespaceRelative = relative.substringAfter('/', missingDelimiterValue = "")
                    namespaceFilter(namespace) && ALLOWED_NAMESPACE_PREFIXES.any { namespaceRelative.startsWith(it) }
                }
                .toList()
            val safeAssetsRoot = assetsDir.canonicalFile.toPath()
            entries.forEach { entry ->
                val path = entry.name
                val namespace = path.removePrefix("assets/").substringBefore('/')
                ns.add(namespace)
                val relative = path.removePrefix("assets/")
                val dest = File(assetsDir, relative).canonicalFile
                if (!dest.toPath().startsWith(safeAssetsRoot)) return@forEach
                dest.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    BufferedInputStream(input, 64 * 1024).use { buffered ->
                        dest.outputStream().use { output -> buffered.copyTo(output, 64 * 1024) }
                    }
                }
                count++
                totalSize += dest.length()
                onProgress?.invoke(relative, count, entries.size)
            }
        }
        return AssetExtractorResult(count, ns, totalSize)
    }

    fun extractFromModJar(
        jar: File,
        assetsDir: File,
        onProgress: ((currentPath: String, extracted: Int, total: Int) -> Unit)? = null,
    ): AssetExtractorResult = extractFromJar(jar, assetsDir, { it != "minecraft" }, onProgress)

    suspend fun downloadLangFromIndex(
        indexJson: JSONObject,
        locale: String,
        assetsDir: File,
        http: AppHttpClient,
    ): Boolean {
        val key = "minecraft/lang/$locale.json"
        val dest = File(assetsDir, key)
        if (dest.isFile) return true
        val objects = indexJson.optJSONObject("objects") ?: return false
        val entry = objects.optJSONObject(key) ?: return false
        val hash = entry.optString("hash")
        if (hash.length < 2) return false
        val url = "${AssetMirrors.BMC_API}/assets/${hash.substring(0, 2)}/$hash"
        val result = http.getBytes(url, userAgent = AssetMirrors.BROWSER_UA)
        val data = (result as? AppNetworkResult.Success)?.value ?: return false
        dest.parentFile?.mkdirs()
        dest.writeBytes(data)
        return true
    }
}
