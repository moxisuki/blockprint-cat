package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import io.github.moxisuki.blockprint.core.model.BlockPrintDocument
import java.io.File

interface ResourcePackAssetLocator {
    fun assetsRootDir(): File
    fun installedNamespaces(): Set<String>
    fun hasAssets(namespace: String): Boolean
    fun missingNamespacesFor(document: BlockPrintDocument): Set<String>
    suspend fun loadLang(namespace: String, locale: String): String?
    fun textureCandidates(blockId: String, maxResults: Int = 3): List<File>
}