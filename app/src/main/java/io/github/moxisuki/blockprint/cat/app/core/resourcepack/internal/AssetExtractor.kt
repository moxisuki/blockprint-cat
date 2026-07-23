package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import java.io.File
import java.util.zip.ZipInputStream

internal object AssetExtractor {

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
}
