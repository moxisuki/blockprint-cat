package io.github.moxisuki.blockprint.cat.app.core.data.blueprint

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "blueprints",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["documentId"], unique = true),
    ],
)
data class BlueprintEntity(
    val id: String,
    val documentId: String,
    val fileName: String,
    val displayName: String,
    val author: String,
    val format: String,
    val category: String,
    val blockCount: Int,
    val regionCount: Int,
    val sizeBytes: Long,
    val lastModifiedAt: Long,
    val scannedAt: Long,
)

@Entity(
    tableName = "blueprint_materials",
    primaryKeys = ["blueprintId", "blockId"],
    foreignKeys = [
        ForeignKey(
            entity = BlueprintEntity::class,
            parentColumns = ["id"],
            childColumns = ["blueprintId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["blueprintId"]),
    ],
)
data class BlueprintMaterialEntity(
    val blueprintId: String,
    val blockId: String,
    val count: Int,
)

@Entity(
    tableName = "blueprint_categories",
    primaryKeys = ["name"],
)
data class BlueprintCategoryEntity(
    val name: String,
)
