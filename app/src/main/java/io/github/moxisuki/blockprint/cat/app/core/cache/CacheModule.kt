package io.github.moxisuki.blockprint.cat.app.core.cache

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CacheModule {
    @Binds
    @Singleton
    abstract fun bindAppCacheManager(impl: DefaultAppCacheManager): AppCacheManager
}
