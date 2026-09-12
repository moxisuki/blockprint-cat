package io.github.moxisuki.blockprint.cat.app.core.locale

import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.moxisuki.blockprint.cat.app.core.persistence.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLanguageManager @Inject constructor(
    private val repository: AppSettingsRepository,
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var cachedLanguage: AppLanguage = AppLanguage.System
    private val _language = MutableStateFlow(cachedLanguage)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    init {
        appScope.launch {
            repository.language.collect { language ->
                _language.value = language
                if (cachedLanguage != language) {
                    cachedLanguage = language
                    applyLanguage(language)
                }
            }
        }
    }

    val isSystemChinese: Boolean
        get() = currentSystemLocale().language == Locale.CHINESE.language

    fun getLanguage(): AppLanguage = cachedLanguage

    fun toBcp47Tag(): String = when (getLanguage()) {
        AppLanguage.System -> {
            val raw = currentSystemLocale().toLanguageTag()
            if (raw.lowercase().startsWith("zh")) "zh_cn" else raw.lowercase().replace('-', '_')
        }
        AppLanguage.Chinese -> "zh_cn"
        AppLanguage.English -> "en_us"
    }

    fun resourcePackLocaleCandidates(): List<String> {
        val current = toBcp47Tag()
        val normalized = current.lowercase().replace('-', '_')
        val languageFallback = normalized.substringBefore('_')
        val preferred = when (languageFallback) {
            "zh" -> listOf("zh_cn")
            "en" -> listOf("en_us")
            else -> listOf(normalized)
        }
        return (preferred + "en_us").distinct()
    }

    fun setLanguage(language: AppLanguage) {
        cachedLanguage = language
        _language.value = language
        applyLanguage(language)
        appScope.launch(Dispatchers.IO) {
            repository.setLanguage(language)
        }
    }

    fun applyLanguage() {
        applyLanguage(getLanguage())
    }

    private fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            when (language) {
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

    private fun currentSystemLocale(): Locale {
        val config = Resources.getSystem().configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales[0]
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }
    }
}
