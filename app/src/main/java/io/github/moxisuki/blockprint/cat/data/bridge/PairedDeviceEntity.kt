package io.github.moxisuki.blockprint.cat.data.bridge

import androidx.room.Entity

@Entity(tableName = "paired_device", primaryKeys = ["host", "wsPort", "folderName"])
data class PairedDeviceEntity(
    val host: String,
    val wsPort: Int,
    val folderName: String = "",
    val token: String,
    val tokenHint: String,
    val label: String? = null,
    val lastConnectedAt: Long,
    val mcVersion: String? = null,
    val loader: String? = null,
    val loaderVersion: String? = null,
)
