package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import io.github.moxisuki.blockprint.core.model.BlockPrintDocument
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackAssetPath
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackNamespaceInfo
import java.io.File

interface ResourcePackAssetLocator {
    fun assetsRootDir(): File
    fun namespaceDir(namespace: String): File
    fun installedNamespaces(): Set<String>
    fun installedNamespaceInfo(): List<ResourcePackNamespaceInfo>
    fun namespaceInfo(namespace: String): ResourcePackNamespaceInfo?
    fun hasNamespace(namespace: String): Boolean
    fun hasAssets(namespace: String): Boolean
    fun missingNamespacesFor(document: BlockPrintDocument): Set<String>
    fun blockstatePath(blockId: String): ResourcePackAssetPath?
    fun blockModelPath(namespace: String, modelPath: String): ResourcePackAssetPath?
    fun texturePath(namespace: String, texturePath: String): ResourcePackAssetPath?
    suspend fun loadLang(namespace: String, locale: String): String?

    suspend fun loadDisplayName(blockId: String, locales: List<String>): String?
    fun textureCandidates(blockId: String, maxResults: Int = 3): List<File>
}
