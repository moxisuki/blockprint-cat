package io.github.moxisuki.blockprint.cat.di

import android.content.Context
import androidx.room.Room
import io.github.moxisuki.blockprint.cat.data.AppDatabase
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMetaDao
import io.github.moxisuki.blockprint.cat.data.blueprint.StorageConfigDao
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeEventDao
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceDao
import io.github.moxisuki.blockprint.cat.data.vanilla.VanillaAssetStatusDao
import io.github.moxisuki.blockprint.cat.data.community.DisclaimerStatusDao
import io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao
import io.github.moxisuki.blockprint.cat.data.vanilla.ModAssetStatusDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "blockprintcat.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBlueprintMetaDao(db: AppDatabase): BlueprintMetaDao = db.blueprintMetaDao()

    @Provides
    fun provideStorageConfigDao(db: AppDatabase): StorageConfigDao = db.storageConfigDao()

    @Provides
    fun provideVanillaAssetStatusDao(db: AppDatabase): VanillaAssetStatusDao = db.vanillaAssetStatusDao()

    @Provides
    fun provideModAssetStatusDao(db: AppDatabase): ModAssetStatusDao = db.modAssetStatusDao()

    @Provides
    fun provideDisclaimerStatusDao(db: AppDatabase): DisclaimerStatusDao = db.disclaimerStatusDao()

    @Provides
    fun provideGlbCacheDao(db: AppDatabase): GlbCacheDao = db.glbCacheDao()

    @Provides
    fun providePairedDeviceDao(db: AppDatabase): PairedDeviceDao = db.pairedDeviceDao()

    @Provides
    fun provideBridgeEventDao(db: AppDatabase): BridgeEventDao = db.bridgeEventDao()
}
