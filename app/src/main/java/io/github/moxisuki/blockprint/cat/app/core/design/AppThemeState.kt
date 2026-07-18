package io.github.moxisuki.blockprint.cat.app.core.design

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Stable
class AppThemeState internal constructor(
    private val modeState: MutableState<AppThemeMode>,
    private val colorSourceState: MutableState<AppThemeColorSource>,
    private val seedColorArgbState: MutableState<Int>,
) {
    var mode: AppThemeMode
        get() = modeState.value
        private set(value) {
            modeState.value = value
        }

    var colorSource: AppThemeColorSource
        get() = colorSourceState.value
        private set(value) {
            colorSourceState.value = value
        }

    var seedColor: Color
        get() = Color(seedColorArgbState.value)
        private set(value) {
            seedColorArgbState.value = value.copy(alpha = 1f).toArgb()
        }

    fun selectMode(mode: AppThemeMode) {
        this.mode = mode
    }

    fun selectColorSource(colorSource: AppThemeColorSource) {
        this.colorSource = colorSource
    }

    fun selectSeedColor(color: Color) {
        seedColor = color
        colorSource = AppThemeColorSource.Custom
    }
}

val LocalAppThemeState = staticCompositionLocalOf<AppThemeState> {
    error("No AppThemeState provided")
}
