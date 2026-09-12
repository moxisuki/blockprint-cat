package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal.AssetExtractor.extractFromJar
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AssetExtractorTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun `extractFromJar keeps assets minecraft whitelisted prefixes and drops noise`() {
        val jar = java.io.File("src/test/resources/resourcepack/fixtures/minecraft.jar")
        val dest = tmp.newFolder("dest")
        val result = extractFromJar(jar, dest, namespaceFilter = { it == "minecraft" })
        assertThat(result.fileCount).isEqualTo(4)
        assertThat(result.namespaces).containsExactly("minecraft")
        assertThat(java.io.File(dest, "minecraft/models/block/stone.json").isFile).isTrue()
        assertThat(java.io.File(dest, "minecraft/sounds/foo.ogg").exists()).isFalse()
        assertThat(java.io.File(dest, "create/models/block/shaft.json").exists()).isFalse()
    }

    @Test fun `extractFromModJar skips minecraft namespace`() {
        val jar = java.io.File("src/test/resources/resourcepack/fixtures/minecraft.jar")
        val dest = tmp.newFolder("dest")
        val result = AssetExtractor.extractFromModJar(jar, dest)
        assertThat(result.namespaces).containsExactly("create")
        assertThat(result.fileCount).isEqualTo(1)
        assertThat(java.io.File(dest, "create/models/block/shaft.json").isFile).isTrue()
        assertThat(java.io.File(dest, "minecraft/models/block/stone.json").exists()).isFalse()
    }
}
