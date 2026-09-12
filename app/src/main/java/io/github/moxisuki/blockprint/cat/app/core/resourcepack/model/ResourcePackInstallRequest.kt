package io.github.moxisuki.blockprint.cat.app.core.resourcepack.model

sealed interface ResourcePackInstallRequest {
    val id: ResourcePackId
    val displayName: String
    val fileName: String
    val totalSize: Long
    val source: ResourcePackInstallSource
    val sourceVersionId: String?

    data object Vanilla : ResourcePackInstallRequest {
        override val id: ResourcePackId = ResourcePackId.Vanilla
        override val displayName: String = "Minecraft"
        override val fileName: String = "client.jar"
        override val totalSize: Long = -1L
        override val source: ResourcePackInstallSource = ResourcePackInstallSource.Mojang
        override val sourceVersionId: String? = null
    }

    data class Mod(
        val hit: ModSearchHit,
        val version: ModVersionInfo,
    ) : ResourcePackInstallRequest {
        override val id: ResourcePackId = ResourcePackId.mod(hit.slug)
        override val displayName: String = hit.title
        override val fileName: String = version.fileName
        override val totalSize: Long = version.fileSize
        override val source: ResourcePackInstallSource = ResourcePackInstallSource.Modrinth
        override val sourceVersionId: String = version.id
    }
}
