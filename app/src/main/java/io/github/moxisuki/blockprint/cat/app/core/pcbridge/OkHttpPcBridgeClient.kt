package io.github.moxisuki.blockprint.cat.app.core.pcbridge

import android.util.Log
import io.github.moxisuki.blockprint.cat.BuildConfig
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "PcBridgeClient"
private const val SUBPROTOCOL = "bp.link.v1"
private const val BINARY_MAGIC = "BPL1"

@Singleton
class OkHttpPcBridgeClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : PcBridgeClient {

    private val eventFlow = MutableSharedFlow<PcBridgeEvent>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val events: Flow<PcBridgeEvent> = eventFlow.asSharedFlow()

    private val lock = Any()

    @Volatile
    private var currentWebSocket: WebSocket? = null

    @Volatile
    private var currentTarget: PcBridgeTarget? = null

    @Volatile
    private var currentConnectionId: Long = 0L

    private val activeDownloads = mutableMapOf<String, ActiveDownload>()

    override val isOpen: Boolean
        get() = currentWebSocket != null

    override fun connect(host: String, port: Int, token: String) {
        val target = PcBridgeTarget(host = host.trim(), port = port, token = token)
        if (target.host.isBlank() || target.token.isBlank()) {
            eventFlow.tryEmit(PcBridgeEvent.Error("BAD_REQUEST", "host/token required"))
            return
        }

        if (currentWebSocket != null && currentTarget == target) {
            requestList()
            return
        }
        disconnect()

        currentTarget = target
        val connectionId = synchronized(lock) {
            currentConnectionId += 1L
            currentConnectionId
        }
        val request = Request.Builder()
            .url("ws://${target.host}:${target.port}/bp/link")
            .header("Authorization", "Bearer ${target.token}")
            .header("Sec-WebSocket-Protocol", SUBPROTOCOL)
            .build()

        currentWebSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (!isCurrentConnection(connectionId, target)) {
                        webSocket.close(1000, "stale connection")
                        return
                    }
                    Log.d(TAG, "connected ${target.host}:${target.port} code=${response.code}")
                    currentWebSocket = webSocket
                    eventFlow.tryEmit(PcBridgeEvent.Opened(target.host, target.port))
                    sendJson("session.hello") {
                        put(
                            "payload",
                            JSONObject()
                                .put("client", "blockprint-cat")
                                .put("clientVersion", BuildConfig.VERSION_NAME)
                                .put("protocol", 1)
                                .put("features", JSONArray(listOf("list", "download", "tasks", "cancel"))),
                        )
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (isCurrentConnection(connectionId, target)) {
                        handleText(text)
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    if (isCurrentConnection(connectionId, target)) {
                        handleBinary(bytes.toByteArray())
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (!isCurrentConnection(connectionId, target)) return
                    Log.w(TAG, "connection failed code=${response?.code}", t)
                    val code = if (response?.code == 401) "AUTH_FAILED" else "CONNECT_FAILED"
                    val message = if (response?.code == 401) "AUTH_FAILED" else t.message.orEmpty()
                    eventFlow.tryEmit(PcBridgeEvent.Error(code, message.ifBlank { code }))
                    clearConnection(webSocket, connectionId)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "closed code=$code reason=$reason")
                    clearConnection(webSocket, connectionId)
                }
            },
        )
    }

    override fun disconnect() {
        synchronized(lock) {
            currentConnectionId += 1L
            activeDownloads.clear()
        }
        currentWebSocket?.close(1000, "client disconnect")
        currentWebSocket = null
        currentTarget = null
    }

    override fun requestList() {
        sendJson("blueprint.list.request") {
            put("payload", JSONObject().put("includeHashes", false))
        }
    }

    override fun requestDownload(blueprintId: String) {
        sendJson("task.create") {
            put(
                "payload",
                JSONObject()
                    .put("kind", "blueprint.download")
                    .put("blueprintId", blueprintId),
            )
        }
    }

    override fun cancelTask(taskId: String) {
        sendJson("task.cancel") {
            put("payload", JSONObject().put("taskId", taskId))
        }
    }

    private fun handleText(text: String) {
        runCatching {
            val obj = JSONObject(text)
            val payload = obj.optJSONObject("payload") ?: JSONObject()
            when (val type = obj.optString("type")) {
                "session.ready" -> {
                    eventFlow.tryEmit(PcBridgeEvent.SessionReady(payload.toSession()))
                    requestList()
                }
                "blueprint.list.response" -> {
                    eventFlow.tryEmit(PcBridgeEvent.ListReceived(payload.optJSONArray("entries").orEmptyEntries()))
                }
                "blueprint.list.changed" -> {
                    eventFlow.tryEmit(PcBridgeEvent.ListChanged(payload.optJSONArray("entries").orEmptyEntries()))
                }
                "task.created",
                "task.progress",
                -> {
                    val task = payload.toTask()
                    rememberDownload(task)
                    eventFlow.tryEmit(PcBridgeEvent.TaskChanged(task))
                }
                "task.finished" -> {
                    val task = payload.toTask(
                        fallbackPhase = PcTaskPhase.Done,
                        fallbackStatus = PcTaskStatus.Done,
                    )
                    val completed = finishDownload(task, payload.optString("sha256"))
                    eventFlow.tryEmit(PcBridgeEvent.TaskChanged(task))
                    if (completed != null) {
                        eventFlow.tryEmit(completed)
                    }
                }
                "task.failed" -> {
                    val task = payload.toTask(
                        fallbackPhase = PcTaskPhase.Failed,
                        fallbackStatus = PcTaskStatus.Failed,
                    )
                    synchronized(lock) {
                        task.transferId?.let(activeDownloads::remove)
                    }
                    eventFlow.tryEmit(PcBridgeEvent.TaskChanged(task))
                    eventFlow.tryEmit(
                        PcBridgeEvent.Error(
                            code = task.errorCode ?: "TASK_FAILED",
                            message = task.errorMessage ?: task.fileName,
                        ),
                    )
                }
                "task.cancelled" -> {
                    val taskId = payload.optString("taskId")
                    val transferId = payload.optString("transferId").takeIf { it.isNotBlank() }
                    synchronized(lock) {
                        transferId?.let(activeDownloads::remove)
                    }
                    eventFlow.tryEmit(
                        PcBridgeEvent.TaskChanged(
                            PcTransferTask(
                                taskId = taskId,
                                transferId = transferId,
                                kind = "blueprint.download",
                                blueprintId = null,
                                fileName = "",
                                phase = PcTaskPhase.Cancelled,
                                status = PcTaskStatus.Cancelled,
                                bytesDone = 0L,
                                bytesTotal = 0L,
                                progress = 0f,
                            ),
                        ),
                    )
                }
                "error" -> {
                    val error = obj.optJSONObject("error")
                    eventFlow.tryEmit(
                        PcBridgeEvent.Error(
                            code = error?.optString("code").orEmpty().ifBlank { "UNKNOWN" },
                            message = error?.optString("message").orEmpty().ifBlank { "UNKNOWN" },
                        ),
                    )
                }
                else -> Log.w(TAG, "unknown bridge message: $type")
            }
        }.onFailure { error ->
            Log.w(TAG, "bad json from bridge", error)
            eventFlow.tryEmit(PcBridgeEvent.Error("BAD_JSON", error.message.orEmpty()))
        }
    }

    private fun handleBinary(data: ByteArray) {
        runCatching {
            if (data.size < 6 || String(data, 0, 4, Charsets.US_ASCII) != BINARY_MAGIC) {
                throw IllegalArgumentException("bad binary magic")
            }
            val headerLength = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
            val headerStart = 6
            val payloadStart = headerStart + headerLength
            if (headerLength <= 0 || payloadStart > data.size) {
                throw IllegalArgumentException("bad binary header length")
            }
            val header = JSONObject(String(data, headerStart, headerLength, Charsets.UTF_8))
            if (header.optString("type") != "transfer.chunk") return
            val transferId = header.getString("transferId")
            val taskId = header.getString("taskId")
            val offset = header.optLong("offset", -1L)
            val length = header.optInt("length", -1)
            if (length < 0 || payloadStart + length != data.size) {
                throw IllegalArgumentException("chunk length mismatch")
            }
            synchronized(lock) {
                val download = activeDownloads[transferId] ?: ActiveDownload(
                    taskId = taskId,
                    transferId = transferId,
                    fileName = "",
                    expectedSize = -1L,
                ).also { activeDownloads[transferId] = it }
                if (offset >= 0 && download.output.size().toLong() != offset) {
                    throw IllegalStateException("chunk offset mismatch ${download.output.size()} != $offset")
                }
                download.output.write(data, payloadStart, length)
            }
        }.onFailure { error ->
            Log.w(TAG, "bad binary frame", error)
            eventFlow.tryEmit(PcBridgeEvent.Error("BAD_BINARY", error.message.orEmpty()))
        }
    }

    private fun rememberDownload(task: PcTransferTask) {
        val transferId = task.transferId ?: return
        synchronized(lock) {
            val existing = activeDownloads[transferId]
            if (existing == null) {
                activeDownloads[transferId] = ActiveDownload(
                    taskId = task.taskId,
                    transferId = transferId,
                    fileName = task.fileName,
                    expectedSize = task.bytesTotal,
                )
            } else {
                existing.fileName = task.fileName.ifBlank { existing.fileName }
                existing.expectedSize = task.bytesTotal.takeIf { it > 0L } ?: existing.expectedSize
            }
        }
    }

    private fun finishDownload(task: PcTransferTask, sha256: String): PcBridgeEvent.DownloadComplete? {
        val transferId = task.transferId ?: return null
        val download = synchronized(lock) {
            activeDownloads.remove(transferId)
        } ?: return null
        val bytes = download.output.toByteArray()
        val expected = task.bytesTotal.takeIf { it > 0L } ?: download.expectedSize
        if (expected > 0L && bytes.size.toLong() != expected) {
            eventFlow.tryEmit(PcBridgeEvent.Error("LENGTH_MISMATCH", "${task.fileName}: ${bytes.size}/$expected"))
            return null
        }
        if (sha256.isNotBlank()) {
            val actual = bytes.sha256Hex()
            if (!sha256.equals(actual, ignoreCase = true)) {
                eventFlow.tryEmit(PcBridgeEvent.Error("SHA_MISMATCH", task.fileName))
                return null
            }
        }
        return PcBridgeEvent.DownloadComplete(
            taskId = task.taskId,
            transferId = transferId,
            fileName = task.fileName.ifBlank { download.fileName },
            bytes = bytes,
        )
    }

    private fun sendJson(type: String, block: JSONObject.() -> Unit = {}) {
        val ws = currentWebSocket ?: run {
            eventFlow.tryEmit(PcBridgeEvent.Error("NOT_CONNECTED", "Bridge is not connected"))
            return
        }
        val obj = JSONObject()
            .put("messageId", "msg-${shortId()}")
            .put("type", type)
            .put("timestamp", System.currentTimeMillis())
            .apply(block)
        if (!ws.send(obj.toString())) {
            eventFlow.tryEmit(PcBridgeEvent.Error("SEND_FAILED", "WebSocket send failed"))
        }
    }

    private fun clearConnection(webSocket: WebSocket, connectionId: Long) {
        if (currentConnectionId != connectionId) return
        synchronized(lock) {
            activeDownloads.clear()
        }
        if (currentWebSocket === webSocket) {
            currentWebSocket = null
            currentTarget = null
        }
        eventFlow.tryEmit(PcBridgeEvent.Closed)
    }

    private fun isCurrentConnection(connectionId: Long, target: PcBridgeTarget): Boolean =
        currentConnectionId == connectionId && currentTarget == target
}

