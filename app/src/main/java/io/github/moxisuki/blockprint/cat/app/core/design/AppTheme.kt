package io.github.moxisuki.blockprint.cat.app.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.toArgb
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val themeModeState = rememberSaveable { mutableStateOf(AppThemeMode.System) }
    val colorSourceState = rememberSaveable { mutableStateOf(AppThemeColorSource.Default) }
    val seedColorArgbState = rememberSaveable { mutableStateOf(AppDefaultThemeSeedColor.toArgb()) }
    val appThemeState = remember(themeModeState, colorSourceState, seedColorArgbState) {
        AppThemeState(
            modeState = themeModeState,
            colorSourceState = colorSourceState,
            seedColorArgbState = seedColorArgbState,
        )
    }
    val themeController = remember(appThemeState.mode, appThemeState.colorSource, appThemeState.seedColor) {
        ThemeController(
            colorSchemeMode = appThemeState.mode.toColorSchemeMode(appThemeState.colorSource),
            keyColor = appThemeState.seedColor.takeIf {
                appThemeState.colorSource == AppThemeColorSource.Custom
            },
        )
    }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = appThemeState.mode.isDark(systemDark)

    MiuixTheme(themeController) {
        AppSystemBars(
            color = MiuixTheme.colorScheme.surface,
            darkIcons = !darkTheme,
        )
        CompositionLocalProvider(LocalAppThemeState provides appThemeState) {
            MaterialTheme(
                typography = AppTypography,
                content = content,
            )
        }
    }
}
