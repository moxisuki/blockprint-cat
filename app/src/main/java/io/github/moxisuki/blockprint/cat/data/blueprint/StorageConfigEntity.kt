package io.github.moxisuki.blockprint.cat.data.blueprint

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_config")
data class StorageConfigEntity(
    @PrimaryKey val id: Int = 1,
    val treeUri: String,
    val folderDocId: String,
    val displayName: String,
)
