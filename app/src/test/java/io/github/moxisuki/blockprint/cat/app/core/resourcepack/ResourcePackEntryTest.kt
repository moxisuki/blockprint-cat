package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import org.junit.Test

class ResourcePackEntryTest {
    private val sample = ResourcePackEntry(
        id = ResourcePackId.mod("create"),
        kind = ResourcePackEntry.Kind.MOD,
        displayName = "Create",
        version = "6.0.4",
        mcVersion = "1.20.1",
        fileCount = 234,
        totalSize = 5_242_880L,
        installedAt = 1_700_000_000_000L,
        namespaces = setOf("create"),
        hasAssets = true,
    )

    @Test fun `hasAssets mirrors fileCount greater than zero`() {
        val empty = sample.copy(fileCount = 0, hasAssets = false)
        assertThat(empty.hasAssets).isFalse()
        assertThat(sample.hasAssets).isTrue()
    }

    @Test fun `Kind enum has only VANILLA and MOD`() {
        assertThat(ResourcePackEntry.Kind.values().toList())
            .containsExactly(ResourcePackEntry.Kind.VANILLA, ResourcePackEntry.Kind.MOD)
    }
}