package io.github.moxisuki.blockprint.cat.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "litematic_theme"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_CUSTOM_PRIMARY = "custom_primary"
        private const val KEY_CUSTOM_SECONDARY = "custom_secondary"
        const val MODE_SYSTEM = 0
        const val MODE_LIGHT = 1
        const val MODE_DARK = 2
    }

    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    init {
        _themeState.value = ThemeState(
            mode = prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM),
            customPrimary = prefs.getString(KEY_CUSTOM_PRIMARY, "") ?: "",
            customSecondary = prefs.getString(KEY_CUSTOM_SECONDARY, "") ?: "",
        )
    }

    data class ThemeState(
        val mode: Int = MODE_SYSTEM,
        val customPrimary: String = "",
        val customSecondary: String = "",
    )

    var themeMode: Int
        get() = _themeState.value.mode
        set(value) {
            prefs.edit().putInt(KEY_THEME_MODE, value).apply()
            _themeState.value = _themeState.value.copy(mode = value)
        }

    var customPrimary: String
        get() = _themeState.value.customPrimary
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_PRIMARY, value).apply()
            _themeState.value = _themeState.value.copy(customPrimary = value)
        }

    var customSecondary: String
        get() = _themeState.value.customSecondary
        set(value) {
            prefs.edit().putString(KEY_CUSTOM_SECONDARY, value).apply()
            _themeState.value = _themeState.value.copy(customSecondary = value)
        }

    val presets: List<ThemePreset> = listOf(
        ThemePreset("薄荷绿", Color(0xFFA5D6A7), Color(0xFF388E3C)),
        ThemePreset("樱花粉", Color(0xFFF8BBD9), Color(0xFFE91E63)),
        ThemePreset("天空蓝", Color(0xFF81D4FA), Color(0xFF0288D1)),
        ThemePreset("柠檬黄", Color(0xFFFFF59D), Color(0xFFFBC02D)),
        ThemePreset("暮光紫", Color(0xFFCE93D8), Color(0xFF7B1FA2)),
        ThemePreset("珊瑚橙", Color(0xFFFFAB91), Color(0xFFE64A19)),
        ThemePreset("冰川蓝", Color(0xFFB3E5FC), Color(0xFF00ACC1)),
        ThemePreset("森林绿", Color(0xFFA5D6A7), Color(0xFF2E7D32)),
    )

    fun parseColor(hex: String): Color? {
        if (hex.isBlank()) return null
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            null
        }
    }

    fun colorToHex(color: Color): String {
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        return "#%02X%02X%02X".format(r, g, b)
    }

    fun currentColors(): Pair<Color, Color> {
        val state = _themeState.value
        // 首次启动 / 解析失败时落到薄荷绿（与 presets[0] 保持一致，显式写出来更稳）
        val p = parseColor(state.customPrimary) ?: Color(0xFFA5D6A7)
        val s = parseColor(state.customSecondary) ?: Color(0xFF388E3C)
        return p to s
    }

    fun findPresetIndex(): Int {
        val state = _themeState.value
        val pHex = state.customPrimary
        val sHex = state.customSecondary
        if (pHex.isBlank() && sHex.isBlank()) return 0
        presets.forEachIndexed { index, preset ->
            if (colorToHex(preset.primary) == pHex && colorToHex(preset.secondary) == sHex) {
                return index
            }
        }
        return -1
    }

    fun colorSchemeFor(dark: Boolean): ColorScheme {
        val (primary, secondary) = currentColors()
        val surfaceColor = if (dark) Color(0xFF1E1E1E) else Color.White
        val backgroundColor = surfaceColor
        val surfaceVariant = if (dark) Color(0xFF2C2C2C) else Color(0xFFE8E8E8)

        return if (dark) {
            darkColorScheme(
                primary = primary, onPrimary = Color.White,
                primaryContainer = primary.copy(alpha = 0.6f), onPrimaryContainer = Color.White,
                secondary = secondary, onSecondary = Color.White,
                secondaryContainer = secondary.copy(alpha = 0.6f), onSecondaryContainer = Color.White,
                tertiary = secondary.copy(alpha = 0.7f), onTertiary = Color.White,
                background = backgroundColor, onBackground = Color(0xFFE0E0E0),
                surface = surfaceColor, onSurface = Color(0xFFE0E0E0),
                surfaceVariant = surfaceVariant, onSurfaceVariant = Color(0xFFBDBDBD),
                error = Color(0xFFCF6679), onError = Color.Black,
            )
        } else {
            lightColorScheme(
                primary = primary, onPrimary = Color.White,
                primaryContainer = primary.copy(alpha = 0.10f), onPrimaryContainer = primary,
                secondary = secondary, onSecondary = Color.White,
                secondaryContainer = secondary.copy(alpha = 0.10f), onSecondaryContainer = secondary,
                tertiary = secondary.copy(alpha = 0.8f), onTertiary = Color.White,
                background = backgroundColor, onBackground = Color(0xFF1C1C1C),
                surface = surfaceColor, onSurface = Color(0xFF1C1C1C),
                surfaceVariant = surfaceVariant, onSurfaceVariant = Color(0xFF616161),
                error = Color(0xFFB00020), onError = Color.White,
            )
        }
    }
}

data class ThemePreset(
    val name: String,
    val primary: Color,
    val secondary: Color,
)
