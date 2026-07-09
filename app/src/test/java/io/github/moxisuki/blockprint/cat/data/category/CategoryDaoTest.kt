package io.github.moxisuki.blockprint.cat.data.category

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.moxisuki.blockprint.cat.data.AppDatabase
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMetaDao
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMetaEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class CategoryDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: CategoryDao
    private lateinit var bpDao: BlueprintMetaDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.categoryDao()
        bpDao = db.blueprintMetaDao()
    }

    @After
    fun tearDown() { db.close() }

    private fun cat(id: String, name: String, colorIdx: Int = 0, patternIdx: Int = 0, createdAt: Long = 1L) =
        CategoryEntity(id = id, name = name, colorIdx = colorIdx, patternIdx = patternIdx, sortOrder = 0, createdAt = createdAt)

    private fun bp(uuid: String, categoryId: String? = null) = BlueprintMetaEntity(
        uuid = uuid,
        fileDocId = "tree:$uuid",
        fileName = "$uuid.litematic",
        displayName = uuid,
        author = "tester",
        regionCount = 1,
        blockCount = 1,
        format = "LITEMATIC",
        lastScannedAt = 1L,
        categoryId = categoryId,
    )

    @Test
    fun `upsert then observeAll returns inserted entity`() = runTest {
        assertEquals(emptyList<CategoryEntity>(), dao.observeAll().first())
        dao.upsert(cat("c1", "Castle", createdAt = 2L))
        assertEquals(listOf("c1"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun `observeAll orders by sortOrder then createdAt`() = runTest {
        dao.upsert(cat("c1", "A", createdAt = 3L))
        dao.upsert(cat("c2", "B", createdAt = 1L))
        dao.upsert(cat("c3", "C", createdAt = 2L))
        val list = dao.observeAll().first()
        assertEquals(listOf("c2", "c3", "c1"), list.map { it.id })
    }

    @Test
    fun `update changes fields`() = runTest {
        dao.upsert(cat("c1", "Old"))
        val updated = cat("c1", "New", colorIdx = 3)
        dao.update(updated)
        assertEquals("New", dao.observeAll().first()[0].name)
    }

    @Test
    fun `deleteById removes category`() = runTest {
        dao.upsert(cat("c1", "X"))
        dao.deleteById("c1")
        assertEquals(0, dao.observeAll().first().size)
    }

    @Test
    fun `observeCountsByCategory returns group counts including null`() = runTest {
        dao.upsert(cat("c1", "Castle"))
        dao.upsert(cat("c2", "Redstone"))
        bpDao.upsert(bp("b1", "c1"))
        bpDao.upsert(bp("b2", "c1"))
        bpDao.upsert(bp("b3", "c2"))
        bpDao.upsert(bp("b4", null))

        val counts = dao.observeCountsByCategory().first().associate { it.categoryId to it.cnt }
        assertEquals(2, counts["c1"])
        assertEquals(1, counts["c2"])
        assertEquals(1, counts[null])
    }
}