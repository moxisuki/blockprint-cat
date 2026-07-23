package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.LangReader
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackAssetLocator
import io.github.moxisuki.blockprint.core.model.BlockPrintDocument
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultResourcePackAssetLocator @Inject constructor(
    @ApplicationContext private val context: Context,
) : ResourcePackAssetLocator {

    private fun root(): File = File(context.filesDir, "blockprintcat/render_assets")

    override fun assetsRootDir(): File = root()

    override fun installedNamespaces(): Set<String> {
        val dir = root()
        if (!dir.isDirectory) return emptySet()
        return dir.listFiles()?.filter { it.isDirectory }?.map { it.name }?.toSet() ?: emptySet()
    }

    override fun hasAssets(namespace: String): Boolean {
        val dir = File(root(), namespace)
        return dir.isDirectory && (dir.listFiles()?.isNotEmpty() == true)
    }

    override fun missingNamespacesFor(document: BlockPrintDocument): Set<String> {
        val needed = document.regions
            .flatMap { it.palette.entries }
            .map { it.name.substringBefore(':') }
            .toSet()
        return needed.filterNot { hasAssets(it) }.toSet()
    }

    override suspend fun loadLang(namespace: String, locale: String): String? =
        LangReader.loadLang(root(), namespace, locale)?.toString()

    override fun textureCandidates(blockId: String, maxResults: Int): List<File> {
        val colon = blockId.indexOf(':')
        val ns = if (colon >= 0) blockId.substring(0, colon) else "minecraft"
        val target = if (colon >= 0) blockId.substring(colon + 1) else blockId
        val root = File(root(), ns)
        if (!root.isDirectory) return emptyList()
        val candidates = mutableListOf<Pair<File, Int>>()
        for (sub in listOf("textures/item", "textures/block")) {
            val dir = File(root, sub)
            if (!dir.isDirectory) continue
            dir.listFiles()?.filter { it.extension == "png" }?.forEach { file ->
                val stem = file.nameWithoutExtension.lowercase()
                val score = when {
                    stem == target -> 1000
                    stem.contains(target) -> 500 - (stem.length - target.length).coerceAtLeast(0)
                    target.contains(stem) -> 400 - (target.length - stem.length).coerceAtLeast(0)
                    else -> null
                } ?: return@forEach
                candidates.add(file to score)
            }
        }
        return candidates.sortedByDescending { it.second }.take(maxResults).map { it.first }
    }
}