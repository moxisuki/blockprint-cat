package io.github.moxisuki.blockprint.cat.data.vanilla

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VanillaAssetStatusDao {
    @Query("SELECT * FROM vanilla_asset_status WHERE id = 1")
    suspend fun get(): VanillaAssetStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VanillaAssetStatusEntity)

    @Query("DELETE FROM vanilla_asset_status WHERE id = 1")
    suspend fun clear()

    @Query("SELECT * FROM vanilla_asset_status WHERE id = 1")
    fun observe(): Flow<VanillaAssetStatusEntity?>
}
