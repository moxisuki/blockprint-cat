package io.github.moxisuki.blockprint.cat.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintDao
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintCategoryEntity
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintEntity
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintMaterialEntity
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.ResourcePackDao
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.ResourcePackEntity

@Database(
    entities = [
        BlueprintEntity::class,
        BlueprintMaterialEntity::class,
        BlueprintCategoryEntity::class,
        ResourcePackEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blueprintDao(): BlueprintDao
    abstract fun resourcePackDao(): ResourcePackDao

    companion object {
        val MIGRATIONS_2_3: Array<Migration> = arrayOf(
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `resource_pack` (" +
                            "`id` TEXT NOT NULL, `kind` TEXT NOT NULL, `projectSlug` TEXT NOT NULL DEFAULT '', " +
                            "`displayName` TEXT NOT NULL, `versionName` TEXT NOT NULL, `mcVersion` TEXT, " +
                            "`fileCount` INTEGER NOT NULL, `totalSize` INTEGER NOT NULL, `installedAt` INTEGER NOT NULL, " +
                            "`namespaces` TEXT NOT NULL DEFAULT '', `source` TEXT NOT NULL, `sourceVersionId` TEXT, " +
                            "PRIMARY KEY(`id`))",
                    )
                }
            },
        )
    }
}
