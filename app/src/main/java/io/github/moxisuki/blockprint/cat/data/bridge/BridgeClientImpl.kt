package io.github.moxisuki.blockprint.cat.data.bridge

import android.util.Log
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BridgeClient"

@Singleton
class BridgeClientImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : BridgeClient {

    private val events = MutableSharedFlow<BridgeEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val eventFlow: Flow<BridgeEvent> = events.asSharedFlow()

    @Volatile
    private var currentTarget: Triple<String, Int, String>? = null
    private var currentWs: WebSocket? = null
    private var downloadingFileName: String? = null
    private var currentUploadSm: UploadStateMachine? = null
    private var currentUploadSize: Long = -1L
    private val downloadSm = DownloadStateMachine()

    override val isOpen: Boolean get() = currentWs != null && currentTarget != null

    override fun connect(host: String, port: Int, token: String) {
        val target = Triple(host, port, token)
        if (currentWs != null && currentTarget == target) {
            Log.d(TAG, "connect: already connected to $target, no-op")
            return
        }
        if (currentWs != null) {
            Log.d(TAG, "connect: target changed, disconnecting first")
            disconnect()
        }

        val url = "ws://$host:$port/ws?token=$token"
        Log.d(TAG, "connecting to $url")
        val request = Request.Builder().url(url).build()
        val ws = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "connected, code=${response.code}")
                requestList()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onMessage(ws: WebSocket, bytes: okio.ByteString) {
                handleBinary(bytes.toByteArray())
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "failure: ${t.message}, code=${response?.code}")
                if (response?.code == 401) {
                    events.tryEmit(BridgeEvent.Error("AUTH_FAILED", "Token 错误"))
                } else {
                    events.tryEmit(BridgeEvent.Error("CONNECT_FAILED", t.message))
                }
                events.tryEmit(BridgeEvent.Disconnected)
                currentWs = null
                currentTarget = null
                currentUploadSm = null
                currentUploadSize = -1L
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "closed: $code $reason")
                events.tryEmit(BridgeEvent.Disconnected)
                currentWs = null
                currentTarget = null
                currentUploadSm = null
                currentUploadSize = -1L
            }
        })
        currentWs = ws
        currentTarget = target
    }

    override fun disconnect() {
        currentWs?.close(1000, "client disconnect")
        currentWs = null
        currentTarget = null
        downloadingFileName = null
    }

    override fun requestList() {
        val id = "list-${UUID.randomUUID().toString().take(8)}"
        val msg = JSONObject().apply {
            put("type", "list")
            put("requestId", id)
        }
        send(msg.toString())
    }

    override fun requestDownload(fileName: String) {
        val requestId = downloadSm.newRequestId()
        val signals = downloadSm.onDownloadRequested(fileName = fileName, requestId = requestId, source = null)
        val sendText = signals.filterIsInstance<DownloadAction.SendText>().first()
        val msg = JSONObject().apply {
            put("type", sendText.type)
            put("requestId", sendText.requestId)
            put("fileName", sendText.fileName)
        }
        send(msg.toString())
    }

    override fun requestUpload(fileName: String, data: ByteArray, overwrite: Boolean) {
        val ws = currentWs ?: run {
            Log.w(TAG, "requestUpload: not connected")
            return
        }
        currentUploadSize = data.size.toLong()
        events.tryEmit(BridgeEvent.UploadProgress(fileName, 0L))

        // All upload work — SHA-256, upload/init text frame, binary chunk
        // streaming, upload/commit — runs on this daemon thread so the
        // calling thread (potentially UI for a click handler) isn't blocked
        // by hashing a 100 MB file. OkHttp backpressure handles flow
        // control; no Thread.sleep throttling.
        Thread {
            try {
                val sha = try {
                    val md = java.security.MessageDigest.getInstance("SHA-256")
                    val digest = md.digest(data)
                    digest.joinToString("") { "%02x".format(it) }
                } catch (e: Exception) {
                    Log.w(TAG, "SHA-256 failed: ${e.message}")
                    ""
                }
                // Re-check connection — user may have disconnected between
                // requestUpload() and this thread starting.
                val liveWs = currentWs ?: run {
                    Log.w(TAG, "requestUpload: aborted, ws gone before thread ran")
                    currentUploadSize = -1L
                    return@Thread
                }
                val sm = UploadStateMachine(fileName = fileName, size = data.size.toLong(), overwrite = overwrite, clientSha = sha)
                currentUploadSm = sm

                val initSignals = sm.onInit()
                val init = initSignals.filterIsInstance<UploadAction.SendText>().first()
                val initJson = JSONObject().apply {
                    put("type", init.type)
                    put("requestId", init.requestId)
                    put("fileName", init.fileName)
                    put("size", init.size)
                    put("overwrite", init.overwrite.toString())
                    if (!init.sha256.isNullOrBlank()) put("sha256", init.sha256)
                }
                liveWs.send(initJson.toString())
                Log.d(TAG, "requestUpload: sent ${init.type} requestId=${init.requestId} size=${init.size}")

                val chunkSize = 32 * 1024
                var offset = 0
                while (offset < data.size) {
                    if (currentUploadSm == null) {
                        Log.w(TAG, "upload aborted: disconnected mid-stream at $offset/${data.size}")
                        return@Thread
                    }
                    val end = minOf(offset + chunkSize, data.size)
                    val chunk = ByteString.of(*data.copyOfRange(offset, end))
                    val ok = liveWs.send(chunk)
                    if (!ok) {
                        Log.w(TAG, "ws.send returned false at $offset/${data.size}")
                        break
                    }
                    sm.onChunkSent(end - offset)
                    offset = end
                    events.tryEmit(BridgeEvent.UploadProgress(fileName, offset.toLong()))
                }
                if (offset >= data.size) {
                    val commitSignals = sm.onCommit()
                    val commit = commitSignals.filterIsInstance<UploadAction.SendText>().first()
                    val commitJson = JSONObject().apply {
                        put("type", commit.type)
                        put("requestId", commit.requestId)
                        put("fileName", commit.fileName)
                    }
                    liveWs.send(commitJson.toString())
                    Log.d(TAG, "requestUpload: sent ${commit.type} for ${commit.fileName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "upload send failed: ${e.message}", e)
            }
        }.apply { isDaemon = true }.start()
    }

    private fun send(text: String) {
        Log.d(TAG, "send: ${text.take(200)}")
        currentWs?.send(text)
    }

    private fun handleMessage(text: String) {
        Log.d(TAG, "recv: ${text.take(300)}")
        try {
            val obj = JSONObject(text)
            when (val type = obj.optString("type")) {
                "list/response", "list/changed" -> {
                    val session = SessionInfo(
                        mcVersion = obj.optString("mcVersion"),
                        loader = obj.optString("loader"),
                        loaderVersion = obj.optString("loaderVersion"),
                        folderName = obj.optString("folderName"),
                    )
                    val arr = obj.optJSONArray("entries") ?: JSONArray()
                    val entries = (0 until arr.length()).map { i -> parseEntry(arr.getJSONObject(i)) }
                    if (type == "list/response") {
                        events.tryEmit(BridgeEvent.Connected(session, entries))
                    } else {
                        events.tryEmit(BridgeEvent.ListChanged(session, entries))
                    }
                }
                "download/ready" -> {
                    val fn = obj.getString("fileName")
                    val size = obj.optLong("size", -1L)
                    val sha = obj.optString("sha256", "")
                    val reqId = obj.optString("requestId", "")
                    downloadSm.onServerReady(reqId, size = size, sha256 = sha)
                    events.tryEmit(BridgeEvent.DownloadStart(fn, size, sha))
                }
                "download/done" -> {
                    val fn = obj.optString("fileName", "")
                    val reqId = obj.optString("requestId", "")
                    val ok = obj.optBoolean("ok", false)
                    val bytes = obj.optLong("bytes", 0L)
                    val sha = obj.optString("sha256", "")
                    val out = downloadSm.onServerDone(reqId, ok = ok, bytes = bytes, sha256 = sha)
                    out.forEach { signal ->
                        when (signal) {
                            is DownloadEvent.Complete -> events.tryEmit(BridgeEvent.DownloadComplete(signal.fileName, signal.payload))
                            is DownloadEvent.Failed -> {
                                Log.w(TAG, "download failed: ${signal.fileName} ${signal.errorCode}")
                                // TODO Task 4 doesn't add a new BridgeEvent for failed download yet;
                                // emit a generic Error event so BridgeViewModel surfaces it.
                                events.tryEmit(BridgeEvent.Error("DOWNLOAD_FAILED", "${signal.fileName}: ${signal.errorCode}"))
                            }
                            else -> { /* no-op */ }
                        }
                    }
                    downloadingFileName = null
                }
                "upload/result" -> {
                    val fn = obj.optString("fileName", "")
                    val ok = obj.optBoolean("ok", false)
                    val err = obj.optString("error", "").takeIf { it.isNotBlank() }
                    val sha = obj.optString("sha256", "").takeIf { it.isNotBlank() }
                    currentUploadSm?.onResult(ok = ok, errorCode = err, sha256 = sha)?.forEach { signal ->
                        when (signal) {
                            is UploadEvent.Result -> events.tryEmit(BridgeEvent.UploadResult(signal.fileName, signal.ok, signal.errorCode))
                            else -> { /* no-op for now */ }
                        }
                    }
                    currentUploadSm = null
                    currentUploadSize = -1L
                }
                "upload/done" -> {
                    // upload/done is informational only — just log the final SHA.
                    val sha = obj.optString("sha256", "")
                    val fn = obj.optString("fileName", "")
                    Log.d(TAG, "upload/done: $fn sha=${sha.take(16)}... size=${if (currentUploadSize > 0) currentUploadSize else 0L}")
                    currentUploadSize = -1L
                    currentUploadSm = null
                }
                "error" -> {
                    events.tryEmit(BridgeEvent.Error(
                        obj.optString("code", "UNKNOWN"),
                        obj.optString("message"),
                    ))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "parse error: ${e.message}")
        }
    }

    private fun handleBinary(data: ByteArray) {
        // v2 binary frames don't carry requestId. The download state machine
        // routes by the single RECEIVING download (UI mutex from Task 5 ensures
        // only one is in flight per connection).
        val out = downloadSm.onOrphanBinaryReceived(data)
        out.forEach { signal ->
            when (signal) {
                is DownloadEvent.Complete -> events.tryEmit(BridgeEvent.DownloadComplete(signal.fileName, signal.payload))
                is DownloadEvent.Failed -> events.tryEmit(BridgeEvent.Error("DOWNLOAD_FAILED", "${signal.fileName}: ${signal.errorCode}"))
                else -> { /* no-op */ }
            }
        }
    }

    private fun parseEntry(obj: JSONObject) = RemoteBlueprint(
        fileName = obj.getString("fileName"),
        format = obj.optString("format", "unknown"),
        name = obj.optString("name"),
        width = obj.optInt("width"), height = obj.optInt("height"), depth = obj.optInt("depth"),
        blocks = obj.optInt("blocks"),
        author = obj.optString("author"),
        description = obj.optString("description"),
        minecraftDataVersion = if (obj.isNull("minecraftDataVersion")) null else obj.optInt("minecraftDataVersion"),
        version = if (obj.isNull("version")) null else obj.optInt("version"),
        regions = obj.optInt("regions"),
    )
}
