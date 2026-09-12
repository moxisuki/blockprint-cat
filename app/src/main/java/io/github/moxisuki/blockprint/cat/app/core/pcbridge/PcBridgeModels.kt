package io.github.moxisuki.blockprint.cat.app.core.pcbridge

import androidx.compose.runtime.Immutable

const val PcBridgeDefaultPort: Int = 18080
const val PcBridgeDiscoveryPort: Int = 18081

@Immutable
data class PcBridgeState(
    val discoveryActive: Boolean = false,
    val discoveredDevices: List<PcDiscoveredDevice> = emptyList(),
    val connection: PcBridgeConnection = PcBridgeConnection.Disconnected,
    val session: PcBridgeSession? = null,
    val blueprints: List<PcRemoteBlueprint> = emptyList(),
    val tasks: List<PcTransferTask> = emptyList(),
    val lastError: PcBridgeError? = null,
)

@Immutable
sealed interface PcBridgeConnection {
    @Immutable
    data object Disconnected : PcBridgeConnection

    @Immutable
    data class Connecting(
        val host: String,
        val port: Int,
    ) : PcBridgeConnection

    @Immutable
    data class Connected(
        val host: String,
        val port: Int,
    ) : PcBridgeConnection

    @Immutable
    data class Failed(
        val host: String,
        val port: Int,
        val code: String,
        val message: String,
    ) : PcBridgeConnection
}

@Immutable
data class PcDiscoveredDevice(
    val host: String,
    val port: Int,
    val deviceName: String,
    val version: String,
    val mcVersion: String,
    val loader: String,
    val lastSeenAtMillis: Long,
) {
    val id: String
        get() = "$host:$port"
}

@Immutable
data class PcBridgeSession(
    val instanceId: String,
    val deviceName: String,
    val mcVersion: String,
    val loader: String,
    val loaderVersion: String,
    val folderName: String,
) {
    val compactName: String
        get() = buildString {
            append(mcVersion.ifBlank { "Minecraft" })
            if (loader.isNotBlank()) {
                append(" · ")
                append(loader)
            }
            if (loaderVersion.isNotBlank()) {
                append(" ")
                append(loaderVersion)
            }
        }
}

@Immutable
data class PcRemoteBlueprint(
    val id: String,
    val fileName: String,
    val format: String,
    val name: String,
    val width: Int,
    val height: Int,
    val depth: Int,
    val blocks: Int,
    val author: String,
    val description: String,
    val minecraftDataVersion: Int?,
    val version: Int?,
    val regions: Int,
    val source: String,
    val sizeBytes: Long,
    val lastModifiedAt: Long,
) {
    val displayName: String
        get() = name.ifBlank { fileName.substringBeforeLast('.', fileName) }
}

@Immutable
data class PcTransferTask(
    val taskId: String,
    val transferId: String?,
    val kind: String,
    val blueprintId: String?,
    val fileName: String,
    val phase: PcTaskPhase,
    val status: PcTaskStatus,
    val bytesDone: Long,
    val bytesTotal: Long,
    val progress: Float,
    val errorCode: String? = null,
    val errorMessage: String? = null,
) {
    val active: Boolean
        get() = status == PcTaskStatus.Queued || status == PcTaskStatus.Running
}

enum class PcTaskPhase {
    Queued,
    Opening,
    Transferring,
    Verifying,
    Importing,
    Done,
    Failed,
    Cancelled,
}

enum class PcTaskStatus {
    Queued,
    Running,
    Done,
    Failed,
    Cancelled,
}

@Immutable
data class PcBridgeError(
    val code: String,
    val message: String,
)
