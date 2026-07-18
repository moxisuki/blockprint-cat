package io.github.moxisuki.blockprint.cat.app.feature.about

sealed interface AboutAction {
    data object Opened : AboutAction

    data object RefreshHitokoto : AboutAction
}
