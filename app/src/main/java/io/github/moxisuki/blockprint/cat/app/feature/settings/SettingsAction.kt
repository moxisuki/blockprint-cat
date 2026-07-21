package io.github.moxisuki.blockprint.cat.app.feature.settings

import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguage

sealed interface SettingsAction {
    data object Opened : SettingsAction
    data object ThemeSettingsClicked : SettingsAction
    data object AboutClicked : SettingsAction
    data class LanguageSelected(val language: AppLanguage) : SettingsAction
    data object BlueprintDirectoryClicked : SettingsAction
    data object BlueprintDirectoryPickerDismissed : SettingsAction
    data class BlueprintDirectorySelected(
        val treeUri: String,
        val treeDocumentId: String?,
    ) : SettingsAction
    data object BackupClicked : SettingsAction
    data class BackupPermissionDenied(val message: String) : SettingsAction
    data class RestoreFilePicked(
        val uri: String,
        val displayName: String?,
    ) : SettingsAction
    data object RestoreConfirmed : SettingsAction
    data object RestoreConfirmDismissed : SettingsAction
    data object BackupRestoreFeedbackDismissed : SettingsAction
}
