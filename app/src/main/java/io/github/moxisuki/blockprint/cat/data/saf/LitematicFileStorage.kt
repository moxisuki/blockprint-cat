package io.github.moxisuki.blockprint.cat.data.saf

import io.github.moxisuki.blockprint.cat.data.blueprint.FileEntry

interface LitematicFileStorage {
    suspend fun list(): List<FileEntry>
    suspend fun read(docId: String): ByteArray
    suspend fun write(name: String, bytes: ByteArray): String
    suspend fun delete(docId: String)
    suspend fun rename(docId: String, newName: String): String
    suspend fun exists(name: String): Boolean
}
