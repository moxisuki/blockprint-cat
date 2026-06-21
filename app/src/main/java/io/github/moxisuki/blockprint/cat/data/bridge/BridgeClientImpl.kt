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
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "closed: $code $reason")
                events.tryEmit(BridgeEvent.Disconnected)
                currentWs = null
                currentTarget = null
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
        val id = "dl-${UUID.randomUUID().toString().take(8)}"
        downloadingFileName = fileName
        val msg = JSONObject().apply {
            put("type", "download")
            put("requestId", id)
            put("fileName", fileName)
        }
        send(msg.toString())
    }

    override fun requestUpload(fileName: String, data: ByteArray, overwrite: Boolean) {
        val ws = currentWs ?: run {
            Log.w(TAG, "requestUpload: not connected")
            return
        }
        val sha = try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(data)
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.w(TAG, "SHA-256 failed: ${e.message}")
            ""
        }
        val msg = JSONObject().apply {
            put("type", "upload")
            put("fileName", fileName)
            put("size", data.size)
            put("sha256", sha)
            put("overwrite", overwrite.toString())
        }
        ws.send(msg.toString())
        // 分块发送以提供进度反馈（OkHttp WebSocket send 是非阻塞写入底层 socket，
        // 真正发送字节数不可见；按字节切片并让出线程，使其他协程能观察到中间状态）
        val total = data.size.toLong()
        val chunkSize = 32 * 1024
        events.tryEmit(BridgeEvent.UploadProgress(fileName, 0L))
        Thread {
            try {
                var offset = 0
                while (offset < data.size) {
                    val end = minOf(offset + chunkSize, data.size)
                    val chunk = ByteString.of(*data.copyOfRange(offset, end))
                    val ok = ws.send(chunk)
                    if (!ok) {
                        Log.w(TAG, "ws.send returned false at $offset/$total")
                        break
                    }
                    offset = end
                    events.tryEmit(BridgeEvent.UploadProgress(fileName, offset.toLong()))
                    // 节流：每块之间 sleep 让 UI 有时间显示进度
                    Thread.sleep(40)
                }
                if (offset >= data.size) {
                    events.tryEmit(BridgeEvent.UploadProgress(fileName, total))
                }
            } catch (e: Exception) {
                Log.e(TAG, "upload send failed: ${e.message}", e)
            }
        }.start()
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
                "download/start" -> {
                    val fn = obj.getString("fileName")
                    val size = obj.optLong("size", -1L)
                    val sha = obj.optString("sha256", "")
                    events.tryEmit(BridgeEvent.DownloadStart(fn, size, sha))
                }
                "upload/result" -> {
                    val fn = obj.optString("fileName", "")
                    val ok = obj.optBoolean("ok", false)
                    val err = obj.optString("errorCode", "").takeIf { it.isNotBlank() }
                    events.tryEmit(BridgeEvent.UploadResult(fn, ok, err))
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
        val fn = downloadingFileName
        if (fn != null) {
            Log.d(TAG, "recv binary: ${data.size} bytes for $fn")
            downloadingFileName = null
            events.tryEmit(BridgeEvent.DownloadComplete(fn, data))
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
