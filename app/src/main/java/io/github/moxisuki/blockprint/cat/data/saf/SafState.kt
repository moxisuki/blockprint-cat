package io.github.moxisuki.blockprint.cat.data.saf

sealed class SafState {
    data object Idle : SafState()
    data class Ready(val displayName: String) : SafState()
}
