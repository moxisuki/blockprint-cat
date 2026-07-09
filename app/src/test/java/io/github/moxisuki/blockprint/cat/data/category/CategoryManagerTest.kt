package io.github.moxisuki.blockprint.cat.data.category

import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMetaDao
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMetaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CategoryManagerTest {

    private class FakeCategoryDao : CategoryDao {
        val rows = MutableStateFlow<List<CategoryEntity>>(emptyList())
        override fun observeAll(): Flow<List<CategoryEntity>> = rows
        override fun observeCountsByCategory(): Flow<List<CategoryCount>> =
            MutableStateFlow(emptyList())
        override suspend fun upsert(category: CategoryEntity) {
            rows.value = rows.value.filterNot { it.id == category.id } + category
        }
        override suspend fun update(category: CategoryEntity) = upsert(category)
        override suspend fun deleteById(id: String) {
            rows.value = rows.value.filterNot { it.id == id }
        }
    }

    private class FakeBpDao : BlueprintMetaDao {
        data class Reassignment(val uuids: List<String>, val targetId: String?)

        val reassignments = mutableListOf<Reassignment>()
        override suspend fun reassignCategory(uuids: List<String>, targetId: String?) {
            reassignments += Reassignment(uuids, targetId)
        }
        // Other methods unused in these tests — throw to surface accidental calls.
        override fun observeAll(): Flow<List<BlueprintMetaEntity>> =
            throw NotImplementedError()
        override fun observeCount(): Flow<Int> = throw NotImplementedError()
        override suspend fun getAll(): List<BlueprintMetaEntity> =
            throw NotImplementedError()
        override suspend fun getByUuid(uuid: String): BlueprintMetaEntity? =
            throw NotImplementedError()
        override suspend fun getByDocId(docId: String): BlueprintMetaEntity? =
            throw NotImplementedError()
        override suspend fun upsert(entity: BlueprintMetaEntity) =
            throw NotImplementedError()
        override suspend fun upsertAll(entities: List<BlueprintMetaEntity>) =
            throw NotImplementedError()
        override suspend fun delete(uuid: String) = throw NotImplementedError()
        override suspend fun deleteAll() = throw NotImplementedError()
    }

    @Test
    fun `create assigns id and persists`() = runTest {
        val dao = FakeCategoryDao()
        val bpDao = FakeBpDao()
        val mgr = CategoryManager(dao, bpDao)
        val id = mgr.create("Castle", colorIdx = 0, patternIdx = 0)
        assertNotNull(UUID.fromString(id))
        assertEquals(1, dao.rows.value.size)
        assertEquals("Castle", dao.rows.value[0].name)
    }

    @Test
    fun `rename updates name field`() = runTest {
        val dao = FakeCategoryDao().also { it.rows.value = listOf(testCat("c1", "Old")) }
        val mgr = CategoryManager(dao, FakeBpDao())
        mgr.rename("c1", "New")
        assertEquals("New", dao.rows.value.first().name)
    }

    @Test
    fun `changeCover updates color and pattern`() = runTest {
        val dao = FakeCategoryDao().also {
            it.rows.value = listOf(testCat("c1", "X", colorIdx = 0, patternIdx = 0))
        }
        val mgr = CategoryManager(dao, FakeBpDao())
        mgr.changeCover("c1", colorIdx = 5, patternIdx = 3)
        val row = dao.rows.value.first()
        assertEquals(5, row.colorIdx)
        assertEquals(3, row.patternIdx)
    }

    @Test
    fun `delete removes category`() = runTest {
        val dao = FakeCategoryDao().also { it.rows.value = listOf(testCat("c1", "X")) }
        val mgr = CategoryManager(dao, FakeBpDao())
        mgr.delete("c1")
        assertEquals(emptyList<CategoryEntity>(), dao.rows.value)
    }

    @Test
    fun `moveBlueprintsToCategory calls dao reassign`() = runTest {
        val dao = FakeCategoryDao()
        val bpDao = FakeBpDao()
        val mgr = CategoryManager(dao, bpDao)
        mgr.moveBlueprintsToCategory(listOf("b1", "b2"), "c1")
        assertEquals(
            listOf(FakeBpDao.Reassignment(listOf("b1", "b2"), "c1")),
            bpDao.reassignments,
        )
    }

    @Test
    fun `moveBlueprintsToCategory with null removes from any category`() = runTest {
        val dao = FakeCategoryDao()
        val bpDao = FakeBpDao()
        val mgr = CategoryManager(dao, bpDao)
        mgr.moveBlueprintsToCategory(listOf("b1"), null)
        assertEquals(
            listOf(FakeBpDao.Reassignment(listOf("b1"), null)),
            bpDao.reassignments,
        )
    }

    @Test
    fun `categories StateFlow starts with ALL then user categories`() = runTest {
        val dao = FakeCategoryDao().also {
            it.rows.value = listOf(testCat("c2", "Castle"), testCat("c1", "Zeta"))
        }
        val mgr = CategoryManager(dao, FakeBpDao())
        val first = mgr.categories.first()
        // CategoryRow.All must be a singleton (data object), so compare by type.
        assertTrue(
            "First row should be the synthetic ALL row, was ${first.first()}",
            first.first() is CategoryRow.All,
        )
    }

    private fun testCat(
        id: String,
        name: String,
        colorIdx: Int = 0,
        patternIdx: Int = 0,
    ) = CategoryEntity(
        id = id,
        name = name,
        colorIdx = colorIdx,
        patternIdx = patternIdx,
        sortOrder = 0,
        createdAt = 1L,
    )
}
