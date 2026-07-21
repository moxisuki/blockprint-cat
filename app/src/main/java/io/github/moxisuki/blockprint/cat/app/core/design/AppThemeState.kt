package io.github.moxisuki.blockprint.cat.app.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppThemeState(
    val mode: AppThemeMode = AppThemeMode.System,
    val colorSource: AppThemeColorSource = AppThemeColorSource.Default,
    val accentColor: AppThemeAccentColor = AppThemeAccentColor.Default,
    val seedColor: Color = AppDefaultThemeSeedColor,
)

val LocalAppThemeState = staticCompositionLocalOf<AppThemeState> {
    error("No AppThemeState provided")
}

@Stable
interface AppThemeActions {
    fun selectMode(mode: AppThemeMode)

    fun selectColorSource(colorSource: AppThemeColorSource)

    fun selectAccentColor(accentColor: AppThemeAccentColor)

    fun selectSeedColor(color: Color)
}

val LocalAppThemeActions = staticCompositionLocalOf<AppThemeActions> {
    error("No AppThemeActions provided")
}
