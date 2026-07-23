package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import org.junit.Test

class ResourcePackIdTest {
    @Test fun `vanilla constant reports isVanilla true`() {
        assertThat(ResourcePackId.Vanilla.isVanilla).isTrue()
        assertThat(ResourcePackId.Vanilla.modSlug).isEqualTo("")
    }

    @Test fun `mod factory encodes slug under mod prefix`() {
        val id = ResourcePackId.mod("create")
        assertThat(id.value).isEqualTo("mod:create")
        assertThat(id.isVanilla).isFalse()
        assertThat(id.modSlug).isEqualTo("create")
    }

    @Test fun `slug with hyphen survives encode-decode round trip`() {
        val id = ResourcePackId.mod("mc-walled-classic-1")
        assertThat(id.modSlug).isEqualTo("mc-walled-classic-1")
    }
}