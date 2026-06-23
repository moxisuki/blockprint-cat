package io.github.moxisuki.blockprint.cat.ui.bridge

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.moxisuki.blockprint.cat.data.BridgeDiscovery
import io.github.moxisuki.blockprint.cat.data.DispatcherProvider
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeClient
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeEvent
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceDao
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity
import io.github.moxisuki.blockprint.cat.data.bridge.SessionInfo
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.DiscoveryPayload
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BridgeViewModel"

@HiltViewModel
class BridgeViewModel @Inject constructor(
    app: Application,
    private val bridgeClient: BridgeClient,
    private val pairedDeviceDao: PairedDeviceDao,
    private val bridgeDiscovery: BridgeDiscovery,
    private val blueprintManager: BlueprintManager,
    private val dispatcherProvider: DispatcherProvider,
) : AndroidViewModel(app) {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _transfers = MutableStateFlow<List<TransferItem>>(emptyList())
    val transfers: StateFlow<List<TransferItem>> = _transfers.asStateFlow()

    private val _convertInFlight = MutableStateFlow(false)
    val convertInFlight: StateFlow<Boolean> = _convertInFlight.asStateFlow()

    private val _events = Channel<BridgeUiEvent>(
        capacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<BridgeUiEvent> = _events.receiveAsFlow()

    val pairedDevices: StateFlow<List<PairedDeviceEntity>> =
        pairedDeviceDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _discoveries = MutableStateFlow<List<DiscoveryPayload>>(emptyList())
    val discoveries: StateFlow<List<DiscoveryPayload>> = _discoveries.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    fun startScan() {
        if (_scanState.value is ScanState.Scanning) return
        Log.d(TAG, "startScan")
        _scanState.value = ScanState.Scanning(emptyList())
        viewModelScope.launch(dispatcherProvider.io) {
            bridgeDiscovery.start()
        }
        viewModelScope.launch {
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

    @Volatile
    private var wasConnected: Boolean = false

    @Volatile
    private var lastToken: String = ""

    private var transferSeq: Long = 0

    private fun addTransfer(type: TransferType, fileName: String, totalBytes: Long) {
        val item = TransferItem(id = ++transferSeq, type = type, fileName = fileName, totalBytes = totalBytes)
        _transfers.update { it + item }
    }

    private fun updateTransfer(id: Long, receivedBytes: Long) {
        _transfers.update { list -> list.map { if (it.id == id && it.phase == TransferPhase.RUNNING) it.copy(receivedBytes = receivedBytes) else it } }
    }

    private fun setPhase(id: Long, phase: TransferPhase) {
        _transfers.update { list -> list.map { if (it.id == id) it.copy(phase = phase, receivedBytes = if (phase != TransferPhase.RUNNING) it.totalBytes else it.receivedBytes) else it } }
    }

    private fun removeTransfer(id: Long) {
        _transfers.update { it.filter { t -> t.id != id } }
    }

    private fun clearTransfers() {
        _transfers.value = emptyList()
    }

    init {
        viewModelScope.launch {
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

        viewModelScope.launch {
            bridgeClient.eventFlow.collect(::handleBridgeEvent)
        }
    }

    fun connect(host: String, port: Int, token: String) {
        if (host.isBlank() || port !in 1..65535 || token.isBlank()) {
            Log.w(TAG, "connect: invalid args host='$host':$port, ignored")
            return
        }
        stopScan()
        Log.d(TAG, "connect($host:$port)")
        lastToken = token
        clearTransfers()
        _connectionState.value = ConnectionState.Connecting(host, port)
        bridgeClient.connect(host, port, token)
        viewModelScope.launch {
            delay(10_000)
            if (_connectionState.value is ConnectionState.Connecting) {
                clearTransfers()
                _connectionState.value = ConnectionState.Error(
                    lastHost = host,
                    lastPort = port,
                    message = getApplication<android.app.Application>().getString(R.string.bridge_error_timeout),
                )
                bridgeClient.disconnect()
                Log.w(TAG, "connect timeout host=$host:$port")
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "disconnect")
        wasConnected = false
        clearTransfers()
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
        viewModelScope.launch {
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

    fun requestDownload(fileName: String) {
        Log.d(TAG, "requestDownload($fileName)")
        if (_connectionState.value !is ConnectionState.Connected) return
        addTransfer(TransferType.DOWNLOAD, fileName, 0L)
        currentDownloadId = transferSeq
        bridgeClient.requestDownload(fileName)
    }

    fun requestUpload(fileName: String, data: ByteArray, overwrite: Boolean = false) {
        Log.d(TAG, "requestUpload($fileName, ${data.size}B, overwrite=$overwrite)")
        if (_connectionState.value !is ConnectionState.Connected) {
            Log.w(TAG, "requestUpload: not connected, aborting")
            _events.trySend(BridgeUiEvent.UploadFailed(fileName, "NOT_CONNECTED"))
            return
        }
        addTransfer(TransferType.UPLOAD, fileName, data.size.toLong())
        bridgeClient.requestUpload(fileName, data, overwrite)
    }

    /**
     * Convert a loaded blueprint into [target] format. Delegates to
     * [BlueprintManager.convert] (suspend, IO). Emits a
     * [BridgeUiEvent.ConvertSucceeded] or [BridgeUiEvent.ConvertFailed]
     * via the existing one-shot events channel so the activity can show
     * a Snackbar.
     *
     * The file name passed to the event is the *source* blueprint's
     * display name so the user can identify which file was converted
     * (the new file has a `_converted` suffix and may not be visible
     * to them by the time the snackbar shows).
     *
     * The [targetExtension] is the literal file extension to use for the
     * output (e.g. "schem" or "schematic" — both are Sponge format on
     * the blockprint-core side but differ in user-facing extension).
     */
    fun convertBlueprint(uuid: String, target: io.github.moxisuki.blockprint.core.SchematicFormat, targetExtension: String) {
        val sourceDisplayName = blueprintManager.blueprints.value
            .firstOrNull { it.uuid == uuid }
            ?.displayName
            ?: "?"
        _convertInFlight.value = true
        viewModelScope.launch {
            val result = blueprintManager.convert(uuid, target, targetExtension)
            result.onSuccess { meta ->
                _events.trySend(BridgeUiEvent.ConvertSucceeded(meta.displayName))
            }.onFailure { e ->
                Log.w(TAG, "convertBlueprint: failed", e)
                val code = when (e) {
                    is io.github.moxisuki.blockprint.core.exceptions.LitematicException -> e.message ?: "LITEMATIC_ERROR"
                    is IllegalStateException -> e.message ?: "ILLEGAL_STATE"
                    else -> "IO_ERROR"
                }
                _events.trySend(BridgeUiEvent.ConvertFailed(sourceDisplayName, code))
            }
            _convertInFlight.value = false
        }
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

    private suspend fun handleBridgeEvent(event: BridgeEvent) {
        when (event) {
            is BridgeEvent.Connected -> onConnected(event.session, event.entries)
            is BridgeEvent.ListChanged -> onListChanged(event.session, event.entries)
            is BridgeEvent.DownloadStart -> onDownloadStart(event.fileName, event.size, event.sha256)
            is BridgeEvent.DownloadComplete -> onDownloadComplete(event.fileName, event.data)
            is BridgeEvent.DownloadProgress -> {
                val list = _transfers.value
                val item = list.find { it.type == TransferType.DOWNLOAD && it.fileName == event.fileName }
                if (item != null) updateTransfer(item.id, event.bytes)
            }
            is BridgeEvent.UploadResult -> {
                Log.d(TAG, "onUploadResult: $event")
                val item = _transfers.value.find { it.type == TransferType.UPLOAD && it.fileName == event.fileName }
                if (item != null) {
                    setPhase(item.id, if (event.ok) TransferPhase.DONE else TransferPhase.FAILED)
                    kotlinx.coroutines.delay(2000)
                    removeTransfer(item.id)
                }
                if (event.ok) {
                    _events.trySend(BridgeUiEvent.UploadSucceeded(event.fileName))
                } else {
                    _events.trySend(BridgeUiEvent.UploadFailed(event.fileName, event.errorCode ?: getApplication<android.app.Application>().getString(R.string.bridge_error_unknown)))
                }
            }
            is BridgeEvent.UploadProgress -> {
                val item = _transfers.value.find { it.type == TransferType.UPLOAD && it.fileName == event.fileName }
                if (item != null) updateTransfer(item.id, event.bytes)
            }
            is BridgeEvent.Error -> onError(event.code, event.message)
            is BridgeEvent.Disconnected -> onDisconnected()
        }
    }

    private suspend fun onConnected(
        session: SessionInfo,
        entries: List<io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint>,
    ) {
        val current = _connectionState.value
        if (current !is ConnectionState.Connecting) {
            Log.d(TAG, "onConnected: not in Connecting state, ignored")
            return
        }
        if (current.host.isBlank() || current.port <= 0) {
            Log.w(TAG, "onConnected: rejecting invalid host/port: '${current.host}':${current.port}")
            return
        }
        Log.d(TAG, "onConnected: ${entries.size} entries, ${session.folderName}")
        wasConnected = true
        _connectionState.value = ConnectionState.Connected(current.host, current.port, session, entries)

        val folder = session.folderName.ifBlank { current.host }
        val existing = pairedDeviceDao.find(current.host, current.port, folder)
        val label = existing?.label ?: folder
        pairedDeviceDao.upsert(
            PairedDeviceEntity(
                host = current.host,
                wsPort = current.port,
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
        Log.i(TAG, "connected to ${current.host}:${current.port} ($label)")
    }

    private suspend fun onListChanged(
        session: SessionInfo,
        entries: List<io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint>,
    ) {
        Log.d(TAG, "onListChanged: ${entries.size} entries")
        val current = _connectionState.value
        if (current is ConnectionState.Connected) {
            _connectionState.value = current.copy(entries = entries)
        }
    }

    private var currentDownloadId: Long = 0

    private fun onDownloadStart(fileName: String, size: Long, sha256: String) {
        _transfers.update { list ->
            list.map { if (it.type == TransferType.DOWNLOAD && it.fileName == fileName) it.copy(totalBytes = size) else it }
        }
        currentDownloadId = _transfers.value.find { it.type == TransferType.DOWNLOAD && it.fileName == fileName }?.id ?: 0
        _events.trySend(BridgeUiEvent.DownloadStart(fileName))
    }

    private suspend fun onDownloadComplete(fileName: String, bytes: ByteArray) {
        Log.d(TAG, "onDownloadComplete($fileName, ${bytes.size} bytes)")
        // Find transfer by fileName — robust even if download/start is never sent
        val downloadId = _transfers.value.find {
            it.type == TransferType.DOWNLOAD && it.fileName == fileName
        }?.id ?: currentDownloadId
        // Update totalBytes from actual data size so progress bar is determinate
        if (downloadId > 0) {
            _transfers.update { list ->
                list.map { if (it.id == downloadId) it.copy(totalBytes = bytes.size.toLong(), receivedBytes = 0L) else it }
            }
        }
        var downloadedOk = false
        try {
            val meta = blueprintManager.ingest(fileName, bytes) { written, total ->
                if (downloadId > 0) updateTransfer(downloadId, written)
            }
            Log.i(TAG, "PC download OK: $fileName → ${meta.uuid}")
            _events.trySend(BridgeUiEvent.DownloadComplete(fileName, meta.uuid))
            downloadedOk = true
        } catch (e: Exception) {
            Log.e(TAG, "PC download ingest FAILED: $fileName (${bytes.size}B) — ${e.message}", e)
            val app = getApplication<android.app.Application>()
            val msg = when {
                e.message?.contains("SAF", ignoreCase = true) == true -> app.getString(R.string.bridge_error_saf_not_configured)
                else -> e.message ?: app.getString(R.string.bridge_error_import_failed)
            }
            _events.trySend(BridgeUiEvent.DownloadFailed(fileName, msg))
        } finally {
            if (downloadId > 0) {
                setPhase(downloadId, if (downloadedOk) TransferPhase.DONE else TransferPhase.FAILED)
                kotlinx.coroutines.delay(2000)
                removeTransfer(downloadId)
            }
        }
    }

    private suspend fun onError(code: String, message: String?) {
        Log.d(TAG, "onError($code, $message)")
        clearTransfers()
        val current = _connectionState.value
        if (code == "AUTH_FAILED") {
            wasConnected = false
            val host = (current as? ConnectionState.Connected)?.host
                ?: (current as? ConnectionState.Connecting)?.host
                ?: (current as? ConnectionState.Error)?.lastHost
                ?: ""
            val port = (current as? ConnectionState.Connected)?.port
                ?: (current as? ConnectionState.Connecting)?.port
                ?: (current as? ConnectionState.Error)?.lastPort
                ?: 0
            _connectionState.value = ConnectionState.Error(
                lastHost = host.ifBlank { null },
                lastPort = if (port > 0) port else null,
                message = getApplication<android.app.Application>().getString(R.string.bridge_error_token),
            )
            bridgeClient.disconnect()
            _events.trySend(BridgeUiEvent.AuthFailed(getApplication<android.app.Application>().getString(R.string.bridge_error_token)))
        } else if (current is ConnectionState.Connecting) {
            _connectionState.value = ConnectionState.Error(
                lastHost = current.host,
                lastPort = current.port,
                message = message ?: code,
            )
        } else {
            Log.w(TAG, "onError during $current: $code $message")
        }
    }

    private fun onDisconnected() {
        Log.d(TAG, "onDisconnected")
        clearTransfers()
        val current = _connectionState.value
        val wasConn = current is ConnectionState.Connected
        val host = (current as? ConnectionState.Connected)?.host
            ?: (current as? ConnectionState.Connecting)?.host
        val port = (current as? ConnectionState.Connected)?.port
            ?: (current as? ConnectionState.Connecting)?.port
        _connectionState.value = ConnectionState.Disconnected(
            lastHost = host,
            lastPort = port,
        )
        wasConnected = false
        val inesperado = wasConn
        _events.trySend(BridgeUiEvent.Disconnected(inesperado))
        if (inesperado) {
            Log.w(TAG, "unexpected disconnect host=$host port=$port")
        }
    }
}

private fun PairedDeviceEntity.sessionInfoOrNull(): SessionInfo? {
    val mc = mcVersion ?: return null
    val ld = loader ?: return null
    val lv = loaderVersion ?: return null
    val fn = folderName ?: return null
    return SessionInfo(mcVersion = mc, loader = ld, loaderVersion = lv, folderName = fn)
}

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
