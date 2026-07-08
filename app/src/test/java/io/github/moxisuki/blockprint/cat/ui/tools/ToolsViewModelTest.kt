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
    fun `onToolClick navigates for image_to_blueprint`() {
        val vm = ToolsViewModel()
        val entry = ToolCatalog.entries.first { it.id == "image_to_blueprint" }
        assertThat(vm.onToolClick(entry)).isEqualTo(ToolClickResult.NavigateToImageToBlueprint)
    }

    @Test
    fun `onToolClick returns NotImplemented for unimplemented tools`() {
        val vm = ToolsViewModel()
        val implementedIds = setOf("image_to_blueprint", "text_to_blueprint", "block_paint")
        ToolCatalog.entries.filter { it.id !in implementedIds }.forEach { entry ->
            assertThat(vm.onToolClick(entry)).isEqualTo(ToolClickResult.NotImplemented)
        }
    }
}
