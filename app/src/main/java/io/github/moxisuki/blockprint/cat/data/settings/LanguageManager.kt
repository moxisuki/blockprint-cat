package io.github.moxisuki.blockprint.cat.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor(@ApplicationContext context: Context) {

    enum class Mode { SYSTEM, ZH_CN, EN }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(loadMode())
    val mode: StateFlow<Mode> = _mode.asStateFlow()

    fun setMode(mode: Mode) {
        prefs.edit { putString(KEY_MODE, mode.name) }
        val tag = when (mode) {
            Mode.SYSTEM -> ""
            Mode.ZH_CN -> "zh-CN"
            Mode.EN -> "en"
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        _mode.value = mode
    }

    private fun loadMode(): Mode {
        val name = prefs.getString(KEY_MODE, Mode.SYSTEM.name) ?: Mode.SYSTEM.name
        return runCatching { Mode.valueOf(name) }.getOrDefault(Mode.SYSTEM)
    }

    companion object {
        private const val PREFS_NAME = "language_pref"
        private const val KEY_MODE = "mode"
    }
}
