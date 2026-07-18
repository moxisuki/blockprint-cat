package io.github.moxisuki.blockprint.cat.app.core.locale

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLanguageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 启动时快照保存系统原生语言，后续不会被 setApplicationLocales 污染 */
    private val systemLocale: Locale = run {
        val config = Resources.getSystem().configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales[0]
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }
    }

    val isSystemChinese: Boolean = systemLocale.language == Locale.CHINESE.language

    fun getLanguage(): AppLanguage {
        val name = prefs.getString(KEY_LANGUAGE, null) ?: return AppLanguage.System
        return runCatching { AppLanguage.valueOf(name) }.getOrDefault(AppLanguage.System)
    }

    fun setLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.name).apply()
    }

    fun applyLanguage() {
        AppCompatDelegate.setApplicationLocales(
            when (getLanguage()) {
                AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
                AppLanguage.Chinese -> LocaleListCompat.create(Locale.SIMPLIFIED_CHINESE)
                AppLanguage.English -> LocaleListCompat.create(Locale.ENGLISH)
            },
        )
    }

    fun isChinese(): Boolean {
        return when (getLanguage()) {
            AppLanguage.System -> isSystemChinese
            AppLanguage.Chinese -> true
            AppLanguage.English -> false
        }
    }

    private companion object {
        const val PREFS_NAME = "app_locale"
        const val KEY_LANGUAGE = "language"
    }
}
