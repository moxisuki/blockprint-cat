package io.github.moxisuki.blockprint.cat.data.blueprint

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageConfigDao {
    @Query("SELECT * FROM storage_config WHERE id = 1")
    suspend fun get(): StorageConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StorageConfigEntity)

    @Query("DELETE FROM storage_config WHERE id = 1")
    suspend fun clear()

    @Query("SELECT * FROM storage_config WHERE id = 1")
    fun observe(): Flow<StorageConfigEntity?>
}
