package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.ResourcePackEntity
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import io.github.moxisuki.blockprint.cat.app.testfakes.FakeResourcePackDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultResourcePackRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun newRepo(initial: List<ResourcePackEntity> = emptyList()): DefaultResourcePackRepository {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val renderDir = java.io.File(ctx.filesDir, "blockprintcat/render_assets")
        if (renderDir.exists()) renderDir.deleteRecursively()
        return DefaultResourcePackRepository(
            dao = FakeResourcePackDao(initial),
            context = ctx,
            vanillaInstaller = null,
            modInstaller = null,
            modrinthSource = null,
        )
    }

    @Test fun `deleteAll clears the dao state and emits empty list`() = runTest {
        val initial = listOf(
            ResourcePackEntity(
                id = "vanilla", kind = "vanilla", projectSlug = "",
                displayName = "Vanilla", versionName = "1.21", mcVersion = null,
                fileCount = 10, totalSize = 100L, installedAt = 1L,
                namespaces = "minecraft", source = "mojang", sourceVersionId = null,
            ),
        )
        val repo = newRepo(initial)
        assertThat(repo.installedPacks.first()).hasSize(1)
        repo.deleteAll()
        assertThat(repo.installedPacks.first()).isEmpty()
    }

    @Test fun `searchMods returns empty when modrinthSource is null`() = runTest {
        val repo = newRepo()
        assertThat(repo.searchMods("anything")).isEmpty()
    }

    @Test fun `fetchModVersions returns empty when search returns nothing`() = runTest {
        val repo = newRepo()
        assertThat(repo.fetchModVersions("unknown-slug", mcVersion = null)).isEmpty()
    }

    @Test fun `cancel with no active install is a no-op`() {
        val repo = newRepo()
        // Should not throw.
        repo.cancel(ResourcePackId.Vanilla)
        repo.cancel(ResourcePackId.mod("any"))
    }
}
