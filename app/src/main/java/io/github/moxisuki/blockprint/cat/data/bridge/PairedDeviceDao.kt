package io.github.moxisuki.blockprint.cat.data.bridge

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_device ORDER BY lastConnectedAt DESC")
    fun observeAll(): Flow<List<PairedDeviceEntity>>

    @Query("SELECT * FROM paired_device WHERE host = :host AND wsPort = :port AND folderName = :folderName LIMIT 1")
    suspend fun find(host: String, port: Int, folderName: String): PairedDeviceEntity?

    @Query("SELECT * FROM paired_device ORDER BY lastConnectedAt DESC LIMIT 1")
    suspend fun mostRecent(): PairedDeviceEntity?

    @Upsert
    suspend fun upsert(entity: PairedDeviceEntity)

    @Query("DELETE FROM paired_device WHERE host = :host AND wsPort = :port AND folderName = :folderName")
    suspend fun delete(host: String, port: Int, folderName: String)

    @Query("DELETE FROM paired_device")
    suspend fun clear()
}
