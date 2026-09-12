package io.github.moxisuki.blockprint.cat.app.core.resourcepack.model

sealed interface PackProgress {
    data object Idle : PackProgress
    data class Preparing(val label: String) : PackProgress
    data class FetchingManifest(val label: String) : PackProgress
    data class Downloading(
        val fileName: String,
        val fraction: Float,
        val bytesRead: Long = -1L,
        val totalBytes: Long = -1L,
    ) : PackProgress
    data class Installing(
        val label: String,
        val fraction: Float?,
        val installedFiles: Int,
        val totalFiles: Int?,
    ) : PackProgress
    data class Extracting(val currentPath: String, val extracted: Int) : PackProgress
    data class Failed(val message: String) : PackProgress
    data object Cancelled : PackProgress
    data class Done(val entry: ResourcePackEntry) : PackProgress
}
