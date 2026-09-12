package io.github.moxisuki.blockprint.cat.app.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.persistence.AppSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class AppShellViewModel @Inject constructor(
    settingsRepository: AppSettingsRepository,
) : ViewModel() {
    val communityEnabled: StateFlow<Boolean> = settingsRepository.communityEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )
}
