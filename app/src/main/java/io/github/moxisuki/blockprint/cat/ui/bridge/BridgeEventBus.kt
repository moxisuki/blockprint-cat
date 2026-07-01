package io.github.moxisuki.blockprint.cat.ui.bridge

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared one-shot event sink for bridge UI events.
 *
 * Replaces the per-VM Channel that used to live inside BridgeViewModel.
 * Both [BridgeSessionController] and [BridgeTransferController] emit
 * here, and the Activity observes [events] (replacing `bridgeVm.events`)
 * to drive snackbars and navigation.
 *
 * Buffered so an emitter that runs ahead of a slow collector (e.g. a
 * screen rotation that suspends collection momentarily) does not block
 * — old events are dropped with a warning rather than crashing the bus.
 */
@Singleton
class BridgeEventBus @Inject constructor() {
    private val _events = Channel<BridgeUiEvent>(
        capacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<BridgeUiEvent> = _events.receiveAsFlow()
    fun emit(event: BridgeUiEvent) { _events.trySend(event) }
}
