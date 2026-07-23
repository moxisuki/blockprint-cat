package io.github.moxisuki.blockprint.cat.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test fun migrationFromV2ToV3_createsResourcePackTable() {
        helper.createDatabase("bp.db", 2).use { db ->
            // nothing to seed; we only care that the new table exists after migration
        }
        helper.runMigrationsAndValidate("bp.db", 3, true, *AppDatabase.MIGRATIONS_2_3).use { db ->
            db.query("SELECT id, kind FROM resource_pack").use { cursor ->
                assertThat(cursor.count).isEqualTo(0)
            }
        }
    }
}
