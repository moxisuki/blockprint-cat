package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.moxisuki.blockprint.cat.data.AppDatabase
import io.github.moxisuki.blockprint.cat.data.bridge.BridgeEventRepository
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConnectionViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repo: BridgeEventRepository
    private lateinit var pairedDao: io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceDao

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = BridgeEventRepository(db.bridgeEventDao())
        pairedDao = db.pairedDeviceDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun `clearLog empties the events list`() = runTest {
        repo.log("INFO", "x")
        repo.clear()
        assertTrue(repo.observe(10).first().isEmpty())
    }

    @Test
    fun `deletePaired removes the row`() = runTest {
        pairedDao.upsert(row("a", 1))
        pairedDao.delete("a", 1)
        assertEquals(null, pairedDao.find("a", 1))
    }

    @Test
    fun `renamePaired updates the label`() = runTest {
        pairedDao.upsert(row("a", 1, label = "old"))
        val existing = pairedDao.find("a", 1)
        assertNotNull(existing)
        pairedDao.upsert(existing!!.copy(label = "new"))
        val updated = pairedDao.find("a", 1)
        assertNotNull(updated)
        assertEquals("new", updated?.label)
    }

    private fun row(host: String, port: Int, label: String? = null) = PairedDeviceEntity(
        host = host,
        wsPort = port,
        token = "tok",
        tokenHint = "abc…",
        label = label,
        lastConnectedAt = 0L,
    )
}