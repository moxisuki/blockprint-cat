package io.github.moxisuki.blockprint.cat.data.bridge

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bridge_event")
data class BridgeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long,
    val type: String,
    val message: String,
    val host: String? = null,
    val wsPort: Int? = null,
)