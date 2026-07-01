package io.github.moxisuki.blockprint.cat.ui.bridge

import android.content.Context
import android.util.Log
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeClient
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeEvent
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "BridgeTransferCtrl"

private const val TRANSFER_FINALIZE_DELAY_MS = 2_000L

/**
 * Owns the upload / download / convert concerns of the PC bridge.
 *
 * One half of the BridgeViewModel split — the other half is
 * [BridgeSessionController]. Both are constructed and held by
 * [BridgeViewModel] which exposes their flows through a single facade
 * API to avoid touching any of the seven downstream consumers.
 *
 * Responsibilities:
 *  - [transfers] in-memory list of in-flight transfers (drives the home-tab progress UI)
 *  - [isTaskInFlight] mutex — at most one transfer with phase RUNNING; concurrent
 *    `requestDownload` / `requestUpload` calls fail-fast with `BUSY_LOCAL`
 *  - [convertBlueprint] which delegates to [BlueprintManager.convert]
 *  - upload/download event handling from [BridgeClient.eventFlow]
 *
 * Connection / scan / pairing concerns are handled by [BridgeSessionController];
 * this controller only **reads** `session.connectionState` to gate transfers.
 *
 * Not @Inject-annotated: holds bare `Context` and `CoroutineScope` which
 * Hilt cannot resolve without extra modules. [BridgeViewModel] builds this
 * controller in its `init` block.
 */
