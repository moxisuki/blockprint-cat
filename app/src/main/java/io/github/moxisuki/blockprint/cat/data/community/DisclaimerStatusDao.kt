package io.github.moxisuki.blockprint.cat.data.community

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DisclaimerStatusDao {
    @Query("SELECT * FROM disclaimer_status WHERE id = 1")
    suspend fun get(): DisclaimerStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DisclaimerStatusEntity)
}
