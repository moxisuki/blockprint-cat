package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResourcePacksScreenTest {

    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun emptyState_rendersSectionTitles_andAddModButton_andDeleteAll() {
        compose.setContent {
            ResourcePacksScreen(state = ResourcePacksState(), onAction = {})
        }
        // "原版资源包"/"Mod 资源" appear at minimum once (in the LazyColumn section
        // header); VanillaPackCard also renders the vanilla title for self-containment.
        assertThat(compose.onAllNodesWithText("原版资源包").fetchSemanticsNodes().isNotEmpty()).isTrue()
        compose.onNodeWithText("Mod 资源").assertExists()
        compose.onNodeWithText("添加 Mod 资源").assertExists()
        compose.onNodeWithText("清理全部").assertExists()
    }

    @Test fun installedVanilla_rendersVanilla_section_title_when_entry_present() {
        val s = ResourcePacksState(
            installed = listOf(
                ResourcePackEntry(
                    id = ResourcePackId.Vanilla,
                    kind = ResourcePackEntry.Kind.VANILLA,
                    displayName = "Minecraft 原版",
                    version = "1.21.4",
                    mcVersion = null,
                    fileCount = 1234,
                    totalSize = 56_000_000L,
                    installedAt = 1_700_000_000_000L,
                    namespaces = setOf("minecraft"),
                    hasAssets = true,
                ),
            ),
        )
        compose.setContent { ResourcePacksScreen(state = s, onAction = {}) }
        // "原版资源包" appears twice in screen (LazyColumn section + VanillaPackCard).
        // Just verify it is rendered at least once.
        assertThat(compose.onAllNodesWithText("原版资源包").fetchSemanticsNodes().isNotEmpty()).isTrue()
    }

    @Test fun installedModCard_rendersModTitle() {
        val s = ResourcePacksState(
            installed = listOf(
                ResourcePackEntry(
                    id = ResourcePackId.mod("create"),
                    kind = ResourcePackEntry.Kind.MOD,
                    displayName = "Create",
                    version = "6.0.4",
                    mcVersion = "1.20.1",
                    fileCount = 234,
                    totalSize = 5_242_880L,
                    installedAt = 1L,
                    namespaces = setOf("create"),
                    hasAssets = true,
                ),
            ),
        )
        compose.setContent { ResourcePacksScreen(state = s, onAction = {}) }
        compose.onNodeWithText("Mod 资源").assertExists()
        compose.onNodeWithText("添加 Mod 资源").assertExists()
    }

    @Test fun app_packageName_matches_manifest() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.moxisuki.blockprint.cat", ctx.packageName)
    }
}
