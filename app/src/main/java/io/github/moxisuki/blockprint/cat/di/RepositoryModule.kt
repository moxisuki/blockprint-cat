package io.github.moxisuki.blockprint.cat.di

import io.github.moxisuki.blockprint.cat.data.bridge.BridgeClient
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeClientImpl
import io.github.moxisuki.blockprint.cat.data.saf.LitematicFileStorage
import io.github.moxisuki.blockprint.cat.data.saf.SafFileStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindFileStorage(impl: SafFileStorage): LitematicFileStorage

    @Binds @Singleton
    abstract fun bindBridgeClient(impl: BridgeClientImpl): BridgeClient
}
