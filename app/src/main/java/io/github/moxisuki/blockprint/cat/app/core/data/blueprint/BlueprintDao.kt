package io.github.moxisuki.blockprint.cat.app.core.data.blueprint

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BlueprintDao {

    @Query(
        "SELECT * FROM blueprints " +
            "ORDER BY lastModifiedAt DESC, scannedAt DESC, displayName COLLATE NOCASE ASC",
    )
    abstract fun observeLocalBlueprints(): Flow<List<BlueprintEntity>>

    @Query("SELECT * FROM blueprints WHERE id = :id LIMIT 1")
    abstract fun observeBlueprint(id: String): Flow<BlueprintEntity?>

    @Query("SELECT * FROM blueprint_materials WHERE blueprintId = :blueprintId ORDER BY count DESC")
    abstract fun observeMaterials(blueprintId: String): Flow<List<BlueprintMaterialEntity>>

    @Query("SELECT * FROM blueprint_categories ORDER BY name COLLATE NOCASE ASC")
    abstract fun observeCategories(): Flow<List<BlueprintCategoryEntity>>

    @Query("SELECT * FROM blueprints")
    abstract suspend fun getAllBlueprints(): List<BlueprintEntity>

    @Query("SELECT COUNT(*) FROM blueprints")
    abstract suspend fun countBlueprints(): Int

    @Query("SELECT * FROM blueprint_categories ORDER BY name COLLATE NOCASE ASC")
    abstract suspend fun getCategories(): List<BlueprintCategoryEntity>

    @Query("SELECT * FROM blueprints WHERE id = :id LIMIT 1")
    abstract suspend fun getBlueprint(id: String): BlueprintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertBlueprint(entity: BlueprintEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertMaterials(entities: List<BlueprintMaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertCategory(entity: BlueprintCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertCategories(entities: List<BlueprintCategoryEntity>)

    @Query("DELETE FROM blueprint_materials WHERE blueprintId = :blueprintId")
    protected abstract suspend fun deleteMaterials(blueprintId: String)

    @Query("DELETE FROM blueprints")
    abstract suspend fun deleteAllBlueprints()

    @Query("DELETE FROM blueprint_materials")
    protected abstract suspend fun deleteAllMaterials()

    @Query("DELETE FROM blueprint_categories")
    protected abstract suspend fun deleteAllCategories()

    @Query("DELETE FROM blueprint_categories WHERE name = :name")
    protected abstract suspend fun deleteCategoryEntity(name: String)

    @Query("UPDATE blueprint_categories SET name = :newCategory WHERE name = :oldCategory")
    protected abstract suspend fun renameCategoryEntity(oldCategory: String, newCategory: String)

    @Query("DELETE FROM blueprints WHERE id = :id")
    abstract suspend fun deleteBlueprint(id: String)

    @Query("DELETE FROM blueprints WHERE documentId NOT IN (:documentIds)")
    abstract suspend fun deleteBlueprintsMissingFrom(documentIds: List<String>)

    @Query("UPDATE blueprints SET category = :category WHERE id IN (:ids)")
    abstract suspend fun updateCategory(ids: List<String>, category: String)

    @Query("UPDATE blueprints SET category = :newCategory WHERE category = :oldCategory")
    protected abstract suspend fun renameBlueprintCategory(oldCategory: String, newCategory: String)

    @Query("UPDATE blueprints SET category = '' WHERE category = :category")
    protected abstract suspend fun clearBlueprintCategory(category: String)

    @Query("UPDATE blueprints SET category = :category WHERE documentId = :documentId")
    abstract suspend fun updateCategoryByDocumentId(documentId: String, category: String)

    @Query(
        "UPDATE blueprints SET documentId = :documentId, fileName = :fileName, " +
            "displayName = :displayName, lastModifiedAt = :lastModifiedAt, scannedAt = :scannedAt " +
            "WHERE id = :id",
    )
    abstract suspend fun updateFileName(
        id: String,
        documentId: String,
        fileName: String,
        displayName: String,
        lastModifiedAt: Long,
        scannedAt: Long,
    )

    @Transaction
    open suspend fun upsertBlueprintWithMaterials(
        blueprint: BlueprintEntity,
        materials: List<BlueprintMaterialEntity>,
    ) {
        upsertBlueprint(blueprint)
        deleteMaterials(blueprint.id)
        if (materials.isNotEmpty()) {
            upsertMaterials(materials)
        }
    }

    @Transaction
    open suspend fun clearBlueprintMetadata() {
        deleteAllMaterials()
        deleteAllBlueprints()
        deleteAllCategories()
    }

    @Transaction
    open suspend fun createCategory(name: String) {
        upsertCategory(BlueprintCategoryEntity(name))
    }

    @Transaction
    open suspend fun upsertCategoryNames(names: List<String>) {
        val entities = names
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { BlueprintCategoryEntity(it) }
        if (entities.isNotEmpty()) {
            upsertCategories(entities)
        }
    }

    @Transaction
    open suspend fun renameCategory(oldCategory: String, newCategory: String) {
        renameCategoryEntity(oldCategory, newCategory)
        renameBlueprintCategory(oldCategory, newCategory)
    }

    @Transaction
    open suspend fun deleteCategory(category: String) {
        deleteCategoryEntity(category)
        clearBlueprintCategory(category)
    }
}
