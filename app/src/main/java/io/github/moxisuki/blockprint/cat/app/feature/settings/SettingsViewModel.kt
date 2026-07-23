package io.github.moxisuki.blockprint.cat.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.data.backup.BlueprintBackupRepository
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguage
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguageManager
import io.github.moxisuki.blockprint.cat.app.core.persistence.AppSettingsRepository
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languageManager: AppLanguageManager,
    private val settingsRepository: AppSettingsRepository,
    private val backupRepository: BlueprintBackupRepository,
    private val resourcePackRepository: ResourcePackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(languageManager.getLanguage())
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            languageManager.language.collect { language ->
                _selectedLanguage.value = language
            }
        }
        viewModelScope.launch {
            combine(
                settingsRepository.localBlueprintTreeUri,
                settingsRepository.localBlueprintTreeDocumentId,
                settingsRepository.communityEnabled,
                settingsRepository.mcsAuthCookies,
                resourcePackRepository.installedPacks.map { it.size },
            ) { treeUri, treeDocumentId, communityEnabled, mcsAuthCookies, packsSize ->
                SettingsState(
                    localBlueprintTreeUri = treeUri,
                    localBlueprintTreeDocumentId = treeDocumentId,
                    communityEnabled = communityEnabled,
                    mcsAuthCookies = mcsAuthCookies,
                    installedResourcePackCount = packsSize,
                )
            }.collect { persistedState ->
                _state.update {
                    it.copy(
                        localBlueprintTreeUri = persistedState.localBlueprintTreeUri,
                        localBlueprintTreeDocumentId = persistedState.localBlueprintTreeDocumentId,
                        communityEnabled = persistedState.communityEnabled,
                        mcsAuthCookies = persistedState.mcsAuthCookies,
                        installedResourcePackCount = persistedState.installedResourcePackCount,
                    )
                }
            }
        }
        onAction(SettingsAction.Opened)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.Opened,
            SettingsAction.ThemeSettingsClicked,
            SettingsAction.AboutClicked,
            -> Unit

            is SettingsAction.CommunityEnabledChanged -> {
                viewModelScope.launch {
                    settingsRepository.setCommunityEnabled(action.enabled)
                }
            }

            is SettingsAction.McsCookiesChanged -> {
                viewModelScope.launch {
                    settingsRepository.setMcsAuthCookies(action.cookies)
                }
            }

            SettingsAction.ClearMcsCookiesClicked -> {
                viewModelScope.launch {
                    settingsRepository.clearMcsAuthCookies()
                }
            }

            is SettingsAction.LanguageSelected -> {
                languageManager.setLanguage(action.language)
                languageManager.applyLanguage()
                _selectedLanguage.value = action.language
            }

            SettingsAction.BlueprintDirectoryClicked -> {
                _state.update { it.copy(isBlueprintDirectoryConfirmVisible = true) }
            }

            SettingsAction.BlueprintDirectoryPickerDismissed -> {
                _state.update { it.copy(isBlueprintDirectoryConfirmVisible = false) }
            }

            is SettingsAction.BlueprintDirectorySelected -> {
                _state.update { it.copy(isBlueprintDirectoryConfirmVisible = false) }
                viewModelScope.launch {
                    settingsRepository.setLocalBlueprintTree(
                        treeUri = action.treeUri,
                        treeDocumentId = action.treeDocumentId,
                    )
                }
            }

            SettingsAction.BackupClicked -> backupBlueprints()

            is SettingsAction.BackupPermissionDenied -> {
                _state.update {
                    it.copy(
                        backupRestoreFeedback = SettingsBackupRestoreFeedback.Error(
                            message = action.message,
                        ),
                    )
                }
            }

            is SettingsAction.RestoreFilePicked -> {
                if (!_state.value.isBackupRunning && !_state.value.isRestoreRunning) {
                    _state.update {
                        it.copy(
                            isRestoreConfirmVisible = true,
                            pendingRestoreFileUri = action.uri,
                            pendingRestoreFileName = action.displayName,
                        )
                    }
                }
            }

            SettingsAction.RestoreConfirmDismissed -> {
                _state.update {
                    it.copy(
                        isRestoreConfirmVisible = false,
                        pendingRestoreFileUri = null,
                        pendingRestoreFileName = null,
                    )
                }
            }

            SettingsAction.RestoreConfirmed -> {
                val restoreUri = _state.value.pendingRestoreFileUri ?: return
                restoreBlueprints(restoreUri)
            }

            SettingsAction.BackupRestoreFeedbackDismissed -> {
                _state.update { it.copy(backupRestoreFeedback = null) }
            }
        }
    }

    private fun backupBlueprints() {
        if (_state.value.isBackupRunning || _state.value.isRestoreRunning) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isBackupRunning = true,
                    backupRestoreFeedback = null,
                )
            }
            runCatching {
                backupRepository.backupToDownloads()
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        isBackupRunning = false,
                        backupRestoreFeedback = SettingsBackupRestoreFeedback.BackupSuccess(
                            fileName = result.fileName,
                            fileCount = result.fileCount,
                            sizeBytes = result.sizeBytes,
                        ),
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isBackupRunning = false,
                        backupRestoreFeedback = SettingsBackupRestoreFeedback.Error(
                            message = throwable.message.orEmpty().ifBlank {
                                throwable::class.java.simpleName
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun restoreBlueprints(uri: String) {
        if (_state.value.isBackupRunning || _state.value.isRestoreRunning) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRestoreRunning = true,
                    isRestoreConfirmVisible = false,
                    pendingRestoreFileUri = null,
                    pendingRestoreFileName = null,
                    backupRestoreFeedback = null,
                )
            }
            runCatching {
                backupRepository.restoreFromZip(uri)
            }.onSuccess { result ->
                _state.update {
                    it.copy(
                        isRestoreRunning = false,
                        backupRestoreFeedback = SettingsBackupRestoreFeedback.RestoreSuccess(
                            fileCount = result.fileCount,
                            hasManifest = result.hasManifest,
                        ),
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isRestoreRunning = false,
                        backupRestoreFeedback = SettingsBackupRestoreFeedback.Error(
                            message = throwable.message.orEmpty().ifBlank {
                                throwable::class.java.simpleName
                            },
                        ),
                    )
                }
            }
        }
    }
}
