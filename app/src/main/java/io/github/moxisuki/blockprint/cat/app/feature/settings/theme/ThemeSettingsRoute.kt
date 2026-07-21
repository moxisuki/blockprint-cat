package io.github.moxisuki.blockprint.cat.app.feature.settings.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeColorSource
import io.github.moxisuki.blockprint.cat.app.core.design.LocalAppThemeActions
import io.github.moxisuki.blockprint.cat.app.core.design.LocalAppThemeState

@Composable
fun ThemeSettingsRoute(
    modifier: Modifier = Modifier,
    onAppBarTitleVisibleChange: (Boolean) -> Unit = {},
) {
    val state = LocalAppThemeState.current
    val actions = LocalAppThemeActions.current
    DisposableEffect(onAppBarTitleVisibleChange) {
        onDispose {
            onAppBarTitleVisibleChange(false)
        }
    }

    ThemeSettingsScreen(
        state = state,
        onAppBarTitleVisibleChange = onAppBarTitleVisibleChange,
        onModeSelected = actions::selectMode,
        onMonetEnabledChange = { enabled ->
            actions.selectColorSource(
                if (enabled) {
                    AppThemeColorSource.Monet
                } else {
                    AppThemeColorSource.Default
                },
            )
        },
        onAccentColorSelected = actions::selectAccentColor,
        onCustomColorEnabledChange = { enabled ->
            if (enabled) {
                actions.selectSeedColor(state.seedColor)
            } else {
                actions.selectColorSource(AppThemeColorSource.Default)
            }
        },
        onSeedColorChanged = actions::selectSeedColor,
        modifier = modifier,
    )
}
