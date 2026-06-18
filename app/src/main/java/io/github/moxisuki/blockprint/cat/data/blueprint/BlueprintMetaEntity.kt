package io.github.moxisuki.blockprint.cat.data.blueprint

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blueprints")
data class BlueprintMetaEntity(
    @PrimaryKey val uuid: String,
    val fileDocId: String,
    val fileName: String,
    val displayName: String,
    val author: String,
    val regionCount: Int,
    val blockCount: Int,
    val format: String,
    val lastScannedAt: Long,
)
