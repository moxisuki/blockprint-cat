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

    @Test fun `displayName checks locale candidates in order`() {
        val name = LangReader.chooseDisplayName(
            localizedJsons = listOf(
                "{\"block.mymod.stone\":\"石头\"}",
                "{\"block.mymod.stone\":\"Stone\"}",
            ),
            blockId = "mymod:stone",
        )
        assertThat(name).isEqualTo("石头")
    }

    @Test fun `displayName resolves item entries for mod namespaces`() {
        val name = LangReader.chooseDisplayName(
            localizedJsons = listOf("{\"item.create.wrench\":\"扳手\"}"),
            blockId = "create:wrench",
        )
        assertThat(name).isEqualTo("扳手")
    }
}
