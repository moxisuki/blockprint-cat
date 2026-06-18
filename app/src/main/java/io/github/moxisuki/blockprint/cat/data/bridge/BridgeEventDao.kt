package io.github.moxisuki.blockprint.cat.data.bridge

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BridgeEventDao {
    @Query("SELECT * FROM bridge_event ORDER BY ts DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<BridgeEventEntity>>

    @Insert
    suspend fun insert(event: BridgeEventEntity): Long

    @Query("DELETE FROM bridge_event")
    suspend fun clear()

    @Query("DELETE FROM bridge_event WHERE id NOT IN (SELECT id FROM bridge_event ORDER BY ts DESC LIMIT :keep)")
    suspend fun trim(keep: Int)
}