package io.github.moxisuki.blockprint.cat.app.core.resourcepack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resource_pack")
data class ResourcePackEntity(
    @PrimaryKey val id: String,
    val kind: String, // "vanilla" | "mod"
    val projectSlug: String, // empty for vanilla
    val displayName: String,
    val versionName: String,
    val mcVersion: String?,
    val fileCount: Int,
    val totalSize: Long,
    val installedAt: Long,
    val namespaces: String, // comma-separated
    val source: String, // "modrinth" | "mojang"
    val sourceVersionId: String?,
)