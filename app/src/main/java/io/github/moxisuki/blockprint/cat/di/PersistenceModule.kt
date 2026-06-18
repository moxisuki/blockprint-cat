package io.github.moxisuki.blockprint.cat.di

import android.content.Context
import io.github.moxisuki.blockprint.cat.data.McschematicCookieStore
import io.github.moxisuki.blockprint.cat.data.ThemeManager
import io.github.moxisuki.blockprint.cat.data.community.CmsCookieStore
import io.github.moxisuki.blockprint.cat.data.community.CommunitySourcePersistence
import io.github.moxisuki.blockprint.cat.data.settings.LanguageManager
import io.github.moxisuki.blockprint.cat.data.settings.TermsAcceptance
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    @Provides
    @Singleton
    fun provideMcschematicCookieStore(@ApplicationContext context: Context): McschematicCookieStore =
        McschematicCookieStore(context)

    @Provides
    @Singleton
    fun provideThemeManager(@ApplicationContext context: Context): ThemeManager =
        ThemeManager(context)

    @Provides
    @Singleton
    fun provideCommunitySourcePersistence(@ApplicationContext context: Context): CommunitySourcePersistence =
        CommunitySourcePersistence(context)

    @Provides
    @Singleton
    fun provideCmsCookieStore(@ApplicationContext context: Context): CmsCookieStore =
        CmsCookieStore(context)

    @Provides
    @Singleton
    fun provideLanguageManager(@ApplicationContext context: Context): LanguageManager =
        LanguageManager(context)

    @Provides
    @Singleton
    fun provideTermsAcceptance(@ApplicationContext context: Context): TermsAcceptance =
        TermsAcceptance(context)
}
