package io.github.moxisuki.blockprint.cat.data.bridge

import java.util.UUID

enum class UploadPhase { IDLE, WAITING_READY, SENDING_CHUNKS, WAITING_RESULT }

sealed class UploadAction {
    data class SendText(
        val type: String,
        val requestId: String,
        val fileName: String,
        val size: Long? = null,
        val overwrite: Boolean? = null,
        val sha256: String? = null,
    ) : UploadAction()
}

sealed class UploadEvent {
    data class Result(
        val fileName: String,
        val ok: Boolean,
        val errorCode: String?,
        val sha256: String?,
    ) : UploadEvent()
    data class Progress(val fileName: String, val bytes: Long) : UploadEvent()
}

/**
 * Pure upload state machine. Owns no I/O — caller feeds events in and
 * gets back a list of [UploadAction] (network sends) and/or [UploadEvent]
 * (business signals to emit on the event flow).
 */
class UploadStateMachine(
    val fileName: String,
    val size: Long,
    val overwrite: Boolean,
    val clientSha: String,
) {
    val requestId: String = "up-" + UUID.randomUUID().toString().take(8)

    var phase: UploadPhase = UploadPhase.IDLE
        private set

    private var sentBytes: Long = 0L

    /** Caller has just sent upload/init. State → WAITING_READY. */
    fun onInit(): List<Any> {
        phase = UploadPhase.WAITING_READY
        return listOf(
            UploadAction.SendText(
                type = "upload/init",
                requestId = requestId,
                fileName = fileName,
                size = size,
                overwrite = overwrite,
                sha256 = clientSha.ifBlank { null },
            )
        )
    }

    /** Server sent upload/ready for our requestId. State → SENDING_CHUNKS. */
    fun onServerReady(): List<Any> {
        if (phase != UploadPhase.WAITING_READY) return emptyList()
        phase = UploadPhase.SENDING_CHUNKS
        return emptyList()
    }

    /** Client just sent one binary chunk. */
    fun onChunkSent(chunkSize: Int): List<Any> {
        if (phase != UploadPhase.SENDING_CHUNKS) return emptyList()
        sentBytes += chunkSize
        if (sentBytes >= size) phase = UploadPhase.WAITING_RESULT
        return emptyList()
    }

    /** Client just sent upload/commit text frame. */
    fun onCommit(): List<Any> {
        if (phase != UploadPhase.WAITING_RESULT) return emptyList()
        return listOf(
            UploadAction.SendText(
                type = "upload/commit",
                requestId = requestId,
                fileName = fileName,
            )
        )
    }

    /** Server sent upload/result {ok, error?, sha256?}. */
    fun onResult(ok: Boolean, errorCode: String?, sha256: String?): List<Any> {
        if (phase != UploadPhase.WAITING_RESULT && phase != UploadPhase.SENDING_CHUNKS && phase != UploadPhase.WAITING_READY) {
            return emptyList()
        }
        phase = UploadPhase.IDLE
        return listOf(UploadEvent.Result(fileName, ok, errorCode, sha256))
    }

    /** Server sent upload/done with the final SHA. We just log + emit progress=total. */
    fun onDone(serverSha: String?): List<Any> {
        return listOf(UploadEvent.Progress(fileName, size))
    }

    /** Orphan binary frame received with no in-flight upload. Silently drop. */
    fun onBinaryReceived(bytes: ByteArray): List<Any> {
        // No active upload → drop. No state change.
        if (phase != UploadPhase.SENDING_CHUNKS && phase != UploadPhase.WAITING_READY) {
            return emptyList()
        }
        // Active upload receives binary? In v2 the client never receives binary
        // during upload — only sends. Defensive: ignore.
        return emptyList()
    }
}
