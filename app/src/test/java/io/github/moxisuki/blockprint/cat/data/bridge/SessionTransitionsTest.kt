package io.github.moxisuki.blockprint.cat.data.bridge

import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTransitionsTest {

    private val session = SessionInfo(
        mcVersion = "1.20.4",
        loader = "fabric",
        loaderVersion = "0.15.0",
        folderName = "survival",
    )

    // ---------- nextOnConnected ----------

    @Test
    fun connecting_with_valid_host_port_transitions_to_connected() {
        val current = ConnectionState.Connecting(host = "10.0.0.1", port = 8080)
        val next = SessionTransitions.nextOnConnected(current, session, emptyList())
        assertNotNull(next)
        assertEquals("10.0.0.1", next!!.host)
        assertEquals(8080, next.port)
        assertEquals(session, next.session)
        assertEquals(emptyList<RemoteBlueprint>(), next.entries)
    }

    @Test
    fun connected_event_in_disconnected_state_is_ignored() {
        val current = ConnectionState.Disconnected(lastHost = "10.0.0.1", lastPort = 8080)
        assertNull(SessionTransitions.nextOnConnected(current, session, emptyList()))
    }

    @Test
    fun connected_event_in_error_state_is_ignored() {
        val current = ConnectionState.Error(message = "boom")
        assertNull(SessionTransitions.nextOnConnected(current, session, emptyList()))
    }

    @Test
    fun connecting_with_blank_host_returns_null() {
        val current = ConnectionState.Connecting(host = "  ", port = 8080)
        assertNull(SessionTransitions.nextOnConnected(current, session, emptyList()))
    }

    @Test
    fun connecting_with_zero_port_returns_null() {
        val current = ConnectionState.Connecting(host = "10.0.0.1", port = 0)
        assertNull(SessionTransitions.nextOnConnected(current, session, emptyList()))
    }

    @Test
    fun connecting_with_negative_port_returns_null() {
        val current = ConnectionState.Connecting(host = "10.0.0.1", port = -1)
        assertNull(SessionTransitions.nextOnConnected(current, session, emptyList()))
    }

    // ---------- nextOnListChanged ----------

    @Test
    fun list_changed_in_connected_state_updates_entries() {
        val original = ConnectionState.Connected("h", 1, session, emptyList())
        val next = SessionTransitions.nextOnListChanged(original, session, emptyList())
        assertNotNull(next)
        assertEquals(original.copy(), next)
    }

    @Test
    fun list_changed_in_non_connected_state_is_ignored() {
        val current = ConnectionState.Connecting("h", 1)
        assertNull(SessionTransitions.nextOnListChanged(current, session, emptyList()))
    }

    // ---------- nextOnError ----------

    @Test
    fun auth_failed_in_connecting_emits_error_with_token_message() {
        val current = ConnectionState.Connecting("h", 1)
        val next = SessionTransitions.nextOnError(
            current = current,
            code = "AUTH_FAILED",
            message = "irrelevant",
            tokenErrorMessage = "Token 错误",
        )
        assertNotNull(next)
        assertEquals("h", next!!.lastHost)
        assertEquals(1, next.lastPort)
        assertEquals("Token 错误", next.message)
    }

    @Test
    fun auth_failed_in_connected_also_emits_error() {
        val current = ConnectionState.Connected("h", 1, session, emptyList())
        val next = SessionTransitions.nextOnError(
            current = current,
            code = "AUTH_FAILED",
            message = null,
            tokenErrorMessage = "Token 错误",
        )
        assertNotNull(next)
        assertEquals("h", next!!.lastHost)
        assertEquals(1, next.lastPort)
        assertEquals("Token 错误", next.message)
    }

    @Test
    fun generic_error_in_connecting_uses_message_then_code() {
        val current = ConnectionState.Connecting("h", 1)
        val next = SessionTransitions.nextOnError(
            current = current,
            code = "CONNECT_FAILED",
            message = "Connection refused",
            tokenErrorMessage = "ignored",
        )
        assertNotNull(next)
        assertEquals("Connection refused", next!!.message)
    }

    @Test
    fun generic_error_in_connecting_without_message_falls_back_to_code() {
        val current = ConnectionState.Connecting("h", 1)
        val next = SessionTransitions.nextOnError(
            current = current,
            code = "CONNECT_FAILED",
            message = null,
            tokenErrorMessage = "ignored",
        )
        assertNotNull(next)
        assertEquals("CONNECT_FAILED", next!!.message)
    }

    @Test
    fun generic_error_in_disconnected_is_ignored() {
        val current = ConnectionState.Disconnected()
        assertNull(
            SessionTransitions.nextOnError(
                current = current,
                code = "CONNECT_FAILED",
                message = "boom",
                tokenErrorMessage = "ignored",
            )
        )
    }

    // ---------- nextOnDisconnected ----------

    @Test
    fun disconnected_in_connected_marks_unexpected_true() {
        val current = ConnectionState.Connected("h", 1, session, emptyList())
        val next = SessionTransitions.nextOnDisconnected(current)
        assertNotNull(next)
        assertEquals("h", next!!.first.lastHost)
        assertEquals(1, next.first.lastPort)
        assertTrue("expected unexpected=true", next.second)
    }

    @Test
    fun disconnected_in_connecting_marks_unexpected_false() {
        val current = ConnectionState.Connecting("h", 1)
        val next = SessionTransitions.nextOnDisconnected(current)
        assertNotNull(next)
        assertEquals("h", next!!.first.lastHost)
        assertEquals(1, next.first.lastPort)
        assertEquals(false, next.second)
    }

    @Test
    fun disconnected_in_already_disconnected_returns_null() {
        val current = ConnectionState.Disconnected()
        assertNull(SessionTransitions.nextOnDisconnected(current))
    }

    // ---------- nextOnClearError ----------

    @Test
    fun clear_error_in_error_state_returns_disconnected_preserving_host() {
        val current = ConnectionState.Error(
            lastHost = "h",
            lastPort = 1,
            message = "boom",
        )
        val next = SessionTransitions.nextOnClearError(current)
        assertNotNull(next)
        assertEquals("h", next!!.lastHost)
        assertEquals(1, next.lastPort)
    }

    @Test
    fun clear_error_in_non_error_state_is_ignored() {
        val current = ConnectionState.Connecting("h", 1)
        assertNull(SessionTransitions.nextOnClearError(current))
    }
}
