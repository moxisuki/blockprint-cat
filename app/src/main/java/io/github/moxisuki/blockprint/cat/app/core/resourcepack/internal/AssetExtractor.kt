package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import java.io.File
import java.util.zip.ZipInputStream
import org.json.JSONObject

object AssetExtractor {

    private val VANILLA_PREFIXES = listOf(
        "assets/minecraft/models/",
        "assets/minecraft/blockstates/",
        "assets/minecraft/textures/",
        "assets/minecraft/lang/",
    )

    fun extractFromJar(
        jar: File,
        assetsDir: File,
        namespaceFilter: (String) -> Boolean,
    ): AssetExtractorResult {
        var count = 0
        val ns = mutableSetOf<String>()
        ZipInputStream(jar.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val path = entry.name
                if (!entry.isDirectory && path.startsWith("assets/")) {
                    val namespace = path.removePrefix("assets/").substringBefore('/')
                    val inWhitelist = VANILLA_PREFIXES.any { path.startsWith(it) }
                    if (namespaceFilter(namespace) && inWhitelist) {
                        ns.add(namespace)
                        val dest = File(assetsDir, path.removePrefix("assets/"))
                        dest.parentFile?.mkdirs()
                        dest.outputStream().use { zis.copyTo(it) }
                        count++
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return AssetExtractorResult(count, ns)
    }

    fun extractFromModJar(jar: File, assetsDir: File): AssetExtractorResult =
        extractFromJar(jar, assetsDir) { it != "minecraft" }

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
        val result = http.getBytes(url, userAgent = AssetMirrors.BROWSER_UA) ?: return false
        val data = (result as? AppNetworkResult.Success)?.value ?: return false
        dest.parentFile?.mkdirs()
        dest.writeBytes(data)
        return true
    }
}
