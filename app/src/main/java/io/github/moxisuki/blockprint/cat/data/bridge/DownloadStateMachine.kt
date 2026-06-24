package io.github.moxisuki.blockprint.cat.data.bridge

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class DownloadPhase { WAITING_READY, RECEIVING }

data class DownloadState(
    val requestId: String,
    val fileName: String,
    var size: Long,
    var sha256: String,
    val accumulator: ByteArrayOutputStreamLite = ByteArrayOutputStreamLite(),
    var phase: DownloadPhase = DownloadPhase.WAITING_READY,
)

/** Lightweight byte accumulator — avoids pulling in java.io.ByteArrayOutputStream so this file stays portable / testable. */
class ByteArrayOutputStreamLite {
    private var buf: ByteArray = ByteArray(1024)
    private var count: Int = 0

    fun write(b: ByteArray, off: Int, len: Int) {
        val need = count + len
        if (need > buf.size) buf = buf.copyOf(maxOf(need, buf.size * 2))
        System.arraycopy(b, off, buf, count, len)
        count = need
    }

    fun toByteArray(): ByteArray = buf.copyOf(count)
}

/** Common parent for everything the state machine emits (sends + business events). */
sealed interface DownloadSignal

sealed class DownloadAction : DownloadSignal {
    data class SendText(
        val type: String,
        val requestId: String,
        val fileName: String,
        val source: String?,
    ) : DownloadAction()
}

sealed class DownloadEvent : DownloadSignal {
    data class Complete(
        val fileName: String,
        val payload: ByteArray,
    ) : DownloadEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Complete) return false
            return fileName == other.fileName && payload.contentEquals(other.payload)
        }
        override fun hashCode(): Int = fileName.hashCode() * 31 + payload.contentHashCode()
    }
    data class Failed(val fileName: String, val errorCode: String) : DownloadEvent()
}

/**
 * Pure download state machine. Tracks multiple concurrent downloads by
 * requestId (server protocol §2.8 allows concurrent downloads).
 * Owns no I/O — caller feeds events in and gets back [DownloadAction]
 * (network sends) and/or [DownloadEvent] (business signals).
 */
class DownloadStateMachine {
    private val states = ConcurrentHashMap<String, DownloadState>()

    fun newRequestId(): String = "dl-" + UUID.randomUUID().toString().take(8)

    fun stateOf(requestId: String): DownloadState? = states[requestId]

    /** Caller is about to send the `download` text frame. Create state eagerly so chunks/done can route by requestId. */
    fun onDownloadRequested(fileName: String, requestId: String, source: String?): List<DownloadSignal> {
        states[requestId] = DownloadState(
            requestId = requestId,
            fileName = fileName,
            size = -1L,
            sha256 = "",
        )
        return listOf(DownloadAction.SendText("download", requestId, fileName, source))
    }

    fun onServerReady(requestId: String, size: Long, sha256: String): List<DownloadSignal> {
        val s = states[requestId] ?: return emptyList()
        s.size = size
        s.sha256 = sha256
        s.phase = DownloadPhase.RECEIVING
        return emptyList()
    }

    fun onBinaryReceived(requestId: String, bytes: ByteArray): List<DownloadSignal> {
        val s = states[requestId] ?: return emptyList() // orphan → drop
        if (s.phase != DownloadPhase.RECEIVING) return emptyList()
        s.accumulator.write(bytes, 0, bytes.size)
        if (s.size >= 0 && s.accumulator.toByteArray().size.toLong() >= s.size) {
            // size reached but server hasn't sent done — auto-complete
            val payload = s.accumulator.toByteArray()
            states.remove(requestId)
            return listOf(DownloadEvent.Complete(s.fileName, payload))
        }
        return emptyList()
    }

    /**
     * Route an inbound binary frame that doesn't carry requestId.
     * Picks the single RECEIVING download (UI mutex ensures at most one exists).
     * If none exists, drops. Returns the resulting signals.
     */
    fun onOrphanBinaryReceived(bytes: ByteArray): List<DownloadSignal> {
        val active = states.values.firstOrNull { it.phase == DownloadPhase.RECEIVING }
            ?: return emptyList()
        return onBinaryReceived(active.requestId, bytes)
    }

    fun onServerDone(requestId: String, ok: Boolean, bytes: Long, sha256: String?): List<DownloadSignal> {
        val s = states.remove(requestId) ?: return emptyList()
        if (!ok) {
            return listOf(DownloadEvent.Failed(s.fileName, "SERVER_NOT_OK"))
        }
        // Optional: SHA-256 verification
        val expected = s.sha256
        if (expected.isNotBlank() && sha256 != null && sha256.isNotBlank() && !expected.equals(sha256, ignoreCase = true)) {
            return listOf(DownloadEvent.Failed(s.fileName, "SHA_MISMATCH"))
        }
        return listOf(DownloadEvent.Complete(s.fileName, s.accumulator.toByteArray()))
    }
}
