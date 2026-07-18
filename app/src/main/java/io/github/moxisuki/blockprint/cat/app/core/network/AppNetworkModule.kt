package io.github.moxisuki.blockprint.cat.app.core.network

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppNetworkBindingModule {
    @Binds
    abstract fun bindAppHttpClient(impl: OkHttpAppHttpClient): AppHttpClient
}

@Module
@InstallIn(SingletonComponent::class)
object AppNetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppNetworkConstants.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(AppNetworkConstants.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()
}
