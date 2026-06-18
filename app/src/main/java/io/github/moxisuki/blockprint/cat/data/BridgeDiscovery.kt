package io.github.moxisuki.blockprint.cat.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveryPayload(
    val host: String,
    val wsPort: Int,
    val tokenHint: String,
)

@Singleton
class BridgeDiscovery @Inject constructor() {
    private val _discoveries = MutableSharedFlow<DiscoveryPayload>(replay = 4)
    val discoveries: SharedFlow<DiscoveryPayload> = _discoveries.asSharedFlow()

    private var socket: DatagramSocket? = null
    private var running = false

    suspend fun start() = withContext(Dispatchers.IO) {
        if (running) return@withContext
        running = true
        try {
            socket = DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(PORT_UDP))
            }
            Log.d(TAG, "listening on UDP $PORT_UDP")
            val buf = ByteArray(2048)
            while (running) {
                val packet = DatagramPacket(buf, buf.size)
                try {
                    socket?.receive(packet)
                } catch (e: Exception) {
                    if (!running) break
                    continue
                }
                val json = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val host = packet.address.hostAddress ?: continue
                try {
                    val obj = org.json.JSONObject(json)
                    if (obj.optString("type") != TYPE_DISCOVERY) continue
                    val port = obj.optInt("wsPort", PORT_WS)
                    val hint = obj.optString("tokenHint", "")
                    _discoveries.emit(DiscoveryPayload(host, port, hint))
                    Log.d(TAG, "discovered $host:$port hint=$hint")
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "start failed", e)
        } finally {
            running = false
        }
    }

    fun stop() {
        running = false
        runCatching { socket?.close() }
        socket = null
        Log.d(TAG, "stopped")
    }

    companion object {
        private const val TAG = "BridgeDiscovery"
        const val PORT_UDP = 18081
        const val PORT_WS = 18080
        private const val TYPE_DISCOVERY = "blockprintlink/discovery"
    }
}
