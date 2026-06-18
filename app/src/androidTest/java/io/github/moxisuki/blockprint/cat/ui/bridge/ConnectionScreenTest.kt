package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun disconnected_state_shows_scan_and_manual_buttons() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionCard(
                    state = BridgeState(),
                    scanning = false,
                    discoveries = emptyList(),
                    onScan = {},
                    onStopScan = {},
                    onManual = {},
                    onConnectDiscovery = { _, _, _ -> },
                    onDisconnect = {},
                    onDismissError = {},
                )
            }
        }
        composeTestRule.onNodeWithText("扫描").assertIsDisplayed()
        composeTestRule.onNodeWithText("手动输入连接").assertIsDisplayed()
    }

    @Test
    fun scanning_state_shows_stop_scan_button() {
        composeTestRule.setContent {
            MaterialTheme {
                SessionCard(
                    state = BridgeState(),
                    scanning = true,
                    discoveries = emptyList(),
                    onScan = {},
                    onStopScan = {},
                    onManual = {},
                    onConnectDiscovery = { _, _, _ -> },
                    onDisconnect = {},
                    onDismissError = {},
                )
            }
        }
        composeTestRule.onNodeWithText("停止扫描").assertIsDisplayed()
        composeTestRule.onNodeWithText("扫描中…").assertIsDisplayed()
    }

    @Test
    fun empty_paired_section_shows_empty_message() {
        composeTestRule.setContent {
            MaterialTheme {
                PairedDevicesSection(devices = emptyList(), onConnect = {}, onDelete = {}, onRename = { _, _ -> })
            }
        }
        composeTestRule.onNodeWithText(
            "暂无配对设备，扫描或手动连接后会自动保存",
        ).assertIsDisplayed()
    }

    @Test
    fun empty_event_log_shows_empty_message() {
        composeTestRule.setContent {
            MaterialTheme {
                EventLogSection(events = emptyList(), onClear = {})
            }
        }
        composeTestRule.onNodeWithText("暂无事件").assertIsDisplayed()
    }

    @Test
    fun manual_panel_default_collapsed() {
        composeTestRule.setContent {
            MaterialTheme {
                ManualConnectPanel(disabled = false, onConnect = { _, _, _ -> })
            }
        }
        composeTestRule.onNodeWithText("Host").assertDoesNotExist()
        composeTestRule.onNodeWithText("手动输入连接").performClick()
        composeTestRule.onNodeWithText("Host").assertIsDisplayed()
    }
}