package io.github.moxisuki.blockprint.cat.ui.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ToolCatalogTest {

    @Test
    fun `entries is non-empty`() {
        assertThat(ToolCatalog.entries).isNotEmpty()
    }

    @Test
    fun `exactly one Hero entry`() {
        val heroes = ToolCatalog.entries.filter { it.kind == ToolKind.Hero }
        assertThat(heroes).hasSize(1)
    }

    @Test
    fun `all ids are unique`() {
        val ids = ToolCatalog.entries.map { it.id }
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun `Hero entry has non-null subtitle`() {
        val hero = ToolCatalog.entries.first { it.kind == ToolKind.Hero }
        assertThat(hero.subtitleRes).isNotNull()
    }

    @Test
    fun `non-Hero entries do not require subtitle`() {
        val nonHero = ToolCatalog.entries.filter { it.kind != ToolKind.Hero }
        assertThat(nonHero).isNotEmpty()
    }
}