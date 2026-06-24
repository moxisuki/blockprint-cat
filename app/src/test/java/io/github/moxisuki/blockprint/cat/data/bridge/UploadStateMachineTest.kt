package io.github.moxisuki.blockprint.cat.data.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UploadStateMachineTest {

    @Test
    fun happy_path_init_to_ready_to_commit_to_result() {
        val sm = UploadStateMachine(fileName = "foo.schem", size = 1024L, overwrite = false, clientSha = "")
        // init: send upload/init
        val initOut = sm.onInit()
        assertEquals(1, initOut.size)
        assertEquals(UploadAction.SendText("upload/init", sm.requestId, "foo.schem", 1024L, false, null), initOut[0])
        // server says ready
        val readyOut = sm.onServerReady()
        assertTrue(readyOut.isEmpty()) // no client action, just internal state change
        assertEquals(UploadPhase.SENDING_CHUNKS, sm.phase)
        // client sends binary (one chunk)
        val chunkOut = sm.onChunkSent(1024)
        assertTrue(chunkOut.isEmpty())
        assertEquals(UploadPhase.WAITING_RESULT, sm.phase)
        // client sends commit
        val commitOut = sm.onCommit()
        assertEquals(1, commitOut.size)
        assertEquals(UploadAction.SendText("upload/commit", sm.requestId, "foo.schem"), commitOut[0])
        // server says ok
        val resultOut = sm.onResult(ok = true, errorCode = null, sha256 = "abc")
        assertEquals(1, resultOut.size)
        assertEquals(UploadEvent.Result(fileName = "foo.schem", ok = true, errorCode = null, sha256 = "abc"), resultOut[0])
        assertEquals(UploadPhase.IDLE, sm.phase)
    }

    @Test
    fun server_returns_busy_error_emits_failed_event() {
        val sm = UploadStateMachine(fileName = "x.litematic", size = 100L, overwrite = true, clientSha = "")
        sm.onInit()
        val out = sm.onResult(ok = false, errorCode = "BUSY", sha256 = null)
        assertEquals(1, out.size)
        assertEquals(UploadEvent.Result(fileName = "x.litematic", ok = false, errorCode = "BUSY", sha256 = null), out[0])
        assertEquals(UploadPhase.IDLE, sm.phase)
    }

    @Test
    fun request_id_format_is_up_prefix_then_8_hex() {
        val sm = UploadStateMachine(fileName = "z.litematic", size = 100L, overwrite = false, clientSha = "")
        assertTrue(sm.requestId.matches(Regex("^up-[0-9a-f]{8}$")))
    }

    @Test
    fun sha_mismatch_resets_to_idle() {
        val sm = UploadStateMachine(fileName = "a.litematic", size = 100L, overwrite = false, clientSha = "deadbeef")
        sm.onInit()
        sm.onServerReady()
        sm.onChunkSent(100)
        sm.onCommit()
        val out = sm.onResult(ok = false, errorCode = "SHA_MISMATCH", sha256 = null)
        assertEquals(1, out.size)
        assertEquals(UploadPhase.IDLE, sm.phase)
    }

    @Test
    fun onServerReady_in_idle_phase_returns_empty() {
        val sm = UploadStateMachine(fileName = "a.litematic", size = 100L, overwrite = false, clientSha = "")
        // No onInit() called.
        val out = sm.onServerReady()
        assertTrue(out.isEmpty())
        assertEquals(UploadPhase.IDLE, sm.phase)
    }

    @Test
    fun onCommit_from_sending_chunks_emits_action_and_advances() {
        // Repro of the second Fix B scenario: server is ready but no
        // chunks have been sent (e.g. zero-byte file or a race where
        // onChunkSent was skipped). onCommit must still emit the
        // upload/commit action and advance to WAITING_RESULT.
        val sm = UploadStateMachine(fileName = "a.litematic", size = 100L, overwrite = false, clientSha = "")
        sm.onInit()
        sm.onServerReady()
        // Skip onChunkSent; jump straight to onCommit.
        val out = sm.onCommit()
        assertEquals(1, out.size)
        assertEquals(UploadAction.SendText("upload/commit", sm.requestId, "a.litematic"), out[0])
        assertEquals(UploadPhase.WAITING_RESULT, sm.phase)
    }

    @Test
    fun multi_chunk_accumulates_sent_bytes() {
        val sm = UploadStateMachine(fileName = "a.litematic", size = 100L, overwrite = false, clientSha = "")
        sm.onInit()
        sm.onServerReady()
        sm.onChunkSent(30)
        assertEquals(UploadPhase.SENDING_CHUNKS, sm.phase)
        sm.onChunkSent(30)
        assertEquals(UploadPhase.SENDING_CHUNKS, sm.phase)
        sm.onChunkSent(40) // total 100
        assertEquals(UploadPhase.WAITING_RESULT, sm.phase)
    }

    @Test
    fun eager_chunk_send_before_server_ready_advances_to_sending_chunks() {
        // Repro of the e2e bug: client sends upload/init, then sends
        // binary chunks immediately (before server's upload/ready),
        // then calls onCommit. All three must work without NoSuchElementException.
        val sm = UploadStateMachine(fileName = "small.litematic", size = 100L, overwrite = false, clientSha = "")
        val initOut = sm.onInit()
        assertEquals(1, initOut.size)
        assertEquals(UploadPhase.WAITING_READY, sm.phase)

        // Eager chunk send (server hasn't said ready yet)
        val chunkOut = sm.onChunkSent(50)
        assertTrue(chunkOut.isEmpty())
        assertEquals(UploadPhase.SENDING_CHUNKS, sm.phase)  // must transition

        // Second chunk fills the file
        val chunkOut2 = sm.onChunkSent(50)
        assertTrue(chunkOut2.isEmpty())
        assertEquals(UploadPhase.WAITING_RESULT, sm.phase)

        // Commit must succeed
        val commitOut = sm.onCommit()
        assertEquals(1, commitOut.size)
        assertEquals(UploadAction.SendText("upload/commit", sm.requestId, "small.litematic"), commitOut[0])
    }
}
