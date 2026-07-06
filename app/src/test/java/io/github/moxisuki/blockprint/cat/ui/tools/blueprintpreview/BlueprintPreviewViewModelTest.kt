package io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.ExportPayloadCodec
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class BlueprintPreviewViewModelTest {
    @Test fun `decodes encoded payload on construction`() {
        val bmp: Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val encoded = ExportPayloadCodec.encode(bmp, 2, 2, 4, mapOf("white_wool" to 4))
        // Simulate URL round-trip: production nav arg is URL-encoded by NavRoutes, then
        // URL-decoded by Compose Nav before reaching the ViewModel.
        val vm = BlueprintPreviewViewModel(
            java.net.URLDecoder.decode(java.net.URLEncoder.encode(encoded, "UTF-8"), "UTF-8"),
        )
        assertThat(vm.state.value.width).isEqualTo(2)
        assertThat(vm.state.value.height).isEqualTo(2)
        assertThat(vm.state.value.totalBlocks).isEqualTo(4)
        assertThat(vm.state.value.materials).containsEntry("white_wool", 4)
    }

    @Test fun `changing export type does not lose materials`() {
        val bmp: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val encoded = ExportPayloadCodec.encode(bmp, 1, 1, 1, mapOf("red_wool" to 1))
        val vm = BlueprintPreviewViewModel(
            java.net.URLDecoder.decode(java.net.URLEncoder.encode(encoded, "UTF-8"), "UTF-8"),
        )
        vm.setExportType(ExportType.BLUEPRINT_SPONGE)
        assertThat(vm.state.value.exportType).isEqualTo(ExportType.BLUEPRINT_SPONGE)
        assertThat(vm.state.value.materials).containsEntry("red_wool", 1)
    }

    @Test fun `changing blueprint mode does not lose materials`() {
        val bmp: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val encoded = ExportPayloadCodec.encode(bmp, 1, 1, 1, mapOf("blue_wool" to 1))
        val vm = BlueprintPreviewViewModel(
            java.net.URLDecoder.decode(java.net.URLEncoder.encode(encoded, "UTF-8"), "UTF-8"),
        )
        vm.setBlueprintMode(BlueprintMode.FLAT)
        assertThat(vm.state.value.blueprintMode).isEqualTo(BlueprintMode.FLAT)
        assertThat(vm.state.value.materials).containsEntry("blue_wool", 1)
    }

    @Test fun `default export type is litematica and mode is wall`() {
        val bmp: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val encoded = ExportPayloadCodec.encode(bmp, 1, 1, 1, mapOf("stone" to 1))
        val vm = BlueprintPreviewViewModel(encoded)
        assertThat(vm.state.value.exportType).isEqualTo(ExportType.BLUEPRINT_LITEMATICA)
        assertThat(vm.state.value.blueprintMode).isEqualTo(BlueprintMode.WALL)
    }
}