package io.github.moxisuki.blockprint.cat.data.blueprint

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.moxisuki.blockprint.cat.data.category.CategoryEntity

@Entity(
    tableName = "blueprints",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("categoryId")],
)
data class BlueprintMetaEntity(
    @PrimaryKey val uuid: String,
    val fileDocId: String,
    val fileName: String,
    val displayName: String,
    val author: String,
    val regionCount: Int,
    val blockCount: Int,
    val format: String,
    @ColumnInfo(defaultValue = "0")
    val lastScannedAt: Long,
    val categoryId: String? = null,
)