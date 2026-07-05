package io.github.moxisuki.blockprint.cat.ui.tools

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class ToolsContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renders_hero_with_subtitle() {
        val hero = ToolCatalog.entries.first { it.kind == ToolKind.Hero }

        composeTestRule.setContent {
            Surface {
                ToolsContent(tools = ToolCatalog.entries, onToolClick = {})
            }
        }

        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(hero.titleRes))
            .assertExists()
    }

    @Test
    fun renders_every_list_row_title() {
        composeTestRule.setContent {
            Surface {
                ToolsContent(tools = ToolCatalog.entries, onToolClick = {})
            }
        }

        ToolCatalog.entries
            .filter { it.kind == ToolKind.ListItem }
            .forEach { entry ->
                composeTestRule
                    .onNodeWithText(composeTestRule.activity.getString(entry.titleRes))
                    .assertExists()
            }
    }

    @Test
    fun click_on_list_row_invokes_callback_with_entry() {
        val firstList = ToolCatalog.entries.first { it.kind == ToolKind.ListItem }
        var clickedId: String? = null

        composeTestRule.setContent {
            Surface {
                ToolsContent(
                    tools = ToolCatalog.entries,
                    onToolClick = { clickedId = it.id },
                )
            }
        }

        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(firstList.titleRes))
            .performClick()

        assertThat(clickedId).isEqualTo(firstList.id)
    }
}
