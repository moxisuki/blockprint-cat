package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.moxisuki.blockprint.cat.data.AppDatabase
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeEventRepository
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BridgeViewModelLogTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var pairedDao: PairedDeviceDao
    private lateinit var repo: BridgeEventRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        pairedDao = db.pairedDeviceDao()
        repo = BridgeEventRepository(db.bridgeEventDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `upsertPaired creates new row`() = runTest {
        pairedDao.upsert(
            io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity(
                host = "192.168.1.10",
                wsPort = 51234,
                token = "tok",
                tokenHint = "abc…",
                label = "客厅 PC",
                lastConnectedAt = System.currentTimeMillis(),
                folderName = "survival-v6",
            )
        )
        val list = pairedDao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals("客厅 PC", list[0].label)
    }

    @Test
    fun `mostRecent returns the latest connected device`() = runTest {
        pairedDao.upsert(row("a", 1, ts = 1000L))
        pairedDao.upsert(row("b", 2, ts = 3000L))
        val recent = pairedDao.mostRecent()
        assertNotNull(recent)
        assertEquals("b", recent?.host)
    }

    @Test
    fun `delete removes the row`() = runTest {
        pairedDao.upsert(row("a", 1))
        pairedDao.delete("a", 1)
        assertNull(pairedDao.find("a", 1))
    }

    @Test
    fun `event log trims to 200`() = runTest {
        repeat(250) { i ->
            repo.log("INFO", "msg-$i", host = "h", port = i)
        }
        val list = repo.observe(500).first()
        assertEquals(200, list.size)
    }

    private fun row(host: String, port: Int, ts: Long = 0L) =
        io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity(
            host = host,
            wsPort = port,
            token = "tok",
            tokenHint = "abc…",
            label = null,
            lastConnectedAt = ts,
        )
}
