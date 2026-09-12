package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.PackProgress
import org.junit.Test

class PackProgressTest {
    @Test fun `Downloading percentage is between 0 and 1`() {
        val p = PackProgress.Downloading("foo.jar", 0.5f)
        assertThat(p.fraction).isAtLeast(0f)
        assertThat(p.fraction).isAtMost(1f)
    }

    @Test fun `Installing carries install progress`() {
        val p = PackProgress.Installing(
            label = "minecraft/models/block/stone.json",
            fraction = 0.25f,
            installedFiles = 1,
            totalFiles = 4,
        )
        assertThat(p.installedFiles).isEqualTo(1)
        assertThat(p.totalFiles).isEqualTo(4)
        assertThat(p.fraction).isEqualTo(0.25f)
    }

    @Test fun `Done carries an immutable entry reference`() {
        val entry = io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry(
            id = io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId.Vanilla,
            kind = io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry.Kind.VANILLA,
            displayName = "Minecraft",
            version = "1.21",
            mcVersion = null,
            fileCount = 10,
            totalSize = 100L,
            installedAt = 0L,
            namespaces = setOf("minecraft"),
            hasAssets = true,
        )
        val done = PackProgress.Done(entry)
        assertThat(done.entry.displayName).isEqualTo("Minecraft")
    }
}
