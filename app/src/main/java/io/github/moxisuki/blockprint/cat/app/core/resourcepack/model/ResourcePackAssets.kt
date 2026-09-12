package io.github.moxisuki.blockprint.cat.app.core.resourcepack.model

import java.io.File

data class ResourcePackNamespaceInfo(
    val namespace: String,
    val rootDir: File,
    val fileCount: Int,
    val totalSize: Long,
    val hasBlockStates: Boolean,
    val hasModels: Boolean,
    val hasTextures: Boolean,
    val hasLang: Boolean,
)

data class ResourcePackAssetPath(
    val namespace: String,
    val relativePath: String,
    val file: File,
) {
    val exists: Boolean get() = file.isFile
}
