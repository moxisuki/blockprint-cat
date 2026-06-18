package io.github.moxisuki.blockprint.cat.data.blueprint

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlueprintMetaDao {
    @Query("SELECT * FROM blueprints ORDER BY displayName ASC")
    fun observeAll(): Flow<List<BlueprintMetaEntity>>

    @Query("SELECT COUNT(*) FROM blueprints")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM blueprints ORDER BY displayName ASC")
    suspend fun getAll(): List<BlueprintMetaEntity>

    @Query("SELECT * FROM blueprints WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): BlueprintMetaEntity?

    @Query("SELECT * FROM blueprints WHERE fileDocId = :docId")
    suspend fun getByDocId(docId: String): BlueprintMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BlueprintMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<BlueprintMetaEntity>)

    @Query("DELETE FROM blueprints WHERE uuid = :uuid")
    suspend fun delete(uuid: String)

    @Query("DELETE FROM blueprints")
    suspend fun deleteAll()
}
