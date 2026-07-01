package io.github.moxisuki.blockprint.cat.ui.bridge

import android.content.Context
import android.util.Log
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.BridgeDiscovery
import io.github.moxisuki.blockprint.cat.data.DispatcherProvider
import io.github.moxisuki.blockprint.cat.data.DiscoveryPayload
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeClient
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeEvent
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceDao
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity
import io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint
import io.github.moxisuki.blockprint.cat.data.bridge.SessionInfo
import io.github.moxisuki.blockprint.cat.data.bridge.SessionTransitions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "BridgeSessionCtrl"

const val CONNECT_TIMEOUT_MS = 10_000L

/**
 * Owns the connection / discovery / pairing concerns of the PC bridge.
 *
 * One half of the BridgeViewModel split — the other half is
 * [BridgeTransferController]. Both are constructed and held by
 * [BridgeViewModel] which exposes their flows through a single facade
 * API to avoid touching any of the seven downstream consumers.
 *
 * Responsibilities:
 *  - [connectionState] state machine (Disconnected / Connecting / Connected / Error)
 *  - UDP discovery scan ([scanState], [discoveries])
 *  - Persistent pairing list via [pairedDeviceDao] ([paired])
 *  - 10-second connect timeout
 *  - Auto-reconnect from the most recent paired device on init and on
 *    `reconnectIfNeeded()`
 *
 * Routed events (consumed from [BridgeClient.eventFlow]):
 *  - [BridgeEvent.Connected], [BridgeEvent.ListChanged] → connection state
 *  - [BridgeEvent.Disconnected], [BridgeEvent.Error]        → connection state
 *  - transfer-protocol events (DownloadStart, UploadProgress, etc.)
 *    are ignored here and are handled by [BridgeTransferController].
 *
 * Not @Inject-annotated: holds bare `Context` and `CoroutineScope` which
 * Hilt cannot resolve without extra modules. [BridgeViewModel] builds this
 * controller in its `init` block.
 */