private data class PcBridgeTarget(
    val host: String,
    val port: Int,
    val token: String,
)

private class ActiveDownload(
    val taskId: String,
    val transferId: String,
    var fileName: String,
    var expectedSize: Long,
) {
    val output = ByteArrayOutputStream()
}

private fun JSONObject.toSession(): PcBridgeSession = PcBridgeSession(
    instanceId = optString("instanceId"),
    deviceName = optString("deviceName"),
    mcVersion = optString("mcVersion"),
    loader = optString("loader"),
    loaderVersion = optString("loaderVersion"),
    folderName = optString("folderName"),
)

private fun JSONArray?.orEmptyEntries(): List<PcRemoteBlueprint> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val obj = optJSONObject(index) ?: continue
            val dimensions = obj.optJSONObject("dimensions") ?: JSONObject()
            add(
                PcRemoteBlueprint(
                    id = obj.optString("id").ifBlank {
                        "${obj.optString("source", "schematics")}/${obj.optString("fileName")}"
                    },
                    fileName = obj.optString("fileName"),
                    format = obj.optString("format", "unknown"),
                    name = obj.optString("displayName").ifBlank { obj.optString("name") },
                    width = dimensions.optInt("width"),
                    height = dimensions.optInt("height"),
                    depth = dimensions.optInt("depth"),
                    blocks = obj.optInt("blocks"),
                    author = obj.optString("author"),
                    description = obj.optString("description"),
                    minecraftDataVersion = obj.optNullableInt("minecraftDataVersion"),
                    version = obj.optNullableInt("version"),
                    regions = obj.optInt("regions"),
                    source = obj.optString("source", "schematics"),
                    sizeBytes = obj.optLong("size", -1L),
                    lastModifiedAt = obj.optLong("lastModified", 0L),
                ),
            )
        }
    }
}

