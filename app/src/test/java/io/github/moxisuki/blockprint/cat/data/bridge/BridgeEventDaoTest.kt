package io.github.moxisuki.blockprint.cat.data.bridge

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.moxisuki.blockprint.cat.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BridgeEventDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: BridgeEventDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.bridgeEventDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `insert then observeRecent returns ordered DESC by ts`() = runTest {
        dao.insert(event(ts = 100, type = "A", message = "m1"))
        dao.insert(event(ts = 300, type = "C", message = "m2"))
        dao.insert(event(ts = 200, type = "B", message = "m3"))
        val list = dao.observeRecent(10).first()
        assertEquals(listOf("m2", "m3", "m1"), list.map { it.message })
    }

    @Test
    fun `trim keeps only the latest N`() = runTest {
        repeat(250) { i -> dao.insert(event(ts = i.toLong())) }
        dao.trim(200)
        val list = dao.observeRecent(500).first()
        assertEquals(200, list.size)
        // 保留的是 ts 最大的 200 条 (ts 50..249)
        assertEquals(249L, list.first().ts)
        assertEquals(50L, list.last().ts)
    }

    @Test
    fun `clear empties the table`() = runTest {
        dao.insert(event(1))
        dao.clear()
        assertTrue(dao.observeRecent(10).first().isEmpty())
    }

    private fun event(
        id: Long = 0L,
        ts: Long = 0L,
        type: String = "INFO",
        message: String = "msg",
    ) = BridgeEventEntity(id = id, ts = ts, type = type, message = message)
}