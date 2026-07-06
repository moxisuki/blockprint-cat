package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

import android.graphics.Bitmap
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class ExportPayloadCodecTest {
    @Test fun `roundtrip preserves bitmap dimensions and pixel data`() {
        val original: Bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        for (x in 0 until 4) for (y in 0 until 4) original.setPixel(x, y, Color.rgb(x * 50, y * 50, 128))

        val encoded = ExportPayloadCodec.encode(
            bitmap = original,
            width = 4, height = 4,
            totalBlocks = 16,
            materials = mapOf("white_wool" to 4, "red_wool" to 12),
        )
        assertThat(encoded).isNotEmpty()
        assertThat(encoded.length).isLessThan(200_000)

        val decoded = ExportPayloadCodec.decode(encoded)
        assertThat(decoded.width).isEqualTo(4)
        assertThat(decoded.height).isEqualTo(4)
        assertThat(decoded.totalBlocks).isEqualTo(16)
        assertThat(decoded.materials).containsExactly("white_wool", 4, "red_wool", 12)
        assertThat(decoded.bitmap.width).isEqualTo(4)
        assertThat(decoded.bitmap.height).isEqualTo(4)
        for (x in 0 until 4) for (y in 0 until 4) {
            assertThat(decoded.bitmap.getPixel(x, y)).isEqualTo(Color.rgb(x * 50, y * 50, 128))
        }
    }

    @Test fun `roundtrip works with empty materials map`() {
        val original: Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val encoded = ExportPayloadCodec.encode(original, 2, 2, 4, emptyMap())
        val decoded = ExportPayloadCodec.decode(encoded)
        assertThat(decoded.materials).isEmpty()
        assertThat(decoded.totalBlocks).isEqualTo(4)
    }
}