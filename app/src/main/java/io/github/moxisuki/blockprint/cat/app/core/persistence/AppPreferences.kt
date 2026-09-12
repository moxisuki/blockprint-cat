package io.github.moxisuki.blockprint.cat.app.core.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeAccentColor
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeColorSource
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeMode
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPreferences(private val dataStore: DataStore<Preferences>) {

    val themeMode: Flow<AppThemeMode> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE]
        raw?.let { fromString(it) } ?: AppThemeMode.System
    }

    val themeColorSource: Flow<AppThemeColorSource> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_COLOR_SOURCE]
        raw?.let { fromString(it) } ?: AppThemeColorSource.Default
    }

    val themeSeedColor: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_SEED_COLOR] ?: DEFAULT_SEED_COLOR
    }

    val themeAccentColor: Flow<AppThemeAccentColor> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_ACCENT_COLOR]
        raw?.let { fromString(it) } ?: AppThemeAccentColor.Default
    }

    val language: Flow<AppLanguage> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_LANGUAGE]
        raw?.let { fromString(it) } ?: AppLanguage.System
    }

    val localBlueprintTreeUri: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LOCAL_BLUEPRINT_TREE_URI]
    }

    val localBlueprintTreeDocumentId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_LOCAL_BLUEPRINT_TREE_DOCUMENT_ID]
    }

    val communityEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_COMMUNITY_ENABLED] ?: true
    }

    val mcsAuthCookies: Flow<McsAuthCookies> = dataStore.data.map { prefs ->
        McsAuthCookies(
            uuid = prefs[KEY_MCS_UUID].orEmpty(),
            userAuth = prefs[KEY_MCS_USER_AUTH].orEmpty(),
            cfClearance = prefs[KEY_MCS_CF_CLEARANCE].orEmpty(),
            nickname = prefs[KEY_MCS_NICKNAME].orEmpty(),
            savedAt = prefs[KEY_MCS_SAVED_AT] ?: 0L,
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setThemeColorSource(colorSource: AppThemeColorSource) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_COLOR_SOURCE] = colorSource.name
        }
    }

    suspend fun setThemeSeedColor(argb: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_SEED_COLOR] = argb
        }
    }

    suspend fun setThemeAccentColor(accentColor: AppThemeAccentColor) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_ACCENT_COLOR] = accentColor.name
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.name
        }
    }

    suspend fun setLocalBlueprintTree(
        treeUri: String,
        treeDocumentId: String?,
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_LOCAL_BLUEPRINT_TREE_URI] = treeUri
            if (treeDocumentId.isNullOrBlank()) {
                prefs.remove(KEY_LOCAL_BLUEPRINT_TREE_DOCUMENT_ID)
            } else {
                prefs[KEY_LOCAL_BLUEPRINT_TREE_DOCUMENT_ID] = treeDocumentId
            }
        }
    }

    suspend fun setCommunityEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_COMMUNITY_ENABLED] = enabled
        }
    }

    suspend fun setMcsAuthCookies(cookies: McsAuthCookies) {
        dataStore.edit { prefs ->
            if (cookies.uuid.isBlank()) {
                prefs.remove(KEY_MCS_UUID)
            } else {
                prefs[KEY_MCS_UUID] = cookies.uuid
            }
            prefs[KEY_MCS_USER_AUTH] = cookies.userAuth
            if (cookies.cfClearance.isBlank()) {
                prefs.remove(KEY_MCS_CF_CLEARANCE)
            } else {
                prefs[KEY_MCS_CF_CLEARANCE] = cookies.cfClearance
            }
            if (cookies.nickname.isBlank()) {
                prefs.remove(KEY_MCS_NICKNAME)
            } else {
                prefs[KEY_MCS_NICKNAME] = cookies.nickname
            }
            prefs[KEY_MCS_SAVED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun clearMcsAuthCookies() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_MCS_UUID)
            prefs.remove(KEY_MCS_USER_AUTH)
            prefs.remove(KEY_MCS_CF_CLEARANCE)
            prefs.remove(KEY_MCS_NICKNAME)
            prefs.remove(KEY_MCS_SAVED_AT)
        }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_THEME_COLOR_SOURCE = stringPreferencesKey("theme_color_source")
        private val KEY_THEME_SEED_COLOR = intPreferencesKey("theme_seed_color")
        private val KEY_THEME_ACCENT_COLOR = stringPreferencesKey("theme_accent_color")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_LOCAL_BLUEPRINT_TREE_URI = stringPreferencesKey("local_blueprint_tree_uri")
        private val KEY_LOCAL_BLUEPRINT_TREE_DOCUMENT_ID =
            stringPreferencesKey("local_blueprint_tree_document_id")
        private val KEY_COMMUNITY_ENABLED = booleanPreferencesKey("community_enabled")
        private val KEY_MCS_UUID = stringPreferencesKey("mcs_uuid")
        private val KEY_MCS_USER_AUTH = stringPreferencesKey("mcs_user_auth")
        private val KEY_MCS_CF_CLEARANCE = stringPreferencesKey("mcs_cf_clearance")
        private val KEY_MCS_NICKNAME = stringPreferencesKey("mcs_nickname")
        private val KEY_MCS_SAVED_AT =
            androidx.datastore.preferences.core.longPreferencesKey("mcs_saved_at")

        private const val DEFAULT_SEED_COLOR = 0xFF3482FF.toInt()

        private inline fun <reified T : Enum<T>> fromString(name: String): T? =
            runCatching { enumValueOf<T>(name) }.getOrNull()
    }
}
