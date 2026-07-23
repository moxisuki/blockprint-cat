package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.LangReader
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LangReaderTest {
    @Test fun `displayName prefers primary then fallback locale`() {
        val ns = "mymod"
        val primary = "{\"block.mymod.stone\":\"Stone (zh)\"}"
        val fallback = "{\"block.mymod.stone\":\"Stone (en)\"}"
        val name = LangReader.chooseDisplayName(primary, fallback, "mymod:stone", "zh_cn", "en_us")
        assertThat(name).isEqualTo("Stone (zh)")
    }

    @Test fun `displayName falls back when primary is missing`() {
        val fallback = "{\"block.foo.stone\":\"Stone (en)\"}"
        val name = LangReader.chooseDisplayName(primaryJson = "{}", fallback, "foo:stone", "zh_cn", "en_us")
        assertThat(name).isEqualTo("Stone (en)")
    }

    @Test fun `displayName strips minecraft prefix block entries`() {
        val primary = "{\"block.minecraft.stone\":\"Stone\"}"
        val name = LangReader.chooseDisplayName(primary, "{}", "minecraft:stone", "zh_cn", "en_us")
        assertThat(name).isEqualTo("Stone")
    }

    @Test fun `displayName returns raw id when nothing matches`() {
        val name = LangReader.chooseDisplayName("{}", "{}", "mymod:nonexistent", "zh_cn", "en_us")
        assertThat(name).isEqualTo("mymod:nonexistent")
    }
}
