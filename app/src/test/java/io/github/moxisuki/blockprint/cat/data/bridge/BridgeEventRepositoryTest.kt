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
class BridgeEventRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: BridgeEventRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = BridgeEventRepository(db.bridgeEventDao())
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun `log inserts a row observable via observe`() = runTest {
        repo.log("DISCOVER", "192.168.1.10", host = "192.168.1.10", port = 51234)
        val list = repo.observe(10).first()
        assertEquals(1, list.size)
        assertEquals("DISCOVER", list[0].type)
        assertEquals("192.168.1.10", list[0].host)
    }

    @Test
    fun `log with same timestamp trims to keep limit`() = runTest {
        repeat(250) { i -> repo.log("INFO", "msg-$i") }
        val list = repo.observe(500).first()
        assertEquals(200, list.size)
    }

    @Test
    fun `clear empties the list`() = runTest {
        repo.log("INFO", "x")
        repo.clear()
        assertTrue(repo.observe(10).first().isEmpty())
    }
}