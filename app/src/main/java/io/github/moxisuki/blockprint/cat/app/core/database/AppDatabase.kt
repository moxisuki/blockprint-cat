package io.github.moxisuki.blockprint.cat.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintDao
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintCategoryEntity
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintEntity
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintMaterialEntity

@Database(
    entities = [
        BlueprintEntity::class,
        BlueprintMaterialEntity::class,
        BlueprintCategoryEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blueprintDao(): BlueprintDao
}
