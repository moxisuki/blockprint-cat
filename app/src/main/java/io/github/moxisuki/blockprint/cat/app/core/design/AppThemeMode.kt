package io.github.moxisuki.blockprint.cat.app.core.design

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

enum class AppThemeMode {
    System,
    Light,
    Dark,
}

enum class AppThemeColorSource {
    Default,
    Monet,
    Custom,
}

internal fun AppThemeMode.toColorSchemeMode(colorSource: AppThemeColorSource): ColorSchemeMode = when (colorSource) {
    AppThemeColorSource.Default -> when (this) {
        AppThemeMode.System -> ColorSchemeMode.System
        AppThemeMode.Light -> ColorSchemeMode.Light
        AppThemeMode.Dark -> ColorSchemeMode.Dark
    }

    AppThemeColorSource.Monet,
    AppThemeColorSource.Custom,
    -> when (this) {
        AppThemeMode.System -> ColorSchemeMode.MonetSystem
        AppThemeMode.Light -> ColorSchemeMode.MonetLight
        AppThemeMode.Dark -> ColorSchemeMode.MonetDark
    }
}

internal val AppDefaultThemeSeedColor = Color(0xFF3482FF)

internal fun AppThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    AppThemeMode.System -> systemDark
    AppThemeMode.Light -> false
    AppThemeMode.Dark -> true
}
