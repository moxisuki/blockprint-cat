package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
import org.junit.Test

class ModModelTest {
    @Test fun `ModSearchHit exposes slug and title`() {
        val h = ModSearchHit(slug = "create", title = "Create", description = "mod", projectId = "abc")
        assertThat(h.slug).isEqualTo("create")
        assertThat(h.title).isEqualTo("Create")
    }

    @Test fun `ModVersionInfo preserves mc version list and size`() {
        val v = ModVersionInfo(id = "v1", name = "1.0", gameVersions = listOf("1.20.1"), fileName = "create-1.0.jar", fileSize = 1234L, fileUrl = "https://example.test/create-1.0.jar")
        assertThat(v.gameVersions).containsExactly("1.20.1")
        assertThat(v.fileSize).isEqualTo(1234L)
    }
}