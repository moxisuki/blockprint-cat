package io.github.moxisuki.blockprint.cat.data.bridge

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.moxisuki.blockprint.cat.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PairedDeviceDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: PairedDeviceDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.pairedDeviceDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `upsert then observeAll returns entity`() = runTest {
        val entity = sample("192.168.1.10", 51234, ts = 1000L)
        dao.upsert(entity)
        val list = dao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals(entity, list[0])
    }

    @Test
    fun `observeAll sorts by lastConnectedAt DESC`() = runTest {
        dao.upsert(sample("a", 1, ts = 1000L))
        dao.upsert(sample("b", 2, ts = 3000L))
        dao.upsert(sample("c", 3, ts = 2000L))
        val list = dao.observeAll().first()
        assertEquals(listOf("b", "c", "a"), list.map { it.host })
    }

    @Test
    fun `delete removes the row`() = runTest {
        dao.upsert(sample("a", 1))
        dao.delete("a", 1)
        assertNull(dao.find("a", 1))
    }

    @Test
    fun `upsert on existing primary key overwrites`() = runTest {
        dao.upsert(sample("a", 1, label = "old"))
        dao.upsert(sample("a", 1, label = "new"))
        assertEquals("new", dao.find("a", 1)?.label)
    }

    private fun sample(host: String, port: Int, ts: Long = 0L, label: String? = null) =
        PairedDeviceEntity(
            host = host,
            wsPort = port,
            token = "tok",
            tokenHint = "abc…",
            label = label,
            lastConnectedAt = ts,
        )
}