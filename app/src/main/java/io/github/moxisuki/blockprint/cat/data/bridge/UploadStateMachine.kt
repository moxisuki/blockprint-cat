package io.github.moxisuki.blockprint.cat.data.bridge

import java.util.UUID

enum class UploadPhase { IDLE, WAITING_READY, SENDING_CHUNKS, WAITING_RESULT }

/** Common parent for everything the state machine emits (sends + business events). */
sealed interface UploadSignal

sealed class UploadAction : UploadSignal {
    data class SendText(
        val type: String,
        val requestId: String,
        val fileName: String,
        val size: Long? = null,
        val overwrite: Boolean? = null,
        val sha256: String? = null,
    ) : UploadAction()
}

sealed class UploadEvent : UploadSignal {
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
    // Timeout / disconnect recovery is the caller's responsibility — they
    // must null out the state machine on `BridgeEvent.Disconnected` to
    // avoid a stale `WAITING_*` phase being silently preserved across reconnects.

    private var sentBytes: Long = 0L

    /** Caller has just sent upload/init. State → WAITING_READY. */
    fun onInit(): List<UploadSignal> {
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
    fun onServerReady(): List<UploadSignal> {
        if (phase != UploadPhase.WAITING_READY) return emptyList()
        phase = UploadPhase.SENDING_CHUNKS
        return emptyList()
    }

    /** Client just sent one binary chunk. */
    fun onChunkSent(chunkSize: Int): List<UploadSignal> {
        if (phase != UploadPhase.SENDING_CHUNKS) return emptyList()
        sentBytes += chunkSize
        if (sentBytes >= size) phase = UploadPhase.WAITING_RESULT
        return emptyList()
    }

    /** Client just sent upload/commit text frame. */
    fun onCommit(): List<UploadSignal> {
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
    fun onResult(ok: Boolean, errorCode: String?, sha256: String?): List<UploadSignal> {
        if (phase != UploadPhase.WAITING_READY && phase != UploadPhase.WAITING_RESULT) {
            return emptyList()
        }
        phase = UploadPhase.IDLE
        return listOf(UploadEvent.Result(fileName, ok, errorCode, sha256))
    }

    /** Server sent upload/done — informational ack. Emit progress=total and end. */
    fun onDone(): List<UploadSignal> {
        return listOf(UploadEvent.Progress(fileName, size))
    }
}
