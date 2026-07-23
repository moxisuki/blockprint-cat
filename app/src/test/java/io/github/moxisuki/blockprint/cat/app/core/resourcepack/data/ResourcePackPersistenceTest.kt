package io.github.moxisuki.blockprint.cat.app.core.resourcepack.data

import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import org.junit.Test

class ResourcePackPersistenceTest {
    @Test fun `round trip preserves every field`() {
        val entry = ResourcePackPersistence.sample().copy()
        val entity = entry.toEntity()
        val back = entity.toDomain()
        assertThat(back).isEqualTo(entry)
    }

    @Test fun `namespaces encoded as comma separated without spaces`() {
        val entity = ResourcePackPersistence.sample().toEntity()
        assertThat(entity.namespaces).isEqualTo("create,create_connected")
    }

    @Test fun `kind round trips between entity and domain`() {
        val v = ResourcePackPersistence.sample().toEntity()
        assertThat(v.kind).isEqualTo("mod")
        assertThat(v.toDomain().kind).isEqualTo(ResourcePackEntry.Kind.MOD)
    }
}
