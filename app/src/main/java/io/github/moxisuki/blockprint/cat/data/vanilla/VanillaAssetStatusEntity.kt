package io.github.moxisuki.blockprint.cat.data.vanilla

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vanilla_asset_status")
data class VanillaAssetStatusEntity(
    @PrimaryKey val id: Int = 1,
    val version: String,
    val fileCount: Int,
    val totalSize: Long,
    val installedAt: Long,
)
