package io.github.moxisuki.blockprint.cat.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.LocalAppWindowWidthSize
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguage
import io.github.moxisuki.blockprint.cat.app.feature.settings.components.SettingsCommunitySection
import io.github.moxisuki.blockprint.cat.app.feature.settings.components.SettingsStorageSection
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val FloatingNavigationSettingsBottomPadding = 104.dp

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    selectedLanguage: AppLanguage,
    onAction: (SettingsAction) -> Unit,
    onPickBlueprintDirectory: () -> Unit,
    onRestoreBackup: () -> Unit,
    onResourcePacksClick: () -> Unit,
    onDebugClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val languages = listOf(
        AppLanguage.System,
        AppLanguage.Chinese,
        AppLanguage.English,
    )
    val languageLabels = listOf(
        stringResource(R.string.settings_language_subtitle_system),
        stringResource(R.string.settings_language_subtitle_zh),
        stringResource(R.string.settings_language_subtitle_en),
    )

    val isWide = LocalAppWindowWidthSize.current.isWide
    if (isWide) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .appScrollEndHaptic(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = FloatingNavigationSettingsBottomPadding,
                ),
            ) {
                appearanceSectionItems(state, onAction, onResourcePacksClick)
                languageSectionItems(languages, languageLabels, selectedLanguage, onAction)
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .appScrollEndHaptic(),
                contentPadding = PaddingValues(
                    top = 12.dp,
                    bottom = FloatingNavigationSettingsBottomPadding,
                ),
            ) {
                storageSectionItems(state, onAction, onRestoreBackup)
                communitySectionItems(state, onAction)
                aboutSectionItems(onAction, onDebugClick)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
                .appScrollEndHaptic()
                .appMaxContentWidth(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = FloatingNavigationSettingsBottomPadding,
            ),
        ) {
            appearanceSectionItems(state, onAction, onResourcePacksClick)
            languageSectionItems(languages, languageLabels, selectedLanguage, onAction)
            storageSectionItems(state, onAction, onRestoreBackup)
            communitySectionItems(state, onAction)
            aboutSectionItems(onAction, onDebugClick)
        }
    }

    SettingsConfirmDialog(
        show = state.isBlueprintDirectoryConfirmVisible,
        title = stringResource(R.string.settings_storage_pick_dialog_title),
        summary = stringResource(R.string.settings_storage_pick_dialog_message),
        confirmText = stringResource(R.string.settings_storage_pick_confirm),
        onDismissRequest = {
            onAction(SettingsAction.BlueprintDirectoryPickerDismissed)
        },
        onConfirm = {
            onAction(SettingsAction.BlueprintDirectoryPickerDismissed)
            onPickBlueprintDirectory()
        },
    )
    SettingsConfirmDialog(
        show = state.isRestoreConfirmVisible,
        title = stringResource(R.string.settings_restore_dialog_title),
        summary = state.pendingRestoreFileName?.let { fileName ->
            stringResource(R.string.settings_restore_dialog_message_with_file, fileName)
        } ?: stringResource(R.string.settings_restore_dialog_message),
        confirmText = stringResource(R.string.settings_restore_dialog_confirm),
        onDismissRequest = {
            onAction(SettingsAction.RestoreConfirmDismissed)
        },
        onConfirm = {
            onAction(SettingsAction.RestoreConfirmed)
        },
    )
}

private fun LazyListScope.appearanceSectionItems(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onResourcePacksClick: () -> Unit,
) {
    item(key = "appearance-title") {
        SectionTitle(
            text = stringResource(R.string.settings_section_appearance),
        )
    }
    item(key = "theme-entry") {
        ArrowPreference(
            title = stringResource(R.string.settings_theme_title),
            summary = stringResource(R.string.theme_dialog_title),
            onClick = { onAction(SettingsAction.ThemeSettingsClicked) },
        )
    }
    item(key = "resourcepacks-entry") {
        ArrowPreference(
            title = stringResource(R.string.settings_resource_packs_title),
            summary = if (state.installedResourcePackCount == 0) {
                stringResource(R.string.settings_resource_packs_subtitle_empty)
            } else {
                stringResource(
                    R.string.settings_resource_packs_subtitle_count,
                    state.installedResourcePackCount,
                )
            },
            onClick = onResourcePacksClick,
        )
    }
}

