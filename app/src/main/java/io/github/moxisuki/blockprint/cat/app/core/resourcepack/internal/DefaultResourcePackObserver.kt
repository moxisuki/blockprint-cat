package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackAssetLocator
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackObserver
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

@Singleton
class DefaultResourcePackObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locator: ResourcePackAssetLocator,
) : ResourcePackObserver {
    override fun observeAvailability(): Flow<Set<String>> = flow {
        emit(locator.installedNamespaces())
        while (true) {
            kotlinx.coroutines.delay(2_000L)
            emit(locator.installedNamespaces())
        }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
}
