package io.github.moxisuki.blockprint.cat.app.core.pcbridge

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import org.json.JSONObject

private const val DISCOVERY_TAG = "PcBridgeDiscovery"

class PcBridgeDiscovery @Inject constructor() {

    fun observe(): Flow<PcDiscoveredDevice> = flow {
        val socket = DatagramSocket(null).apply {
            reuseAddress = true
            broadcast = true
            soTimeout = 1000
            bind(InetSocketAddress(PcBridgeDiscoveryPort))
        }
        val buffer = ByteArray(1024)
        try {
            while (currentCoroutineContext().isActive) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                }
                val device = parsePacket(packet) ?: continue
                emit(device)
            }
        } finally {
            socket.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun parsePacket(packet: DatagramPacket): PcDiscoveredDevice? {
        return runCatching {
            val json = String(packet.data, 0, packet.length, Charsets.UTF_8)
            val obj = JSONObject(json)
            if (obj.optString("type") != "bp.discovery.v1") {
                return null
            }
            PcDiscoveredDevice(
                host = packet.address.hostAddress.orEmpty(),
                port = obj.optInt("wsPort", PcBridgeDefaultPort),
                deviceName = obj.optString("deviceName", "BlockPrint Link"),
                version = "v${obj.optInt("protocol", 1)}",
                mcVersion = obj.optString("mcVersion"),
                loader = obj.optString("loader"),
                lastSeenAtMillis = System.currentTimeMillis(),
            )
        }.onFailure {
            Log.w(DISCOVERY_TAG, "bad discovery packet", it)
        }.getOrNull()
    }
}
