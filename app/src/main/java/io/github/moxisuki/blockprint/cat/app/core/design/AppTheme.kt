package io.github.moxisuki.blockprint.cat.app.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val viewModel: AppThemeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : AppThemeActions {
            override fun selectMode(mode: AppThemeMode) {
                viewModel.selectMode(mode)
            }

            override fun selectColorSource(colorSource: AppThemeColorSource) {
                viewModel.selectColorSource(colorSource)
            }

            override fun selectAccentColor(accentColor: AppThemeAccentColor) {
                viewModel.selectAccentColor(accentColor)
            }

            override fun selectSeedColor(color: Color) {
                viewModel.selectSeedColor(color)
            }
        }
    }

    val themeController = remember(
        state.mode,
        state.colorSource,
        state.seedColor,
        state.accentColor,
    ) {
        ThemeController(
            colorSchemeMode = state.mode.toColorSchemeMode(state.colorSource),
            keyColor = when (state.colorSource) {
                AppThemeColorSource.Custom -> state.seedColor
                AppThemeColorSource.Monet -> state.accentColor.color
                AppThemeColorSource.Default -> null
            },
        )
    }
    val systemDark = isSystemInDarkTheme()
    val darkTheme = state.mode.isDark(systemDark)

    MiuixTheme(themeController) {
        AppSystemBars(
            color = MiuixTheme.colorScheme.surface,
            darkIcons = !darkTheme,
        )
        CompositionLocalProvider(
            LocalAppThemeState provides state,
            LocalAppThemeActions provides actions,
        ) {
            MaterialTheme(
                typography = AppTypography,
                content = content,
            )
        }
    }
}

@Composable
fun PreviewAppTheme(content: @Composable () -> Unit) {
    val themeController = remember { ThemeController() }
    MiuixTheme(themeController) {
        MaterialTheme(
            typography = AppTypography,
            content = content,
        )
    }
}
