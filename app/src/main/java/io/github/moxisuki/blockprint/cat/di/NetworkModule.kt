package io.github.moxisuki.blockprint.cat.di

import io.github.moxisuki.blockprint.cat.data.BridgeDiscovery
import io.github.moxisuki.blockprint.cat.data.IconIndexResolver
import io.github.moxisuki.blockprint.cat.data.McschematicClient
import io.github.moxisuki.blockprint.cat.data.McschematicCookieStore
import io.github.moxisuki.blockprint.cat.data.community.CmsClient
import io.github.moxisuki.blockprint.cat.data.community.CmsCookieStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Provides
    @Singleton
    fun provideMcschematicClient(cookieStore: McschematicCookieStore, okHttpClient: OkHttpClient): McschematicClient =
        McschematicClient(cookieStore, okHttpClient, "https://www.mcschematic.top")

    @Provides
    @Singleton
    fun provideIconIndexResolver(httpClient: OkHttpClient): IconIndexResolver =
        IconIndexResolver(httpClient)

    @Provides
    @Singleton
    fun provideBridgeDiscovery(): BridgeDiscovery = BridgeDiscovery()

    @Provides
    @Singleton
    fun provideCmsClient(cookieStore: CmsCookieStore, okHttpClient: OkHttpClient): CmsClient =
        CmsClient(cookieStore, okHttpClient, "https://www.creativemechanicserver.com")
}
