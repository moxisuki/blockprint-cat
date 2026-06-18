package io.github.moxisuki.blockprint.cat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.moxisuki.blockprint.cat.data.ThemeManager

@Composable
fun BlockPrintCatTheme(
    themeManager: ThemeManager,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeState by themeManager.themeState.collectAsState()

    val effectiveDark = when (themeState.mode) {
        ThemeManager.MODE_LIGHT -> false
        ThemeManager.MODE_DARK -> true
        else -> darkTheme
    }

    val colorScheme = themeManager.colorSchemeFor(effectiveDark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
