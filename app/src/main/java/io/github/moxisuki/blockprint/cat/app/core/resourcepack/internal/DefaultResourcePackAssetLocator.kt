package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.LangReader
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackAssetLocator
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackAssetPath
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackNamespaceInfo
import io.github.moxisuki.blockprint.core.model.BlockPrintDocument
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class DefaultResourcePackAssetLocator @Inject constructor(
    @ApplicationContext private val context: Context,
) : ResourcePackAssetLocator {

    private fun root(): File = File(context.filesDir, "blockprintcat/render_assets")

    override fun assetsRootDir(): File = root()

    override fun namespaceDir(namespace: String): File = File(root(), namespace.cleanResourceSegment())

    override fun installedNamespaces(): Set<String> {
        val dir = root()
        if (!dir.isDirectory) return emptySet()
        return dir.listFiles()
            ?.filter { it.isDirectory && namespaceInfo(it.name)?.fileCount?.let { count -> count > 0 } == true }
            ?.map { it.name }
            ?.toSet()
            ?: emptySet()
    }

    override fun installedNamespaceInfo(): List<ResourcePackNamespaceInfo> =
        installedNamespaces().mapNotNull(::namespaceInfo).sortedBy { it.namespace }

    override fun namespaceInfo(namespace: String): ResourcePackNamespaceInfo? {
        val cleanNamespace = namespace.cleanResourceSegment()
        val dir = namespaceDir(cleanNamespace)
        if (!dir.isDirectory) return null
        var fileCount = 0
        var totalSize = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                fileCount++
                totalSize += file.length()
            }
        }
        if (fileCount == 0) return null
        return ResourcePackNamespaceInfo(
            namespace = cleanNamespace,
            rootDir = dir,
            fileCount = fileCount,
            totalSize = totalSize,
            hasBlockStates = File(dir, "blockstates").isDirectory,
            hasModels = File(dir, "models").isDirectory,
            hasTextures = File(dir, "textures").isDirectory,
            hasLang = File(dir, "lang").isDirectory,
        )
    }

    override fun hasNamespace(namespace: String): Boolean = namespaceInfo(namespace) != null

    override fun hasAssets(namespace: String): Boolean {
        val dir = namespaceDir(namespace)
        if (!dir.isDirectory) return false
        // Extractors only create these directories when at least one matching
        // asset exists, so avoid recursively walking the namespace for every block.
        return File(dir, "blockstates").isDirectory ||
            File(dir, "models").isDirectory ||
            File(dir, "textures").isDirectory
    }

    override fun missingNamespacesFor(document: BlockPrintDocument): Set<String> {
        val needed = document.regions
            .flatMap { it.palette.entries }
            .map { it.name.substringBefore(':') }
            .toSet()
        return needed.filterNot { hasAssets(it) }.toSet()
    }

    override suspend fun loadLang(namespace: String, locale: String): String? =
        LangReader.loadLang(root(), namespace.cleanResourceSegment(), locale)?.toString()

    override suspend fun loadDisplayName(blockId: String, locales: List<String>): String? =
        withContext(Dispatchers.IO) {
            val namespace = blockId.substringBefore(':', missingDelimiterValue = "minecraft")
            if (namespace.isBlank() || blockId.substringAfter(':', missingDelimiterValue = "").isBlank()) {
                return@withContext null
            }
            val langFiles = buildList {
                locales
                    .asSequence()
                    .map(String::lowercase)
                    .distinct()
                    .forEach { locale ->
                        add(LangReader.loadLang(root(), namespace.cleanResourceSegment(), locale))
                    }
            }
            LangReader.chooseDisplayNameFromObjects(langFiles, blockId)
        }

    override fun blockstatePath(blockId: String): ResourcePackAssetPath? {
        val (namespace, name) = splitResourceId(blockId, defaultNamespace = "minecraft")
        return existingAssetPath(namespace, "blockstates/${name.cleanResourcePath()}.json")
    }

    override fun blockModelPath(namespace: String, modelPath: String): ResourcePackAssetPath? {
        val (resolvedNamespace, resolvedPath) = splitResourceId(modelPath, defaultNamespace = namespace)
        val normalized = resolvedPath
            .removePrefix("models/")
            .removeSuffix(".json")
            .cleanResourcePath()
        return existingAssetPath(resolvedNamespace, "models/$normalized.json")
    }

    override fun texturePath(namespace: String, texturePath: String): ResourcePackAssetPath? {
        val (resolvedNamespace, resolvedPath) = splitResourceId(texturePath, defaultNamespace = namespace)
        val normalized = resolvedPath
            .removePrefix("textures/")
            .removeSuffix(".png")
            .cleanResourcePath()
        return existingAssetPath(resolvedNamespace, "textures/$normalized.png")
    }

    override fun textureCandidates(blockId: String, maxResults: Int): List<File> {
        val (ns, target) = splitResourceId(blockId, defaultNamespace = "minecraft")
        val namespaceRoot = namespaceDir(ns)
        if (!namespaceRoot.isDirectory) return emptyList()
        val candidates = mutableListOf<Pair<File, Int>>()
        for (sub in listOf("textures/item", "textures/block")) {
            val dir = File(namespaceRoot, sub)
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

    private fun existingAssetPath(namespace: String, relativePath: String): ResourcePackAssetPath? {
        val cleanNamespace = namespace.cleanResourceSegment()
        val cleanRelativePath = relativePath.cleanResourcePath()
        val file = File(namespaceDir(cleanNamespace), cleanRelativePath)
        return ResourcePackAssetPath(cleanNamespace, cleanRelativePath, file).takeIf { it.exists }
    }

    private fun splitResourceId(value: String, defaultNamespace: String): Pair<String, String> {
        val colon = value.indexOf(':')
        return if (colon >= 0) {
            value.substring(0, colon).cleanResourceSegment() to value.substring(colon + 1).cleanResourcePath()
        } else {
            defaultNamespace.cleanResourceSegment() to value.cleanResourcePath()
        }
    }

    private fun String.cleanResourceSegment(): String =
        lowercase().replace(Regex("[^a-z0-9_.-]"), "")

    private fun String.cleanResourcePath(): String =
        replace('\\', '/')
            .split('/')
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .joinToString("/")
            .lowercase()
}
