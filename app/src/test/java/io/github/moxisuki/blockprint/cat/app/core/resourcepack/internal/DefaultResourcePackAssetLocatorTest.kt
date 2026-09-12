package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DefaultResourcePackAssetLocatorTest {

    private lateinit var renderRoot: File

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // Use the standard app filesDir/render_assets path. Robolectric's ApplicationProvider
        // returns a fresh in-memory app context for each test, so this is naturally isolated.
        renderRoot = File(ctx.filesDir, "blockprintcat/render_assets")
        renderRoot.deleteRecursively()
        renderRoot.mkdirs()
    }

    @After fun tearDown() {
        renderRoot.deleteRecursively()
    }

    private fun newLocator(): DefaultResourcePackAssetLocator {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        return DefaultResourcePackAssetLocator(ctx)
    }

    @Test fun `installedNamespaces returns empty when no subdirectories exist`() {
        assertThat(newLocator().installedNamespaces()).isEmpty()
        assertThat(newLocator().hasAssets("minecraft")).isFalse()
    }

    @Test fun `installedNamespaces enumerates namespaces with at least one file`() {
        File(renderRoot, "minecraft/textures/block").mkdirs()
        File(renderRoot, "minecraft/textures/block/stone.png").writeBytes(byteArrayOf(1))
        File(renderRoot, "create/textures/block").mkdirs()
        File(renderRoot, "create/textures/block/shaft.png").writeBytes(byteArrayOf(1))

        val locator = newLocator()
        val namespaces = locator.installedNamespaces()
        assertThat(namespaces).containsAtLeast("minecraft", "create")
        assertThat(locator.hasAssets("minecraft")).isTrue()
        assertThat(locator.hasAssets("create")).isTrue()
        assertThat(locator.hasAssets("missing")).isFalse()
    }

    @Test fun `namespaceInfo reports namespace capabilities`() {
        File(renderRoot, "minecraft/blockstates").mkdirs()
        File(renderRoot, "minecraft/blockstates/stone.json").writeText("{}")
        File(renderRoot, "minecraft/models/block").mkdirs()
        File(renderRoot, "minecraft/models/block/stone.json").writeText("{}")
        File(renderRoot, "minecraft/textures/block").mkdirs()
        File(renderRoot, "minecraft/textures/block/stone.png").writeBytes(byteArrayOf(1))

        val info = newLocator().namespaceInfo("minecraft")
        assertThat(info).isNotNull()
        assertThat(info!!.namespace).isEqualTo("minecraft")
        assertThat(info.hasBlockStates).isTrue()
        assertThat(info.hasModels).isTrue()
        assertThat(info.hasTextures).isTrue()
    }

    @Test fun `asset path helpers resolve block resources`() {
        File(renderRoot, "minecraft/blockstates").mkdirs()
        File(renderRoot, "minecraft/blockstates/stone.json").writeText("{}")
        File(renderRoot, "minecraft/models/block").mkdirs()
        File(renderRoot, "minecraft/models/block/stone.json").writeText("{}")
        File(renderRoot, "minecraft/textures/block").mkdirs()
        File(renderRoot, "minecraft/textures/block/stone.png").writeBytes(byteArrayOf(1))

        val locator = newLocator()
        assertThat(locator.blockstatePath("minecraft:stone")?.relativePath)
            .isEqualTo("blockstates/stone.json")
        assertThat(locator.blockModelPath("minecraft", "block/stone")?.relativePath)
            .isEqualTo("models/block/stone.json")
        assertThat(locator.texturePath("minecraft", "block/stone")?.relativePath)
            .isEqualTo("textures/block/stone.png")
    }

    @Test fun `textureCandidates ranks by stem-equality descending`() {
        File(renderRoot, "minecraft/textures/block").mkdirs()
        File(renderRoot, "minecraft/textures/block/stone.png").writeBytes(byteArrayOf(0))
        File(renderRoot, "minecraft/textures/block/stone_brick.png").writeBytes(byteArrayOf(0))
        File(renderRoot, "minecraft/textures/block/deepslate.png").writeBytes(byteArrayOf(0))
        val candidates = newLocator().textureCandidates("minecraft:stone", maxResults = 2)
        assertThat(candidates).hasSize(2)
        // exact match ("stone") outranks substring match ("stone_brick")
        assertThat(candidates.first().nameWithoutExtension).isEqualTo("stone")
    }

    @Test fun `textureCandidates returns empty when namespace absent`() {
        assertThat(newLocator().textureCandidates("minecraft:stone")).isEmpty()
    }

    @Test fun `loadDisplayName reads the requested locale from a mod lang file`() {
        File(renderRoot, "create/lang/zh_cn.json").apply {
            requireNotNull(parentFile).mkdirs()
            writeText("{\"item.create.wrench\":\"扳手\"}")
        }

        val name = kotlinx.coroutines.runBlocking {
            newLocator().loadDisplayName("create:wrench", listOf("zh_cn", "en_us"))
        }
        assertThat(name).isEqualTo("扳手")
    }
}
