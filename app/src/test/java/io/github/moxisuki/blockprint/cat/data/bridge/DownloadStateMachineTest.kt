package io.github.moxisuki.blockprint.cat.data.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStateMachineTest {

    @Test
    fun happy_path_ready_to_chunks_to_done() {
        val sm = DownloadStateMachine()
        val requestId = "dl-aaaaaaaa"
        // client sends download
        val out = sm.onDownloadRequested(fileName = "a.schem", requestId = requestId, source = null)
        assertEquals(1, out.size)
        assertEquals(DownloadAction.SendText("download", requestId, "a.schem", null), out[0])
        assertEquals(DownloadPhase.WAITING_READY, sm.stateOf(requestId)?.phase)
        // server sends download/ready
        val readyOut = sm.onServerReady(requestId, size = 1024, sha256 = "abc")
        assertTrue(readyOut.isEmpty())
        assertEquals(DownloadPhase.RECEIVING, sm.stateOf(requestId)?.phase)
        // server sends one binary chunk of 1024 bytes (covers full file)
        val chunkOut = sm.onBinaryReceived(requestId, ByteArray(1024))
        assertEquals(1, chunkOut.size)
        assertTrue(chunkOut[0] is DownloadEvent.Complete)
        assertNull(sm.stateOf(requestId))
    }

    @Test
    fun multi_download_routes_by_request_id() {
        val sm = DownloadStateMachine()
        val r1 = "dl-11111111"
        val r2 = "dl-22222222"
        sm.onDownloadRequested("a.litematic", r1, null)
        sm.onDownloadRequested("b.litematic", r2, null)
        sm.onServerReady(r1, size = 100, sha256 = "h1")
        sm.onServerReady(r2, size = 200, sha256 = "h2")
        sm.onBinaryReceived(r1, ByteArray(50))
        sm.onBinaryReceived(r2, ByteArray(100))
        // complete r2 first (interleaved)
        val done2 = sm.onServerDone(r2, ok = true, bytes = 200, sha256 = "h2")
        assertEquals(1, done2.size)
        assertEquals("b.litematic", (done2[0] as DownloadEvent.Complete).fileName)
        // r1 still receiving
        assertNotNull(sm.stateOf(r1))
        // complete r1
        val done1 = sm.onServerDone(r1, ok = true, bytes = 100, sha256 = "h1")
        assertEquals(1, done1.size)
        assertEquals("a.litematic", (done1[0] as DownloadEvent.Complete).fileName)
    }

    @Test
    fun accumulator_reaches_size_without_done_emits_complete() {
        val sm = DownloadStateMachine()
        val r = "dl-33333333"
        sm.onDownloadRequested("x.nbt", r, null)
        sm.onServerReady(r, size = 5, sha256 = "h")
        val out = sm.onBinaryReceived(r, ByteArray(5))
        assertEquals(1, out.size)
        assertTrue(out[0] is DownloadEvent.Complete)
        assertNull(sm.stateOf(r))
    }

    @Test
    fun orphan_binary_for_unknown_request_id_is_dropped() {
        val sm = DownloadStateMachine()
        val out = sm.onBinaryReceived("dl-unknown", ByteArray(10))
        assertTrue(out.isEmpty())
    }

    @Test
    fun request_id_format_is_dl_prefix_then_8_hex() {
        val sm = DownloadStateMachine()
        val id = sm.newRequestId()
        assertTrue(id.matches(Regex("^dl-[0-9a-f]{8}$")))
    }

    @Test
    fun onServerDone_with_ok_false_emits_failed_event() {
        val sm = DownloadStateMachine()
        val r = "dl-44444444"
        sm.onDownloadRequested("a.litematic", r, null)
        sm.onServerReady(r, size = 100, sha256 = "h")
        sm.onBinaryReceived(r, ByteArray(50))
        val out = sm.onServerDone(r, ok = false, bytes = 50, sha256 = "h")
        assertEquals(1, out.size)
        assertTrue(out[0] is DownloadEvent.Failed)
        assertNull(sm.stateOf(r))
    }

    @Test
    fun onServerDone_with_sha_mismatch_emits_failed_event() {
        val sm = DownloadStateMachine()
        val r = "dl-55555555"
        sm.onDownloadRequested("a.litematic", r, null)
        sm.onServerReady(r, size = 10, sha256 = "deadbeef")
        sm.onBinaryReceived(r, ByteArray(5))
        val out = sm.onServerDone(r, ok = true, bytes = 5, sha256 = "cafebabe")
        assertEquals(1, out.size)
        assertTrue(out[0] is DownloadEvent.Failed)
        assertEquals("SHA_MISMATCH", (out[0] as DownloadEvent.Failed).errorCode)
    }
}
