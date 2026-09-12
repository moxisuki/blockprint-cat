package io.github.moxisuki.blockprint.cat.app.core.pcbridge

import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class PcBridgeRepository @Inject constructor(
    private val client: PcBridgeClient,
    private val discovery: PcBridgeDiscovery,
    private val blueprintRepository: BlueprintRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(PcBridgeState())
    val state: StateFlow<PcBridgeState> = _state.asStateFlow()

    private var discoveryJob: Job? = null

    init {
        scope.launch {
            client.events.collect(::handleClientEvent)
        }
    }

    fun startDiscovery() {
        if (discoveryJob?.isActive == true) return
        _state.update { it.copy(discoveryActive = true) }
        discoveryJob = scope.launch {
            discovery.observe().collect { device ->
                _state.update { state ->
                    state.copy(
                        discoveredDevices = (state.discoveredDevices.filterNot { it.id == device.id } + device)
                            .filter { System.currentTimeMillis() - it.lastSeenAtMillis < DEVICE_STALE_MS }
                            .sortedByDescending { it.lastSeenAtMillis },
                    )
                }
            }
        }
        discoveryJob?.invokeOnCompletion {
            _state.update { it.copy(discoveryActive = false) }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _state.update { it.copy(discoveryActive = false) }
    }

    fun connect(host: String, port: Int, token: String) {
        val cleanHost = host.trim()
        val cleanToken = token.trim()
        if (cleanHost.isBlank() || cleanToken.isBlank() || port <= 0) {
            _state.update {
                it.copy(
                    connection = PcBridgeConnection.Failed(
                        host = cleanHost,
                        port = port,
                        code = "BAD_REQUEST",
                        message = "Invalid host, port or token",
                    ),
                    lastError = PcBridgeError("BAD_REQUEST", "Invalid host, port or token"),
                )
            }
            return
        }
        _state.update {
            it.copy(
                connection = PcBridgeConnection.Connecting(cleanHost, port),
                lastError = null,
            )
        }
        client.connect(cleanHost, port, cleanToken)
    }

    fun disconnect() {
        client.disconnect()
        _state.update {
            it.copy(
                connection = PcBridgeConnection.Disconnected,
                session = null,
                blueprints = emptyList(),
                tasks = emptyList(),
                lastError = null,
            )
        }
    }

    fun refreshList() {
        if (!client.isOpen) {
            _state.update { it.copy(lastError = PcBridgeError("NOT_CONNECTED", "Bridge is not connected")) }
            return
        }
        client.requestList()
    }

    fun download(blueprint: PcRemoteBlueprint) {
        if (!client.isOpen) {
            _state.update { it.copy(lastError = PcBridgeError("NOT_CONNECTED", "Bridge is not connected")) }
            return
        }
        client.requestDownload(blueprint.id)
    }

    fun cancelTask(taskId: String) {
        client.cancelTask(taskId)
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    private fun handleClientEvent(event: PcBridgeEvent) {
        when (event) {
            is PcBridgeEvent.Opened -> {
                _state.update {
                    it.copy(
                        connection = PcBridgeConnection.Connected(event.host, event.port),
                        lastError = null,
                    )
                }
            }
            is PcBridgeEvent.SessionReady -> {
                _state.update {
                    it.copy(
                        session = event.session,
                        lastError = null,
                    )
                }
            }
            is PcBridgeEvent.ListReceived -> {
                _state.update {
                    it.copy(
                        blueprints = event.entries.sortedRemoteBlueprints(),
                        lastError = null,
                    )
                }
            }
            is PcBridgeEvent.ListChanged -> {
                _state.update {
                    it.copy(
                        blueprints = event.entries.sortedRemoteBlueprints(),
                    )
                }
            }
            is PcBridgeEvent.TaskChanged -> {
                _state.update { state ->
                    state.copy(
                        tasks = state.tasks.upsertTask(event.task).trimFinishedTasks(),
                        lastError = if (event.task.status == PcTaskStatus.Failed) {
                            PcBridgeError(
                                event.task.errorCode ?: "TASK_FAILED",
                                event.task.errorMessage ?: event.task.fileName,
                            )
                        } else {
                            state.lastError
                        },
                    )
                }
            }
            is PcBridgeEvent.DownloadComplete -> {
                scope.launch {
                    _state.update { state ->
                        state.copy(
                            tasks = state.tasks.updateTask(event.taskId) { task ->
                                task.copy(
                                    phase = PcTaskPhase.Importing,
                                    status = PcTaskStatus.Running,
                                    bytesDone = task.bytesTotal,
                                    progress = 0.95f,
                                )
                            },
                        )
                    }
                    runCatching {
                        blueprintRepository.importDownloadedBlueprint(event.fileName, event.bytes)
                    }.onSuccess {
                        _state.update { state ->
                            state.copy(
                                tasks = state.tasks.updateTask(event.taskId) { task ->
                                    task.copy(
                                        phase = PcTaskPhase.Done,
                                        status = PcTaskStatus.Done,
                                        progress = 1f,
                                    )
                                },
                                lastError = null,
                            )
                        }
                        delay(DONE_VISIBLE_MS)
                        _state.update { state ->
                            state.copy(
                                tasks = state.tasks.filterNot {
                                    it.taskId == event.taskId && it.status == PcTaskStatus.Done
                                },
                            )
                        }
                    }.onFailure { error ->
                        val message = error.message.orEmpty().ifBlank { error::class.java.simpleName }
                        _state.update { state ->
                            state.copy(
                                tasks = state.tasks.updateTask(event.taskId) { task ->
                                    task.copy(
                                        phase = PcTaskPhase.Failed,
                                        status = PcTaskStatus.Failed,
                                        errorCode = "IMPORT_FAILED",
                                        errorMessage = message,
                                    )
                                },
                                lastError = PcBridgeError("IMPORT_FAILED", message),
                            )
                        }
                    }
                }
            }
            is PcBridgeEvent.Error -> {
                _state.update { state ->
                    val failedConnection = if (
                        event.code == "AUTH_FAILED" ||
                        event.code == "CONNECT_FAILED" ||
                        state.connection is PcBridgeConnection.Connecting
                    ) {
                        val current = state.connection
                        val host = when (current) {
                            is PcBridgeConnection.Connecting -> current.host
                            is PcBridgeConnection.Connected -> current.host
                            is PcBridgeConnection.Failed -> current.host
                            PcBridgeConnection.Disconnected -> ""
                        }
                        val port = when (current) {
                            is PcBridgeConnection.Connecting -> current.port
                            is PcBridgeConnection.Connected -> current.port
                            is PcBridgeConnection.Failed -> current.port
                            PcBridgeConnection.Disconnected -> PcBridgeDefaultPort
                        }
                        PcBridgeConnection.Failed(host, port, event.code, event.message)
                    } else {
                        state.connection
                    }
                    state.copy(
                        connection = failedConnection,
                        lastError = PcBridgeError(event.code, event.message),
                    )
                }
            }
            PcBridgeEvent.Closed -> {
                _state.update { state ->
                    state.copy(
                        connection = if (state.connection is PcBridgeConnection.Failed) {
                            state.connection
                        } else {
                            PcBridgeConnection.Disconnected
                        },
                        session = null,
                        blueprints = emptyList(),
                        tasks = state.tasks.map { task ->
                            if (task.active) {
                                task.copy(
                                    phase = PcTaskPhase.Failed,
                                    status = PcTaskStatus.Failed,
                                    errorCode = "DISCONNECTED",
                                    errorMessage = "Bridge disconnected",
                                )
                            } else {
                                task
                            }
                        },
                    )
                }
            }
        }
    }

    private companion object {
        const val DEVICE_STALE_MS = 12_000L
        const val DONE_VISIBLE_MS = 1_500L
    }
}

private fun List<PcRemoteBlueprint>.sortedRemoteBlueprints(): List<PcRemoteBlueprint> =
    sortedWith(
        compareBy<PcRemoteBlueprint> { it.source }
            .thenBy { it.displayName.lowercase() }
            .thenBy { it.fileName.lowercase() },
    )

private fun List<PcTransferTask>.upsertTask(task: PcTransferTask): List<PcTransferTask> {
    if (task.taskId.isBlank()) return this
    val index = indexOfFirst { it.taskId == task.taskId }
    return if (index < 0) {
        listOf(task) + this
    } else {
        toMutableList().apply { set(index, mergeTask(get(index), task)) }
    }
}

private fun List<PcTransferTask>.updateTask(
    taskId: String,
    transform: (PcTransferTask) -> PcTransferTask,
): List<PcTransferTask> =
    map { task -> if (task.taskId == taskId) transform(task) else task }

private fun List<PcTransferTask>.trimFinishedTasks(): List<PcTransferTask> =
    take(16)

private fun mergeTask(old: PcTransferTask, new: PcTransferTask): PcTransferTask =
    new.copy(
        transferId = new.transferId ?: old.transferId,
        blueprintId = new.blueprintId ?: old.blueprintId,
        fileName = new.fileName.ifBlank { old.fileName },
        bytesTotal = if (new.bytesTotal > 0L) new.bytesTotal else old.bytesTotal,
    )