class BridgeSessionController(
    private val context: Context,
    private val bridgeClient: BridgeClient,
    private val pairedDeviceDao: PairedDeviceDao,
    private val bridgeDiscovery: BridgeDiscovery,
    private val dispatcherProvider: DispatcherProvider,
    private val eventBus: BridgeEventBus,
    private val scope: CoroutineScope,
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _discoveries = MutableStateFlow<List<DiscoveryPayload>>(emptyList())
    val discoveries: StateFlow<List<DiscoveryPayload>> = _discoveries.asStateFlow()

    val paired: StateFlow<List<PairedDeviceEntity>> =
        pairedDeviceDao.observeAll()
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Current token used by the last `connect()` call. Re-used by auto-reconnect. */
    @Volatile
    private var lastToken: String = ""

    init {
        // Auto-connect from the most recent paired device.
        scope.launch {
            val saved = pairedDeviceDao.mostRecent()
            if (saved != null) {
                if (saved.host.isBlank() || saved.wsPort <= 0) {
                    Log.w(TAG, "init: cleaning invalid saved device host='${saved.host}':${saved.wsPort}")
                    pairedDeviceDao.delete(saved.host, saved.wsPort, saved.folderName)
                } else {
                    lastToken = saved.token
                    _connectionState.value = ConnectionState.Connecting(
                        host = saved.host,
                        port = saved.wsPort,
                    )
                    bridgeClient.connect(saved.host, saved.wsPort, saved.token)
                }
            }
        }

        // Subscribe only to session-relevant events. Transfer events fall
        // through and are handled by [BridgeTransferController].
        scope.launch {
            bridgeClient.eventFlow.collect { event ->
                when (event) {
                    is BridgeEvent.Connected -> onConnected(event.session, event.entries)
                    is BridgeEvent.ListChanged -> onListChanged(event.session, event.entries)
                    is BridgeEvent.Disconnected -> onDisconnected()
                    is BridgeEvent.Error -> onError(event.code, event.message)
                    else -> Unit
                }
            }
        }
    }

    fun startScan() {
        if (_scanState.value is ScanState.Scanning) return
        Log.d(TAG, "startScan")
        _scanState.value = ScanState.Scanning(emptyList())
        scope.launch(dispatcherProvider.io) {
            bridgeDiscovery.start()
        }
        scope.launch {
            bridgeDiscovery.discoveries.collect { payload ->
                val current = (_scanState.value as? ScanState.Scanning)?.devices ?: return@collect
                if (current.none { it.host == payload.host && it.wsPort == payload.wsPort }) {
                    Log.d(TAG, "discovered ${payload.host}:${payload.wsPort} hint=${payload.tokenHint}")
                    _scanState.value = ScanState.Scanning(current + payload)
                }
            }
        }
    }

    fun stopScan() {
        if (_scanState.value !is ScanState.Scanning) return
        Log.d(TAG, "stopScan")
        _scanState.value = ScanState.Idle
        bridgeDiscovery.stop()
    }

    fun connect(host: String, port: Int, token: String) {
        if (host.isBlank() || port !in 1..65535 || token.isBlank()) {
            Log.w(TAG, "connect: invalid args host='$host':$port, ignored")
            return
        }
        stopScan()
        Log.d(TAG, "connect($host:$port)")
        lastToken = token
        _connectionState.value = ConnectionState.Connecting(host, port)
        bridgeClient.connect(host, port, token)
        scope.launch {
            delay(CONNECT_TIMEOUT_MS)
            if (_connectionState.value is ConnectionState.Connecting) {
                _connectionState.value = ConnectionState.Error(
                    lastHost = host,
                    lastPort = port,
                    message = context.getString(R.string.bridge_error_timeout),
                )
                bridgeClient.disconnect()
                Log.w(TAG, "connect timeout host=$host:$port")
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "disconnect")
        bridgeClient.disconnect()
        val s = _connectionState.value
        val (host, port) = when (s) {
            is ConnectionState.Connected -> s.host to s.port
            is ConnectionState.Connecting -> s.host to s.port
            is ConnectionState.Error -> (s.lastHost ?: "") to (s.lastPort ?: 0)
            is ConnectionState.Disconnected -> (s.lastHost ?: "") to (s.lastPort ?: 0)
        }
        if (host.isNotBlank() && port > 0) {
            Log.d(TAG, "disconnect $host:$port")
        }
        _connectionState.value = ConnectionState.Disconnected(
            lastHost = host.ifBlank { null },
            lastPort = if (port > 0) port else null,
        )
    }

    fun reconnectIfNeeded() {
        val s = _connectionState.value
        if (s is ConnectionState.Connected || s is ConnectionState.Connecting) return
        scope.launch {
            val saved = pairedDeviceDao.mostRecent()
            if (saved != null && saved.host.isNotBlank() && saved.wsPort > 0 && saved.token.isNotBlank()) {
                Log.d(TAG, "reconnectIfNeeded: reconnecting to ${saved.host}:${saved.wsPort}")
                connect(saved.host, saved.wsPort, saved.token)
            }
        }
    }

    fun requestList() {
        if (_connectionState.value is ConnectionState.Connected) bridgeClient.requestList()
    }

    fun clearError() {
        val s = _connectionState.value
        if (s is ConnectionState.Error) {
            _connectionState.value = ConnectionState.Disconnected(
                lastHost = s.lastHost,
                lastPort = s.lastPort,
            )
        }
    }

    private suspend fun onConnected(
        session: SessionInfo,
        entries: List<RemoteBlueprint>,
    ) {
        val connected = SessionTransitions.nextOnConnected(
            current = _connectionState.value,
            session = session,
            entries = entries,
        ) ?: run {
            Log.d(TAG, "onConnected: rejected (not Connecting or invalid host/port)")
            return
        }
        Log.d(TAG, "onConnected: ${entries.size} entries, ${session.folderName}")
        _connectionState.value = connected

        val folder = session.folderName.ifBlank { connected.host }
        val existing = pairedDeviceDao.find(connected.host, connected.port, folder)
        val label = existing?.label ?: folder
        pairedDeviceDao.upsert(
            PairedDeviceEntity(
                host = connected.host,
                wsPort = connected.port,
                folderName = folder,
                token = lastToken,
                tokenHint = existing?.tokenHint ?: "",
                label = label,
                lastConnectedAt = System.currentTimeMillis(),
                mcVersion = session.mcVersion,
                loader = session.loader,
                loaderVersion = session.loaderVersion,
            )
        )
        Log.i(TAG, "connected to ${connected.host}:${connected.port} ($label)")
    }

    private fun onListChanged(
        session: SessionInfo,
        entries: List<RemoteBlueprint>,
    ) {
        Log.d(TAG, "onListChanged: ${entries.size} entries")
        SessionTransitions.nextOnListChanged(_connectionState.value, session, entries)
            ?.let { _connectionState.value = it }
    }

    private suspend fun onError(code: String, message: String?) {
        Log.d(TAG, "onError($code, $message)")
        val current = _connectionState.value
        val tokenErrorMessage = context.getString(R.string.bridge_error_token)
        val errorState = SessionTransitions.nextOnError(
            current = current,
            code = code,
            message = message,
            tokenErrorMessage = tokenErrorMessage,
        )
        if (errorState == null) {
            Log.w(TAG, "onError during $current: $code $message (no transition)")
            return
        }
        _connectionState.value = errorState
        if (code == "AUTH_FAILED") {
            bridgeClient.disconnect()
            eventBus.emit(BridgeUiEvent.AuthFailed(tokenErrorMessage))
        }
    }

    private fun onDisconnected() {
        Log.d(TAG, "onDisconnected")
        val (disconnected, unexpected) = SessionTransitions.nextOnDisconnected(
            _connectionState.value,
        ) ?: return
        _connectionState.value = disconnected
        eventBus.emit(BridgeUiEvent.Disconnected(unexpected))
        if (unexpected) {
            Log.w(TAG, "unexpected disconnect host=${disconnected.lastHost} port=${disconnected.lastPort}")
        }
    }
}
