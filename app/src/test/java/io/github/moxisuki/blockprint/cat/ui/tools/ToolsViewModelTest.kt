package io.github.moxisuki.blockprint.cat.ui.tools

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ToolsViewModelTest {

    @Test
    fun `tools flow has same size as catalog`() {
        val vm = ToolsViewModel()
        assertThat(vm.tools.value).hasSize(ToolCatalog.entries.size)
    }

    @Test
    fun `tools flow exposes catalog entries in order`() {
        val vm = ToolsViewModel()
        assertThat(vm.tools.value.map { it.id })
            .containsExactlyElementsIn(ToolCatalog.entries.map { it.id })
            .inOrder()
    }

    @Test
    fun `onToolClick returns NotImplemented for any entry`() {
        val vm = ToolsViewModel()
        ToolCatalog.entries.forEach { entry ->
            assertThat(vm.onToolClick(entry)).isEqualTo(ToolClickResult.NotImplemented)
        }
    }
}
