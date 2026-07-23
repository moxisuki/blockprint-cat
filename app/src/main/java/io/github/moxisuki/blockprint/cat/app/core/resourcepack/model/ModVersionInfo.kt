package io.github.moxisuki.blockprint.cat.app.core.resourcepack.model

data class ModVersionInfo(
    val id: String,
    val name: String,
    val gameVersions: List<String>,
    val fileName: String,
    val fileSize: Long,
    val fileUrl: String,
)