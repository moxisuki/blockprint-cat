package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import kotlinx.coroutines.flow.Flow

interface ResourcePackObserver {
    fun observeAvailability(): Flow<Set<String>>
}