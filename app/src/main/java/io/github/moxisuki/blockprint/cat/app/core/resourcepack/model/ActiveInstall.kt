package io.github.moxisuki.blockprint.cat.app.core.resourcepack.model

import kotlinx.coroutines.flow.Flow

/**
 * Carries the metadata of an in-flight install alongside its live progress stream.
 * Repository builds one per install and keeps terminal progress visible briefly.
 */
data class ActiveInstall(
    val packId: ResourcePackId,
    val displayName: String,
    val fileName: String,
    val totalSize: Long,
    val source: ResourcePackInstallSource = ResourcePackInstallSource.Unknown,
    val progress: Flow<PackProgress>,
)

enum class ResourcePackInstallSource {
    Mojang,
    Modrinth,
    Unknown,
}
