package io.github.moxisuki.blockprint.cat.di

import io.github.moxisuki.blockprint.cat.data.DefaultDispatcherProvider
import io.github.moxisuki.blockprint.cat.data.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}
