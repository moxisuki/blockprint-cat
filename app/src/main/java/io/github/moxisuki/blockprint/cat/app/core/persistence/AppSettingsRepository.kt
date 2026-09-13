package io.github.moxisuki.blockprint.cat.app.core.persistence

import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeColorSource
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeAccentColor
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeMode
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AppSettingsRepository @Inject constructor(
    private val prefs: AppPreferences,
) {
    val themeMode: Flow<AppThemeMode> = prefs.themeMode

    val themeColorSource: Flow<AppThemeColorSource> = prefs.themeColorSource

    val themeSeedColor: Flow<Int> = prefs.themeSeedColor

    val themeAccentColor: Flow<AppThemeAccentColor> = prefs.themeAccentColor

    val language: Flow<AppLanguage> = prefs.language

    val localBlueprintTreeUri: Flow<String?> = prefs.localBlueprintTreeUri

    val localBlueprintTreeDocumentId: Flow<String?> = prefs.localBlueprintTreeDocumentId

    val communityEnabled: Flow<Boolean> = prefs.communityEnabled

    suspend fun setThemeMode(mode: AppThemeMode) {
        prefs.setThemeMode(mode)
    }

    suspend fun setThemeColorSource(colorSource: AppThemeColorSource) {
        prefs.setThemeColorSource(colorSource)
    }

    suspend fun setThemeSeedColor(argb: Int) {
        prefs.setThemeSeedColor(argb)
    }

    suspend fun setThemeAccentColor(accentColor: AppThemeAccentColor) {
        prefs.setThemeAccentColor(accentColor)
    }

    suspend fun setLanguage(language: AppLanguage) {
        prefs.setLanguage(language)
    }

    suspend fun setLocalBlueprintTree(
        treeUri: String,
        treeDocumentId: String?,
    ) {
        prefs.setLocalBlueprintTree(treeUri, treeDocumentId)
    }

    suspend fun setCommunityEnabled(enabled: Boolean) {
        prefs.setCommunityEnabled(enabled)
    }

}
