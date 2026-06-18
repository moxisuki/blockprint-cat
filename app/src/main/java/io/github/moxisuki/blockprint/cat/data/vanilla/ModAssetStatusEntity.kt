package io.github.moxisuki.blockprint.cat.data.vanilla

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mod_asset_status")
data class ModAssetStatusEntity(
    @PrimaryKey val projectSlug: String,
    val projectName: String,
    val versionName: String,
    val mcVersion: String,
    val fileCount: Int,
    val totalSize: Long,
    val installedAt: Long,
    /** Comma-separated list of asset namespaces this mod provides (e.g. "create_connected,minecraft") */
    val namespaces: String = "",
)

