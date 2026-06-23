package io.github.moxisuki.blockprint.cat.data.saf

import io.github.moxisuki.blockprint.cat.data.blueprint.FileEntry
import java.io.OutputStream

interface LitematicFileStorage {
    suspend fun list(): List<FileEntry>
    suspend fun read(docId: String): ByteArray
    suspend fun write(name: String, bytes: ByteArray, onProgress: ((Long, Long) -> Unit)? = null): String
    /**
     * Streaming write: create [name] in the configured folder and hand the
     * raw [OutputStream] to [writer]. The stream is closed by the storage
     * implementation after [writer] returns (success or failure). Returns
     * the storage's docId for the new file, suitable for follow-up calls
     * to `read` / `delete` / `rename`.
     *
     * Use this when the producer is a streaming encoder (e.g.
     * `BlueprintConverter.convert(Litematic, target, OutputStream)`) so
     * the encoded payload never has to sit in memory as a `ByteArray`.
     */
    suspend fun writeStream(name: String, writer: (OutputStream) -> Unit): String
    suspend fun delete(docId: String)
    suspend fun rename(docId: String, newName: String): String
    suspend fun exists(name: String): Boolean
}
