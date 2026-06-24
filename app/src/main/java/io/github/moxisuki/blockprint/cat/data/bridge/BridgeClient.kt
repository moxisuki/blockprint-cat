package io.github.moxisuki.blockprint.cat.data.bridge

import kotlinx.coroutines.flow.Flow

data class RemoteBlueprint(
    val fileName: String,
    val format: String,
    val name: String,
    val width: Int, val height: Int, val depth: Int,
    val blocks: Int,
    val author: String,
    val description: String,
    val minecraftDataVersion: Int?,
    val version: Int?,
    val regions: Int,
)

data class SessionInfo(
    val mcVersion: String,
    val loader: String,
    val loaderVersion: String,
    val folderName: String,
)

sealed class BridgeEvent {
    data class Connected(val session: SessionInfo, val entries: List<RemoteBlueprint>) : BridgeEvent()
    data class ListChanged(val session: SessionInfo, val entries: List<RemoteBlueprint>) : BridgeEvent()
    data class DownloadStart(val fileName: String, val size: Long, val sha256: String) : BridgeEvent()
    data class DownloadProgress(val fileName: String, val bytes: Long) : BridgeEvent()
    data class DownloadComplete(val fileName: String, val data: ByteArray) : BridgeEvent()
    data class UploadProgress(val fileName: String, val bytes: Long) : BridgeEvent()
    data class UploadResult(val fileName: String, val ok: Boolean, val error: String? = null) : BridgeEvent()
    data class Error(val code: String, val message: String?) : BridgeEvent()
    object Disconnected : BridgeEvent()
}

interface BridgeClient {
    val eventFlow: Flow<BridgeEvent>
    val isOpen: Boolean

    fun connect(host: String, port: Int, token: String)
    fun disconnect()
    fun requestList()
    fun requestDownload(fileName: String)
    fun requestUpload(fileName: String, data: ByteArray, overwrite: Boolean = false)
}