class BridgeTransferController(
    private val context: Context,
    private val bridgeClient: BridgeClient,
    private val blueprintManager: BlueprintManager,
    private val session: BridgeSessionController,
    private val eventBus: BridgeEventBus,
    private val scope: CoroutineScope,
) {
    private val _transfers = MutableStateFlow<List<TransferItem>>(emptyList())
    val transfers: StateFlow<List<TransferItem>> = _transfers.asStateFlow()

    val isTaskInFlight: StateFlow<Boolean> = _transfers
        .map { isAnyTransferInFlight(it) }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), false)

    private val _convertInFlight = MutableStateFlow(false)
    val convertInFlight: StateFlow<Boolean> = _convertInFlight.asStateFlow()

    private var transferSeq: Long = 0
    private var currentDownloadId: Long = 0

    init {
        // Subscribe only to transfer-relevant events. Session events
        // (Connected / Disconnected / Error / ListChanged) are handled by
        // [BridgeSessionController].
        scope.launch {
            bridgeClient.eventFlow.collect { event ->
                when (event) {
                    is BridgeEvent.DownloadStart -> onDownloadStart(event.fileName, event.size)
                    is BridgeEvent.DownloadComplete -> onDownloadComplete(event.fileName, event.data)
                    is BridgeEvent.DownloadProgress -> onDownloadProgress(event.fileName, event.bytes)
                    is BridgeEvent.UploadProgress -> onUploadProgress(event.fileName, event.bytes)
                    is BridgeEvent.UploadResult -> onUploadResult(event.fileName, event.ok, event.error)
                    else -> Unit
                }
            }
        }
    }

    fun requestDownload(fileName: String, source: String = "schematics") {
        Log.d(TAG, "requestDownload($fileName, source=$source)")
        if (session.connectionState.value !is ConnectionState.Connected) return
        if (isTaskInFlight.value) {
            Log.w(TAG, "requestDownload: BUSY_LOCAL, ignoring $fileName")
            eventBus.emit(BridgeUiEvent.DownloadFailed(fileName, "BUSY_LOCAL"))
            return
        }
        addTransfer(TransferType.DOWNLOAD, fileName, 0L)
        currentDownloadId = transferSeq
        bridgeClient.requestDownload(fileName, source)
    }

    fun requestUpload(fileName: String, data: ByteArray, overwrite: Boolean = false) {
        Log.d(TAG, "requestUpload($fileName, ${data.size}B, overwrite=$overwrite)")
        if (session.connectionState.value !is ConnectionState.Connected) {
            Log.w(TAG, "requestUpload: not connected, aborting")
            eventBus.emit(BridgeUiEvent.UploadFailed(fileName, "NOT_CONNECTED"))
            return
        }
        if (isTaskInFlight.value) {
            Log.w(TAG, "requestUpload: BUSY_LOCAL, ignoring $fileName")
            eventBus.emit(BridgeUiEvent.UploadFailed(fileName, "BUSY_LOCAL"))
            return
        }
        addTransfer(TransferType.UPLOAD, fileName, data.size.toLong())
        bridgeClient.requestUpload(fileName, data, overwrite)
    }

    /**
     * Convert a loaded blueprint into [target] format. Delegates to
     * [BlueprintManager.convert] (suspend, IO). Emits a
     * [BridgeUiEvent.ConvertSucceeded] or [BridgeUiEvent.ConvertFailed]
     * via [eventBus].
     *
     * The file name passed to the event is the *source* blueprint's
     * display name so the user can identify which file was converted.
     *
     * The [targetExtension] is the literal file extension to use for the
     * output (e.g. "schem" or "schematic" — both are Sponge format on
     * the blockprint-core side but differ in user-facing extension).
     */
    fun convertBlueprint(
        uuid: String,
        target: io.github.moxisuki.blockprint.core.SchematicFormat,
        targetExtension: String,
    ) {
        val sourceDisplayName = blueprintManager.blueprints.value
            .firstOrNull { it.uuid == uuid }
            ?.displayName
            ?: "?"
        _convertInFlight.value = true
        scope.launch {
            val result = blueprintManager.convert(uuid, target, targetExtension)
            result.onSuccess { meta ->
                eventBus.emit(BridgeUiEvent.ConvertSucceeded(meta.displayName))
            }.onFailure { e ->
                Log.w(TAG, "convertBlueprint: failed", e)
                val code = when (e) {
                    is io.github.moxisuki.blockprint.core.exceptions.LitematicException ->
                        e.message ?: "LITEMATIC_ERROR"
                    is IllegalStateException -> e.message ?: "ILLEGAL_STATE"
                    else -> "IO_ERROR"
                }
                eventBus.emit(BridgeUiEvent.ConvertFailed(sourceDisplayName, code))
            }
            _convertInFlight.value = false
        }
    }

    private fun addTransfer(type: TransferType, fileName: String, totalBytes: Long) {
        val item = TransferItem(
            id = ++transferSeq,
            type = type,
            fileName = fileName,
            totalBytes = totalBytes,
        )
        _transfers.update { it + item }
    }

    private fun updateTransfer(id: Long, receivedBytes: Long) {
        _transfers.update { list ->
            list.map {
                if (it.id == id && it.phase == TransferPhase.RUNNING)
                    it.copy(receivedBytes = receivedBytes)
                else it
            }
        }
    }

    private fun setPhase(id: Long, phase: TransferPhase) {
        _transfers.update { list ->
            list.map {
                if (it.id == id)
                    it.copy(
                        phase = phase,
                        receivedBytes = if (phase != TransferPhase.RUNNING) it.totalBytes else it.receivedBytes,
                    )
                else it
            }
        }
    }

    private fun removeTransfer(id: Long) {
        _transfers.update { list -> list.filter { it.id != id } }
    }

    private fun clearTransfers() {
        _transfers.value = emptyList()
    }

    /**
     * Called by [BridgeSessionController] when the session drops, so
     * transient transfer rows don't outlive the session.
     */
    fun clearAllOnDisconnect() = clearTransfers()

    private fun onDownloadStart(fileName: String, size: Long) {
        _transfers.update { list ->
            list.map {
                if (it.type == TransferType.DOWNLOAD && it.fileName == fileName)
                    it.copy(totalBytes = size)
                else it
            }
        }
        currentDownloadId = _transfers.value
            .find { it.type == TransferType.DOWNLOAD && it.fileName == fileName }
            ?.id ?: 0
        eventBus.emit(BridgeUiEvent.DownloadStart(fileName))
    }

    private fun onDownloadProgress(fileName: String, bytes: Long) {
        val list = _transfers.value
        val item = list.find { it.type == TransferType.DOWNLOAD && it.fileName == fileName }
        if (item != null) updateTransfer(item.id, bytes)
    }

    private suspend fun onDownloadComplete(fileName: String, bytes: ByteArray) {
        Log.d(TAG, "onDownloadComplete($fileName, ${bytes.size} bytes)")
        val downloadId = _transfers.value.find {
            it.type == TransferType.DOWNLOAD && it.fileName == fileName
        }?.id ?: currentDownloadId

        if (downloadId > 0) {
            _transfers.update { list ->
                list.map {
                    if (it.id == downloadId) it.copy(totalBytes = bytes.size.toLong(), receivedBytes = 0L)
                    else it
                }
            }
        }

        var downloadedOk = false
        try {
            val meta = blueprintManager.ingest(fileName, bytes) { written, _ ->
                if (downloadId > 0) updateTransfer(downloadId, written)
            }
            Log.i(TAG, "PC download OK: $fileName → ${meta.uuid}")
            eventBus.emit(BridgeUiEvent.DownloadComplete(fileName, meta.uuid))
            downloadedOk = true
        } catch (e: Exception) {
            Log.e(TAG, "PC download ingest FAILED: $fileName (${bytes.size}B) — ${e.message}", e)
            val msg = when {
                e.message?.contains("SAF", ignoreCase = true) == true ->
                    context.getString(R.string.bridge_error_saf_not_configured)
                else -> e.message ?: context.getString(R.string.bridge_error_import_failed)
            }
            eventBus.emit(BridgeUiEvent.DownloadFailed(fileName, msg))
        } finally {
            if (downloadId > 0) {
                setPhase(downloadId, if (downloadedOk) TransferPhase.DONE else TransferPhase.FAILED)
                delay(TRANSFER_FINALIZE_DELAY_MS)
                removeTransfer(downloadId)
            }
        }
    }

    private fun onUploadProgress(fileName: String, bytes: Long) {
        val item = _transfers.value.find { it.type == TransferType.UPLOAD && it.fileName == fileName }
        if (item != null) updateTransfer(item.id, bytes)
    }

    private suspend fun onUploadResult(fileName: String, ok: Boolean, errorCode: String?) {
        Log.d(TAG, "onUploadResult: ok=$ok error=$errorCode")
        val item = _transfers.value.find { it.type == TransferType.UPLOAD && it.fileName == fileName }
        if (item != null) {
            setPhase(item.id, if (ok) TransferPhase.DONE else TransferPhase.FAILED)
            delay(TRANSFER_FINALIZE_DELAY_MS)
            removeTransfer(item.id)
        }
        if (ok) {
            eventBus.emit(BridgeUiEvent.UploadSucceeded(fileName))
        } else {
            eventBus.emit(
                BridgeUiEvent.UploadFailed(
                    fileName,
                    errorCode ?: context.getString(R.string.bridge_error_unknown),
                )
            )
        }
    }
}
