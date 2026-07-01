package io.github.moxisuki.blockprint.cat.ui.bridge

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.moxisuki.blockprint.cat.data.BridgeDiscovery
import io.github.moxisuki.blockprint.cat.data.DispatcherProvider
import io.github.moxisuki.blockprint.cat.data.DiscoveryPayload
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeClient
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceDao
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

private const val TAG = "BridgeViewModel"

private typealias SchematicFormat = io.github.moxisuki.blockprint.core.SchematicFormat

/**
 * Thin facade over [BridgeSessionController] and [BridgeTransferController].
 *
 * History:
 *   Previously this class held both session (connection / scan / pairing)
 *   and transfer (upload / download / convert) responsibilities in one
 *   511-line body. The split into two controllers keeps a clean break
 *   point — each controller owns its own state flows and event handling
 *   — while this facade preserves the public surface so none of the
 *   seven downstream consumers (`MainActivity`, `HomeScreen`,
 *   `ConnectionScreen`, etc.) need to change.
 *
 * Why Hilt injects the raw deps here instead of injecting the controllers
 *   directly: both controllers take a bare `Context` and `CoroutineScope`,
 *   which Hilt cannot resolve without custom @Provides modules. We let
 *   Hilt wire the resolvable deps (BridgeClient, DAOs, BlueprintManager,
 *   BridgeDiscovery, DispatcherProvider, BridgeEventBus) into this VM,
 *   then hand the application context and `viewModelScope` to the two
 *   controllers below.
 *
 * Public API preserved verbatim:
 *   - StateFlows: `connectionState`, `transfers`, `isTaskInFlight`,
 *     `convertInFlight`, `events`, `pairedDevices`, `discoveries`,
 *     `scanState`
 *   - Methods: `connect` / `disconnect` / `reconnectIfNeeded`,
 *     `startScan` / `stopScan`, `requestList`,
 *     `requestDownload` / `requestUpload`,
 *     `convertBlueprint`, `clearError`
 */
@HiltViewModel
class BridgeViewModel @Inject constructor(
    app: Application,
    bridgeClient: BridgeClient,
    pairedDeviceDao: PairedDeviceDao,
    bridgeDiscovery: BridgeDiscovery,
    blueprintManager: BlueprintManager,
    dispatcherProvider: DispatcherProvider,
    private val eventBus: BridgeEventBus,
) : AndroidViewModel(app) {

    private val session: BridgeSessionController
    private val transfer: BridgeTransferController

    init {
        val scope = viewModelScope
        session = BridgeSessionController(
            context = app,
            bridgeClient = bridgeClient,
            pairedDeviceDao = pairedDeviceDao,
            bridgeDiscovery = bridgeDiscovery,
            dispatcherProvider = dispatcherProvider,
            eventBus = eventBus,
            scope = scope,
        )
        transfer = BridgeTransferController(
            context = app,
            bridgeClient = bridgeClient,
            blueprintManager = blueprintManager,
            session = session,
            eventBus = eventBus,
            scope = scope,
        )
    }

    // ---- Session facade ----
    val connectionState: StateFlow<ConnectionState> get() = session.connectionState
    val scanState: StateFlow<ScanState> get() = session.scanState
    val discoveries: StateFlow<List<DiscoveryPayload>> get() = session.discoveries

    /**
     * Stable name preserved from the pre-split VM. Delegates to
     * [BridgeSessionController.paired] (which exposes the same DAO flow).
     */
    val pairedDevices: StateFlow<List<PairedDeviceEntity>> get() = session.paired

    fun startScan() = session.startScan()
    fun stopScan() = session.stopScan()
    fun requestList() = session.requestList()
    fun reconnectIfNeeded() = session.reconnectIfNeeded()
    fun clearError() = session.clearError()

    fun connect(host: String, port: Int, token: String) =
        session.connect(host, port, token)

    /**
     * Mirrors the original VM behaviour: drop any in-flight transfer
     * rows before tearing down the socket, so the UI doesn't show a
     * ghost progress bar after the user disconnects mid-transfer.
     */
    fun disconnect() {
        transfer.clearAllOnDisconnect()
        session.disconnect()
    }

    // ---- Transfer facade ----
    val transfers: StateFlow<List<TransferItem>> get() = transfer.transfers
    val isTaskInFlight: StateFlow<Boolean> get() = transfer.isTaskInFlight
    val convertInFlight: StateFlow<Boolean> get() = transfer.convertInFlight

    fun requestDownload(fileName: String, source: String = "schematics") =
        transfer.requestDownload(fileName, source)

    fun requestUpload(fileName: String, data: ByteArray, overwrite: Boolean = false) =
        transfer.requestUpload(fileName, data, overwrite)

    fun convertBlueprint(uuid: String, target: SchematicFormat, targetExtension: String) =
        transfer.convertBlueprint(uuid, target, targetExtension)

    // ---- Shared event sink ----
    val events: Flow<BridgeUiEvent> get() = eventBus.events
}

// ---------------------------------------------------------------------------
// Transfer primitives kept in this file because both controllers and tests
// (IsAnyTransferInFlightTest) depend on them, and they have no natural
// "session" or "transfer" home of their own.
// ---------------------------------------------------------------------------

/**
 * Pure predicate: is any transfer item currently in the running phase?
 * Extracted for unit testing — see `IsAnyTransferInFlightTest`.
 *
 * NOTE: Per the design spec, the mutex considers ONLY RUNNING transfers
 * as "in-flight". DONE and FAILED transfers (even if still in the list
 * during the 2-second display animation before removal) do NOT block new
 * uploads/downloads.
 */
fun isAnyTransferInFlight(transfers: List<TransferItem>): Boolean =
    transfers.any { it.phase == TransferPhase.RUNNING }

enum class TransferType { DOWNLOAD, UPLOAD }

enum class TransferPhase { RUNNING, DONE, FAILED }

data class TransferItem(
    val id: Long,
    val type: TransferType,
    val fileName: String,
    val totalBytes: Long,
    val receivedBytes: Long = 0L,
    val phase: TransferPhase = TransferPhase.RUNNING,
) {
    val fraction: Float?
        get() = if (totalBytes > 0) (receivedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else null
}
