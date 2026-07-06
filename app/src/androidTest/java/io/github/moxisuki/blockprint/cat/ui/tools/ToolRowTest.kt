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

class ToolRowTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renders_title() {
        val entry = ToolCatalog.entries.first { it.kind == ToolKind.ListItem }

        composeTestRule.setContent {
            Surface {
                ToolRow(entry = entry, onClick = {})
            }
        }

        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(entry.titleRes))
            .assertIsDisplayed()
    }

    @Test
    fun click_invokes_onClick() {
        val entry = ToolCatalog.entries.first { it.kind == ToolKind.ListItem }
        var clicked = false

        composeTestRule.setContent {
            Surface {
                ToolRow(entry = entry, onClick = { clicked = true })
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
