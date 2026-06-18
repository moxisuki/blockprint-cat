package io.github.moxisuki.blockprint.cat.glb

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class GlbCacheTest {
    @Test
    fun `put and get`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "glb_cache_test_${System.nanoTime()}")
        dir.deleteOnExit()
        val cache = GlbCache(dir)
        val bytes = byteArrayOf(1, 2, 3, 4)
        cache.put("test_key", 0, bytes)
        val result = cache.get("test_key", 0)
        assertArrayEquals(bytes, result)
    }

    @Test
    fun `get missing returns null`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "glb_cache_test_${System.nanoTime()}")
        dir.deleteOnExit()
        val cache = GlbCache(dir)
        assertNull(cache.get("nonexistent", 0))
    }
}
