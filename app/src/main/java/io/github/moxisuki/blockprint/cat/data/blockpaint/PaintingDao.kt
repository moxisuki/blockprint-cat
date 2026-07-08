package io.github.moxisuki.blockprint.cat.data.blockpaint

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaintingDao {
    @Query("SELECT * FROM block_paintings ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PaintingEntity>>

    @Query("SELECT * FROM block_paintings WHERE id = :id")
    suspend fun getById(id: String): PaintingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PaintingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PaintingEntity>)

    @Query("DELETE FROM block_paintings WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM block_paintings")
    suspend fun deleteAll()
}
