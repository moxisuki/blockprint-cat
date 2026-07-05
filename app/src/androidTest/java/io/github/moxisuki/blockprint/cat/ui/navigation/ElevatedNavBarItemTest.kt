package io.github.moxisuki.blockprint.cat.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Surface
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class ElevatedNavBarItemTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun renders_icon_with_label() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    NavigationBar {
                        ElevatedNavBarItem(
                            selected = false,
                            onClick = {},
                            icon = Icons.Outlined.Computer,
                            selectedIcon = Icons.Filled.Computer,
                            label = "Connection",
                        )
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Connection")
            .assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun click_invokes_onClick() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                Surface {
                    NavigationBar {
                        ElevatedNavBarItem(
                            selected = false,
                            onClick = { clicked = true },
                            icon = Icons.Outlined.Computer,
                            selectedIcon = Icons.Filled.Computer,
                            label = "Connection",
                        )
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Connection")
            .performClick()

        assertThat(clicked).isTrue()
    }
}