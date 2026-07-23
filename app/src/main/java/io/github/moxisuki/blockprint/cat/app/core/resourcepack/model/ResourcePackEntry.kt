package io.github.moxisuki.blockprint.cat.app.core.resourcepack.model

data class ResourcePackEntry(
    val id: ResourcePackId,
    val kind: Kind,
    val displayName: String,
    val version: String,
    val mcVersion: String?,
    val fileCount: Int,
    val totalSize: Long,
    val installedAt: Long,
    val namespaces: Set<String>,
    val hasAssets: Boolean,
) {
    enum class Kind { VANILLA, MOD }
}