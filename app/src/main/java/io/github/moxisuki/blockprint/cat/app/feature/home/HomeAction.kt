package io.github.moxisuki.blockprint.cat.app.feature.home

sealed interface HomeAction {
    data object Opened : HomeAction
}
