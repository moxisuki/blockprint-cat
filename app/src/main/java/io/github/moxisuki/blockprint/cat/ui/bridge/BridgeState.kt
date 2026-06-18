package io.github.moxisuki.blockprint.cat.ui.bridge

import io.github.moxisuki.blockprint.cat.data.DiscoveryPayload
import io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint
import io.github.moxisuki.blockprint.cat.data.bridge.SessionInfo

/**
 * Connection state machine for the PC bridge.
 *
 * Replaces the previous boolean-flag BridgeState (connected / connecting / error)
 * with a sealed class so impossible states (e.g., Connected AND Connecting) are
 * unrepresentable in the type system.
 *
 * Transitions:
 *   Disconnected --connect()--> Connecting --onConnected--> Connected
 *                                     |
 *                                     |--onError / timeout--> Error
 *                                     |
 *   Connected --onError(AUTH_FAILED)--> Error
 *   Connected --disconnect()--> Disconnected
 *   Error --clearError()--> Disconnected
 *   Error --connect()--> Connecting
 */
sealed class ConnectionState {
    data class Disconnected(
        val lastHost: String? = null,
        val lastPort: Int? = null,
    ) : ConnectionState()

    data class Connecting(
        val host: String,
        val port: Int,
    ) : ConnectionState()

    data class Connected(
        val host: String,
        val port: Int,
        val session: SessionInfo,
        val entries: List<RemoteBlueprint>,
    ) : ConnectionState()

    data class Error(
        val lastHost: String? = null,
        val lastPort: Int? = null,
        val message: String,
    ) : ConnectionState()
}

/**
 * Scan state machine for UDP discovery.
 *
 * Replaces the previous `scanning: Boolean` flag.
 * `Scanning.devices` accumulates the devices found during the active scan.
 */
sealed class ScanState {
    object Idle : ScanState()
    data class Scanning(val devices: List<DiscoveryPayload>) : ScanState()
}

/**
 * One-shot events emitted by BridgeViewModel. Channel backed so they
 * don't replay on screen rotation / navigation. The activity collects
 * these to show snackbars and navigate to detail screens.
 */
sealed class BridgeUiEvent {
    data class DownloadStart(val fileName: String) : BridgeUiEvent()
    data class DownloadComplete(val fileName: String, val targetUuid: String) : BridgeUiEvent()
    data class DownloadFailed(val fileName: String, val message: String) : BridgeUiEvent()
    data class UploadSucceeded(val fileName: String) : BridgeUiEvent()
    data class UploadFailed(val fileName: String, val errorCode: String) : BridgeUiEvent()
    data class AuthFailed(val message: String) : BridgeUiEvent()
    data class Disconnected(val unexpected: Boolean) : BridgeUiEvent()
}
