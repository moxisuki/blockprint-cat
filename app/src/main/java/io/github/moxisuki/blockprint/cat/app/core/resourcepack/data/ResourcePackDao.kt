package io.github.moxisuki.blockprint.cat.app.core.resourcepack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ResourcePackDao {
    @Query("SELECT * FROM resource_pack ORDER BY installedAt DESC")
    fun observeAll(): Flow<List<ResourcePackEntity>>

    @Query("SELECT * FROM resource_pack WHERE id = :id")
    suspend fun get(id: String): ResourcePackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ResourcePackEntity)

    @Query("DELETE FROM resource_pack WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM resource_pack")
    suspend fun clearAll()
}