package io.github.moxisuki.blockprint.cat.app.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.app.core.design.LocalAppThemeState

@Composable
fun SettingsRoute(
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appThemeState = LocalAppThemeState.current
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    SettingsScreen(
        state = state,
        selectedThemeMode = appThemeState.mode,
        selectedThemeColorSource = appThemeState.colorSource,
        themeSeedColor = appThemeState.seedColor,
        selectedLanguage = selectedLanguage,
        onAction = { action ->
            when (action) {
                SettingsAction.AboutClicked -> onAboutClick()
                is SettingsAction.ThemeModeSelected -> appThemeState.selectMode(action.mode)
                is SettingsAction.ThemeColorSourceSelected -> appThemeState.selectColorSource(action.colorSource)
                is SettingsAction.ThemeSeedColorSelected -> appThemeState.selectSeedColor(action.color)
                is SettingsAction.LanguageSelected,
                SettingsAction.Opened,
                is SettingsAction.ThemePaletteExpansionChanged,
                -> Unit
            }
            viewModel.onAction(action)
        },
        modifier = modifier,
    )
}
