package io.github.moxisuki.blockprint.cat.data.bridge

import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionState

/**
 * Pure transition function for the bridge [ConnectionState] state machine.
 *
 * Extracted from `BridgeSessionController` to keep all I/O effects out of
 * the transition rules so they can be unit tested without a Hilt graph
 * (see `SessionTransitionsTest`).
 *
 * Convention: each `nextOn*` returns the *new* state when the transition
 * is valid, or `null` when the event should be ignored (e.g. `Connected`
 * arriving while we are already `Disconnected`).
 *
 * The state diagram enforced here is:
 *
 *   Disconnected --connect()--> Connecting --onConnected--> Connected
 *                                          |
 *                                          |--onError / timeout--> Error
 *                                          |
 *   Connected --onError(AUTH_FAILED)--> Error
 *   Connected --onDisconnected------------> Disconnected (unexpected=true)
 *   Connecting --onDisconnected-----------> Disconnected (unexpected=false)
 *   Error --clearError()------------------> Disconnected
 */
object SessionTransitions {

    /**
     * Transition triggered by `BridgeEvent.Connected`.
     *
     * Valid only when [current] is `Connecting` with a non-blank host and
     * positive port. Returns the new `Connected` state preserving the
     * host/port from `Connecting` and adopting the new session info.
     */
    fun nextOnConnected(
        current: ConnectionState,
        session: SessionInfo,
        entries: List<RemoteBlueprint>,
    ): ConnectionState.Connected? {
        if (current !is ConnectionState.Connecting) return null
        if (current.host.isBlank() || current.port <= 0) return null
        return ConnectionState.Connected(
            host = current.host,
            port = current.port,
            session = session,
            entries = entries,
        )
    }

    /**
     * Transition triggered by `BridgeEvent.ListChanged`.
     *
     * Valid only when [current] is already `Connected`. Returns a copy
     * with refreshed session + entries; returns `null` if we lost the
     * connection before the event arrived.
     */
    fun nextOnListChanged(
        current: ConnectionState,
        session: SessionInfo,
        entries: List<RemoteBlueprint>,
    ): ConnectionState? {
        return if (current is ConnectionState.Connected)
            current.copy(session = session, entries = entries)
        else null
    }

    /**
     * Transition triggered by `BridgeEvent.Error`.
     *
     * - `AUTH_FAILED` is always promoted to `Error` (with [tokenErrorMessage]
     *   human-readable string), even from `Connected`. Disconnect is the
     *   caller's job — same as in the original VM.
     * - Any other code while `Connecting` -> `Error` with [message] fallback
     *   to [code].
     * - All other `(state, code)` combinations are ignored (returns null).
     */
    fun nextOnError(
        current: ConnectionState,
        code: String,
        message: String?,
        tokenErrorMessage: String,
    ): ConnectionState.Error? {
        if (code == "AUTH_FAILED") {
            val host = (current as? ConnectionState.Connected)?.host
                ?: (current as? ConnectionState.Connecting)?.host
                ?: (current as? ConnectionState.Error)?.lastHost
                ?: ""
            val port = (current as? ConnectionState.Connected)?.port
                ?: (current as? ConnectionState.Connecting)?.port
                ?: (current as? ConnectionState.Error)?.lastPort
                ?: 0
            return ConnectionState.Error(
                lastHost = host.ifBlank { null },
                lastPort = if (port > 0) port else null,
                message = tokenErrorMessage,
            )
        }
        if (current is ConnectionState.Connecting) {
            return ConnectionState.Error(
                lastHost = current.host,
                lastPort = current.port,
                message = message ?: code,
            )
        }
        return null
    }

    /**
     * Transition triggered by `BridgeEvent.Disconnected`.
     *
     * Returns `null` if we weren't in `Connected` or `Connecting`
     * (i.e. nothing meaningful to mark as Disconnected again).
     *
     * The boolean in the returned pair is `true` when the disconnect
     * was unexpected (we were previously Connected), `false` when it
     * happened mid-Connecting (e.g. timeout, server reject).
     */
    fun nextOnDisconnected(
        current: ConnectionState,
    ): Pair<ConnectionState.Disconnected, Boolean>? {
        val wasConnected = current is ConnectionState.Connected
        val host = (current as? ConnectionState.Connected)?.host
            ?: (current as? ConnectionState.Connecting)?.host
        val port = (current as? ConnectionState.Connected)?.port
            ?: (current as? ConnectionState.Connecting)?.port
        return if (host != null && port != null) {
            ConnectionState.Disconnected(lastHost = host, lastPort = port) to wasConnected
        } else null
    }

    /**
     * Transition triggered by `clearError()` — only valid from `Error`.
     */
    fun nextOnClearError(
        current: ConnectionState,
    ): ConnectionState.Disconnected? {
        return if (current is ConnectionState.Error) {
            ConnectionState.Disconnected(
                lastHost = current.lastHost,
                lastPort = current.lastPort,
            )
        } else null
    }
}
