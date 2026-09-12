package io.github.moxisuki.blockprint.cat.app.core.pcbridge

import kotlinx.coroutines.flow.Flow

sealed interface PcBridgeEvent {
    data class Opened(val host: String, val port: Int) : PcBridgeEvent

    data class SessionReady(val session: PcBridgeSession) : PcBridgeEvent

    data class ListReceived(
        val entries: List<PcRemoteBlueprint>,
    ) : PcBridgeEvent

    data class ListChanged(
        val entries: List<PcRemoteBlueprint>,
    ) : PcBridgeEvent

    data class TaskChanged(val task: PcTransferTask) : PcBridgeEvent

    data class DownloadComplete(
        val taskId: String,
        val transferId: String,
        val fileName: String,
        val bytes: ByteArray,
    ) : PcBridgeEvent {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DownloadComplete) return false
            return taskId == other.taskId &&
                transferId == other.transferId &&
                fileName == other.fileName &&
                bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = taskId.hashCode()
            result = 31 * result + transferId.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    data class Error(
        val code: String,
        val message: String,
    ) : PcBridgeEvent

    data object Closed : PcBridgeEvent
}

interface PcBridgeClient {
    val events: Flow<PcBridgeEvent>
    val isOpen: Boolean

    fun connect(host: String, port: Int, token: String)
    fun disconnect()
    fun requestList()
    fun requestDownload(blueprintId: String)
    fun cancelTask(taskId: String)
}
