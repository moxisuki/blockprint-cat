package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackAssetLocator
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackObserver
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ResourcePackModule {
    @Binds @Singleton abstract fun bindRepository(impl: DefaultResourcePackRepository): ResourcePackRepository
    @Binds @Singleton abstract fun bindLocator(impl: DefaultResourcePackAssetLocator): ResourcePackAssetLocator
    @Binds @Singleton abstract fun bindObserver(impl: DefaultResourcePackObserver): ResourcePackObserver
}