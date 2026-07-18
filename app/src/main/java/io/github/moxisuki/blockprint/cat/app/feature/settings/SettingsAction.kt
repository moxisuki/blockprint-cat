package io.github.moxisuki.blockprint.cat.app.feature.settings

import androidx.compose.ui.graphics.Color
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeColorSource
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeMode
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguage

sealed interface SettingsAction {
    data object Opened : SettingsAction
    data object AboutClicked : SettingsAction
    data class ThemeModeSelected(val mode: AppThemeMode) : SettingsAction
    data class ThemeColorSourceSelected(val colorSource: AppThemeColorSource) : SettingsAction
    data class ThemeSeedColorSelected(val color: Color) : SettingsAction
    data class ThemePaletteExpansionChanged(val expanded: Boolean) : SettingsAction
    data class LanguageSelected(val language: AppLanguage) : SettingsAction
}
