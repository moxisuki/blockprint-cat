package io.github.moxisuki.blockprint.cat.data.blueprint

data class FileEntry(
    val docId: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
)
