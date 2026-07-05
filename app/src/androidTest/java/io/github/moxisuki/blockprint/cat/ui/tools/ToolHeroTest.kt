package io.github.moxisuki.blockprint.cat.ui.tools

import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class ToolHeroTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renders_title_and_subtitle() {
        val entry = ToolCatalog.entries.first { it.kind == ToolKind.Hero }

        composeTestRule.setContent {
            Surface {
                ToolHero(entry = entry, onClick = {})
            }
        }

        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(entry.titleRes))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(entry.subtitleRes!!))
            .assertIsDisplayed()
    }

    @Test
    fun click_invokes_onClick() {
        val entry = ToolCatalog.entries.first { it.kind == ToolKind.Hero }
        var clicked = false

        composeTestRule.setContent {
            Surface {
                ToolHero(entry = entry, onClick = { clicked = true })
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                composeTestRule.activity.getString(entry.titleRes)
            )
            .performClick()

        assertThat(clicked).isTrue()
    }
}
