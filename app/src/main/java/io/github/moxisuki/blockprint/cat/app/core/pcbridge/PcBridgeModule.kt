package io.github.moxisuki.blockprint.cat.app.core.pcbridge

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PcBridgeModule {
    @Binds
    abstract fun bindPcBridgeClient(impl: OkHttpPcBridgeClient): PcBridgeClient
}