private fun JSONObject.toTask(
    fallbackPhase: PcTaskPhase? = null,
    fallbackStatus: PcTaskStatus? = null,
): PcTransferTask {
    val bytesDone = optLong("bytesDone", 0L)
    val bytesTotal = optLong("bytesTotal", 0L)
    val rawProgress = optDouble("progress", if (bytesTotal > 0L) bytesDone.toDouble() / bytesTotal else 0.0)
    return PcTransferTask(
        taskId = optString("taskId"),
        transferId = optString("transferId").takeIf { it.isNotBlank() },
        kind = optString("kind", "blueprint.download"),
        blueprintId = optString("blueprintId").takeIf { it.isNotBlank() },
        fileName = optString("fileName"),
        phase = optString("phase").toTaskPhase() ?: fallbackPhase ?: PcTaskPhase.Queued,
        status = optString("status").toTaskStatus() ?: fallbackStatus ?: PcTaskStatus.Queued,
        bytesDone = bytesDone,
        bytesTotal = bytesTotal,
        progress = rawProgress.toFloat().coerceIn(0f, 1f),
        errorCode = optString("code").takeIf { it.isNotBlank() },
        errorMessage = optString("message").takeIf { it.isNotBlank() },
    )
}

private fun String.toTaskPhase(): PcTaskPhase? = when (lowercase()) {
    "queued" -> PcTaskPhase.Queued
    "opening" -> PcTaskPhase.Opening
    "transferring" -> PcTaskPhase.Transferring
    "verifying" -> PcTaskPhase.Verifying
    "importing" -> PcTaskPhase.Importing
    "done" -> PcTaskPhase.Done
    "failed" -> PcTaskPhase.Failed
    "cancelled" -> PcTaskPhase.Cancelled
    else -> null
}

private fun String.toTaskStatus(): PcTaskStatus? = when (lowercase()) {
    "queued" -> PcTaskStatus.Queued
    "running" -> PcTaskStatus.Running
    "done" -> PcTaskStatus.Done
    "failed" -> PcTaskStatus.Failed
    "cancelled" -> PcTaskStatus.Cancelled
    else -> null
}

private fun JSONObject.optNullableInt(name: String): Int? =
    if (isNull(name)) null else optInt(name)

private fun ByteArray.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(this)
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun shortId(): String = UUID.randomUUID().toString().take(8)
