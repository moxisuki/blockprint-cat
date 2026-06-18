package io.github.moxisuki.blockprint.cat.data

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMetaEntity
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMetaDao
import io.github.moxisuki.blockprint.cat.data.blueprint.StorageConfigEntity
import io.github.moxisuki.blockprint.cat.data.blueprint.StorageConfigDao
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeEventDao
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeEventEntity
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceDao
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity
import io.github.moxisuki.blockprint.cat.data.community.DisclaimerStatusEntity
import io.github.moxisuki.blockprint.cat.data.community.DisclaimerStatusDao
import io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao
import io.github.moxisuki.blockprint.cat.data.render.GlbCacheEntity
import io.github.moxisuki.blockprint.cat.data.vanilla.ModAssetStatusEntity
import io.github.moxisuki.blockprint.cat.data.vanilla.ModAssetStatusDao
import io.github.moxisuki.blockprint.cat.data.vanilla.VanillaAssetStatusEntity
import io.github.moxisuki.blockprint.cat.data.vanilla.VanillaAssetStatusDao

@Database(
    entities = [
        BlueprintMetaEntity::class,
        StorageConfigEntity::class,
        VanillaAssetStatusEntity::class,
        ModAssetStatusEntity::class,
        DisclaimerStatusEntity::class,
        GlbCacheEntity::class,
        PairedDeviceEntity::class,
        BridgeEventEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blueprintMetaDao(): BlueprintMetaDao
    abstract fun storageConfigDao(): StorageConfigDao
    abstract fun vanillaAssetStatusDao(): VanillaAssetStatusDao
    abstract fun modAssetStatusDao(): ModAssetStatusDao
    abstract fun disclaimerStatusDao(): DisclaimerStatusDao
    abstract fun glbCacheDao(): GlbCacheDao
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun bridgeEventDao(): BridgeEventDao
}
