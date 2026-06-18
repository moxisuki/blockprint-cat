package io.github.moxisuki.blockprint.cat.data.render

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GlbCacheDao {
    @Query("SELECT * FROM glb_cache")
    fun observeAll(): Flow<List<GlbCacheEntity>>

    @Query("SELECT * FROM glb_cache")
    suspend fun getAll(): List<GlbCacheEntity>

    @Query("SELECT COUNT(*) FROM glb_cache")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM glb_cache")
    suspend fun totalSize(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GlbCacheEntity)

    @Query("DELETE FROM glb_cache WHERE uuid = :uuid")
    suspend fun delete(uuid: String)

    @Query("DELETE FROM glb_cache")
    suspend fun clearAll()
}
