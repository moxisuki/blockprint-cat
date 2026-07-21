package io.github.moxisuki.blockprint.cat.app.core.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.persistence.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppThemeViewModel @Inject constructor(
    private val repository: AppSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppThemeState())
    val state: StateFlow<AppThemeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.themeMode,
                repository.themeColorSource,
                repository.themeSeedColor,
                repository.themeAccentColor,
            ) { mode, colorSource, seedColorArgb, accentColor ->
                AppThemeState(
                    mode = mode,
                    colorSource = colorSource,
                    accentColor = accentColor,
                    seedColor = Color(seedColorArgb),
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun selectMode(mode: AppThemeMode) {
        _state.value = _state.value.copy(mode = mode)
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun selectColorSource(colorSource: AppThemeColorSource) {
        _state.value = _state.value.copy(colorSource = colorSource)
        viewModelScope.launch { repository.setThemeColorSource(colorSource) }
    }

    fun selectAccentColor(accentColor: AppThemeAccentColor) {
        _state.value = _state.value.copy(
            colorSource = AppThemeColorSource.Monet,
            accentColor = accentColor,
        )
        viewModelScope.launch {
            repository.setThemeAccentColor(accentColor)
            repository.setThemeColorSource(AppThemeColorSource.Monet)
        }
    }

    fun selectSeedColor(color: Color) {
        _state.value = _state.value.copy(
            seedColor = color,
            colorSource = AppThemeColorSource.Custom,
        )
        viewModelScope.launch {
            repository.setThemeSeedColor(color.toArgb())
            repository.setThemeColorSource(AppThemeColorSource.Custom)
        }
    }
}
