package io.github.moxisuki.blockprint.cat.app.feature.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeColorSource
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguage
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguageManager
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languageManager: AppLanguageManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(languageManager.getLanguage())
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    init {
        onAction(SettingsAction.Opened)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.Opened,
            SettingsAction.AboutClicked,
            is SettingsAction.ThemeModeSelected,
            is SettingsAction.ThemeSeedColorSelected,
            -> Unit

            is SettingsAction.ThemeColorSourceSelected -> {
                if (action.colorSource != AppThemeColorSource.Custom) {
                    _state.update { it.copy(isThemePaletteExpanded = false) }
                }
            }

            is SettingsAction.ThemePaletteExpansionChanged -> {
                _state.update { it.copy(isThemePaletteExpanded = action.expanded) }
            }

            is SettingsAction.LanguageSelected -> {
                languageManager.setLanguage(action.language)
                languageManager.applyLanguage()
                _selectedLanguage.value = action.language
            }
        }
    }
}
