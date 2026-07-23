package io.github.moxisuki.blockprint.cat.app.core.resourcepack.model

sealed interface PackProgress {
    data object Idle : PackProgress
    data class FetchingManifest(val label: String) : PackProgress
    data class Downloading(val fileName: String, val fraction: Float) : PackProgress
    data class Extracting(val currentPath: String, val extracted: Int) : PackProgress
    data class Failed(val message: String) : PackProgress
    data object Cancelled : PackProgress
    data class Done(val entry: ResourcePackEntry) : PackProgress
}