private fun LazyListScope.languageSectionItems(
    languages: List<AppLanguage>,
    languageLabels: List<String>,
    selectedLanguage: AppLanguage,
    onAction: (SettingsAction) -> Unit,
) {
    item(key = "language-title") {
        SectionTitle(
            text = stringResource(R.string.settings_language_title),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    item(key = "language") {
        OverlaySpinnerPreference(
            items = languageLabels.map { label -> DropdownItem(text = label) },
            selectedIndex = languages.indexOf(selectedLanguage).coerceAtLeast(0),
            title = stringResource(R.string.settings_language_title),
            onSelectedIndexChange = { index ->
                onAction(SettingsAction.LanguageSelected(languages[index]))
            },
        )
    }
}

private fun LazyListScope.storageSectionItems(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onRestoreBackup: () -> Unit,
) {
    item(key = "storage-title") {
        SectionTitle(
            text = stringResource(R.string.settings_section_storage),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    item(key = "storage") {
        SettingsStorageSection(
            localBlueprintTreeUri = state.localBlueprintTreeUri,
            localBlueprintTreeDocumentId = state.localBlueprintTreeDocumentId,
            isBackupRunning = state.isBackupRunning,
            isRestoreRunning = state.isRestoreRunning,
            feedback = state.backupRestoreFeedback,
            isCacheManagerVisible = state.isCacheManagerVisible,
            cacheStats = state.cacheStats,
            isCacheStatsLoading = state.isCacheStatsLoading,
            isCacheClearing = state.isCacheClearing,
            pendingCacheClearCategory = state.pendingCacheClearCategory,
            cacheErrorMessage = state.cacheErrorMessage,
            onBlueprintDirectoryClick = { onAction(SettingsAction.BlueprintDirectoryClicked) },
            onBackupClick = { onAction(SettingsAction.BackupClicked) },
            onRestoreClick = onRestoreBackup,
            onDismissFeedback = { onAction(SettingsAction.BackupRestoreFeedbackDismissed) },
            onCacheClick = { onAction(SettingsAction.CacheClicked) },
            onCacheDismiss = { onAction(SettingsAction.CacheDismissed) },
            onCacheRefresh = { onAction(SettingsAction.CacheRefreshClicked) },
            onCacheClearRequested = { onAction(SettingsAction.CacheClearRequested(it)) },
            onCacheClearConfirmed = { onAction(SettingsAction.CacheClearConfirmed) },
            onCacheClearDismissed = { onAction(SettingsAction.CacheClearDismissed) },
        )
    }
}

private fun LazyListScope.communitySectionItems(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
) {
    item(key = "community-title") {
        SectionTitle(
            text = stringResource(R.string.settings_community_card_title),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    item(key = "community") {
        SettingsCommunitySection(
            communityEnabled = state.communityEnabled,
            onCommunityEnabledChange = {
                onAction(SettingsAction.CommunityEnabledChanged(it))
            },
        )
    }
}

private fun LazyListScope.aboutSectionItems(
    onAction: (SettingsAction) -> Unit,
    onDebugClick: () -> Unit,
) {
    item(key = "about-title") {
        SectionTitle(
            text = stringResource(R.string.settings_section_about),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    item(key = "about-entry") {
        ArrowPreference(
            title = stringResource(R.string.settings_about_title),
            summary = stringResource(R.string.settings_about_subtitle_info),
            onClick = { onAction(SettingsAction.AboutClicked) },
        )
    }
    item(key = "debug-entry") {
        ArrowPreference(
            title = stringResource(R.string.nav_title_debug),
            onClick = onDebugClick,
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
    )
}

@Composable
private fun SettingsConfirmDialog(
    show: Boolean,
    title: String,
    summary: String,
    confirmText: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
        summary = summary,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                text = stringResource(android.R.string.cancel),
                onClick = onDismissRequest,
            )
            TextButton(
                modifier = Modifier.weight(1f),
                text = confirmText,
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PreviewAppTheme {
        SettingsScreen(
            state = SettingsState(),
            selectedLanguage = AppLanguage.System,
            onAction = {},
            onPickBlueprintDirectory = {},
            onRestoreBackup = {},
            onResourcePacksClick = {},
            onDebugClick = {},
        )
    }
}
