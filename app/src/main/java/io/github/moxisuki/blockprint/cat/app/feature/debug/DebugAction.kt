package io.github.moxisuki.blockprint.cat.app.feature.debug

sealed interface DebugAction {
    data object Opened : DebugAction
    data object CopyInfo : DebugAction
}
