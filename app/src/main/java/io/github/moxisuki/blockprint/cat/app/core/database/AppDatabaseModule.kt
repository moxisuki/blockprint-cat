package io.github.moxisuki.blockprint.cat.app.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppDatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "blockprint_cat.db",
    )
        .addMigrations(MIGRATION_1_2)
        .build()

    @Provides
    fun provideBlueprintDao(database: AppDatabase): BlueprintDao =
        database.blueprintDao()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `blueprint_categories` " +
                    "(`name` TEXT NOT NULL, PRIMARY KEY(`name`))",
            )
        }
    }
}
