package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ActiveInstall
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResourcePacksScreenTest {

    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun emptyState_rendersHeroTitle_andAddModButton_andClearAll() {
        compose.setContent {
            ResourcePacksScreen(state = ResourcePacksState(), onAction = {})
        }
        // Hero title (content area, anchored at top).
        compose.onNodeWithText("资源包管理").assertExists()
        // Section headers — each appears exactly once.
        compose.onNodeWithText("原版资源包").assertExists()
        compose.onNodeWithText("Mod 资源").assertExists()
        // Empty-state hint for the Mod list.
        compose.onNodeWithText("还没有 Mod 资源，点击下方按钮添加").assertExists()
        // Primary CTA.
        compose.onNodeWithText("添加 Mod 资源").assertExists()
        // Clear-all (now a bottom button inside the LazyColumn).
        compose.onNodeWithText("清理全部").assertExists()
    }

    @Test fun installedVanilla_doesNotRemoveClearAllButton() {
        val s = ResourcePacksState(
            installed = listOf(vanillaEntry()),
        )
        compose.setContent { ResourcePacksScreen(state = s, onAction = {}) }
        compose.onAllNodesWithText("原版资源包").fetchSemanticsNodes().also { nodes ->
            assertThat(nodes).isNotEmpty()
        }
        // Still has destructive action even with installed entry.
        compose.onNodeWithText("清理全部").assertExists()
    }

    @Test fun installedMod_rendersModRow_andCountBadge() {
        val s = ResourcePacksState(
            installed = listOf(
                vanillaEntry(),
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
        compose.onNodeWithText("Create").assertExists()
        // Mod count badge ("1") shown in the section header.
        compose.onNodeWithText("1").assertExists()
    }

    @Test fun activeInstallBanner_isRenderedWhenActiveInstallExists() {
        val installId = ResourcePackId.mod("create")
        val s = ResourcePacksState(
            activeInstalls = mapOf(
                installId to ActiveInstall(
                    packId = installId,
                    displayName = "Create",
                    fileName = "create-1.0.jar",
                    totalSize = 5_242_880L,
                    progress = flowOf(
                        io.github.moxisuki.blockprint.cat.app.core.resourcepack.model
                            .PackProgress.Downloading("create-1.0.jar", 0.42f)
                    ),
                ),
            ),
        )
        compose.setContent { ResourcePacksScreen(state = s, onAction = {}) }
        // Banner shows the mod display name (in the active install card).
        // The mod isn't installed yet, so a second "Create" can appear once the
        // banner is shown. Either presence confirms the banner is up.
        compose.onAllNodesWithText("Create").fetchSemanticsNodes().also { nodes ->
            assertThat(nodes.isNotEmpty()).isTrue()
        }
    }

    @Test fun app_packageName_matches_manifest() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.moxisuki.blockprint.cat", ctx.packageName)
    }

    private fun vanillaEntry() = ResourcePackEntry(
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
    )
}

private const val PackProgressExtractionProbe = "create-1.0.jar"
