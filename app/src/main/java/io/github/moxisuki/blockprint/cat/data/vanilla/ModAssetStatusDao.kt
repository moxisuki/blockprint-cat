package io.github.moxisuki.blockprint.cat.data.vanilla

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ModAssetStatusDao {
    @Query("SELECT * FROM mod_asset_status WHERE projectSlug = :slug")
    suspend fun get(slug: String): ModAssetStatusEntity?

    @Query("SELECT * FROM mod_asset_status")
    suspend fun getAll(): List<ModAssetStatusEntity>

    @Query("SELECT * FROM mod_asset_status")
    fun observeAll(): Flow<List<ModAssetStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ModAssetStatusEntity)

    @Query("DELETE FROM mod_asset_status WHERE projectSlug = :slug")
    suspend fun delete(slug: String)
}
