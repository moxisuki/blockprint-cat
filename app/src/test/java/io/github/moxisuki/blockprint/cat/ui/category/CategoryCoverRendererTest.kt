package io.github.moxisuki.blockprint.cat.ui.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryCoverRendererTest {

    @Test
    fun `palette has exactly 8 entries`() {
        assertEquals(8, CategoryCover.palette.size)
    }

    @Test
    fun `patterns has exactly 8 entries`() {
        assertEquals(8, CategoryCover.patterns.size)
    }

    @Test
    fun `every pattern is 4 by 4`() {
        CategoryCover.patterns.forEachIndexed { idx, p ->
            assertEquals("pattern $idx has 4 rows", 4, p.cells.size)
            p.cells.forEach { row -> assertEquals("row has 4 cols", 4, row.size) }
        }
    }

    @Test
    fun `every palette entry has valid hex color`() {
        CategoryCover.palette.forEach { c ->
            assertTrue("main: ${c.main}", c.main.matchesHex())
            assertTrue("light: ${c.light}", c.light.matchesHex())
            assertTrue("dark: ${c.dark}", c.dark.matchesHex())
        }
    }

    @Test
    fun `distinct color and pattern combinations are distinct`() {
        val seen = HashSet<Int>()
        for (color in CategoryCover.palette.indices) {
            for (pat in CategoryCover.patterns.indices) {
                val key = color * 100 + pat
                assertTrue("collision at $key", seen.add(key))
            }
        }
        assertEquals(64, seen.size)
    }

    private fun String.matchesHex() = matches(Regex("^#[0-9a-fA-F]{6}$"))
}