package io.github.moxisuki.blockprint.cat.app.feature.settings.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.feature.settings.SettingsBackupRestoreFeedback
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsStorageSection(
    localBlueprintTreeUri: String?,
    localBlueprintTreeDocumentId: String?,
    isBackupRunning: Boolean,
    isRestoreRunning: Boolean,
    feedback: SettingsBackupRestoreFeedback?,
    onBlueprintDirectoryClick: () -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ArrowPreference(
            title = stringResource(R.string.settings_storage_saf_title),
            summary = remember(localBlueprintTreeUri, localBlueprintTreeDocumentId) {
                localBlueprintTreeDocumentId.toSafDisplayPath()
                    ?: localBlueprintTreeUri.toSafDisplayPath()
            } ?: stringResource(R.string.settings_storage_saf_not_selected),
            onClick = onBlueprintDirectoryClick,
        )
        ArrowPreference(
            modifier = Modifier.padding(top = 8.dp),
            title = stringResource(R.string.settings_cache_title),
            summary = stringResource(R.string.settings_cache_subtitle),
            onClick = {},
        )
        ArrowPreference(
            modifier = Modifier.padding(top = 8.dp),
            title = stringResource(R.string.settings_backup_title),
            summary = if (isBackupRunning) {
                stringResource(R.string.backup_in_progress)
            } else {
                stringResource(R.string.settings_backup_subtitle)
            },
            onClick = {
                if (!isBackupRunning && !isRestoreRunning) {
                    onBackupClick()
                }
            },
        )
        ArrowPreference(
            modifier = Modifier.padding(top = 8.dp),
            title = stringResource(R.string.settings_restore_title),
            summary = if (isRestoreRunning) {
                stringResource(R.string.restore_in_progress)
            } else {
                stringResource(R.string.settings_restore_subtitle)
            },
            onClick = {
                if (!isBackupRunning && !isRestoreRunning) {
                    onRestoreClick()
                }
            },
        )
        if (feedback != null) {
            BackupRestoreFeedbackCard(
                feedback = feedback,
                onDismiss = onDismissFeedback,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun BackupRestoreFeedbackCard(
    feedback: SettingsBackupRestoreFeedback,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeedbackTitle(feedback = feedback, modifier = Modifier.weight(1f))
                TextButton(
                    text = stringResource(R.string.bridge_error_dismiss),
                    onClick = onDismiss,
                )
            }
            FeedbackSummary(feedback = feedback)
        }
    }
}

@Composable
private fun FeedbackTitle(
    feedback: SettingsBackupRestoreFeedback,
    modifier: Modifier = Modifier,
) {
    val title = when (feedback) {
        is SettingsBackupRestoreFeedback.BackupSuccess -> stringResource(R.string.backup_done)
        is SettingsBackupRestoreFeedback.RestoreSuccess -> stringResource(R.string.restore_done)
        is SettingsBackupRestoreFeedback.Error -> stringResource(R.string.backup_restore_failed)
    }
    Text(
        modifier = modifier,
        text = title,
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        style = MiuixTheme.textStyles.body1,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FeedbackSummary(feedback: SettingsBackupRestoreFeedback) {
    when (feedback) {
        is SettingsBackupRestoreFeedback.BackupSuccess -> {
            Text(
                text = stringResource(R.string.backup_file_count, feedback.fileCount),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.backup_size, feedback.sizeBytes.formatBytes()),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.backup_saved_file, feedback.fileName),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        is SettingsBackupRestoreFeedback.RestoreSuccess -> {
            Text(
                text = stringResource(R.string.restore_file_count, feedback.fileCount),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
            Text(
                text = if (feedback.hasManifest) {
                    stringResource(R.string.restore_manifest_applied)
                } else {
                    stringResource(R.string.restore_manifest_missing)
                },
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }

        is SettingsBackupRestoreFeedback.Error -> {
            Text(
                text = feedback.message,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

private fun String?.toSafDisplayPath(): String? {
    val rawValue = this?.takeIf { it.isNotBlank() } ?: return null
    val basePath = Uri.decode(rawValue)
        .replace("primary:", "primary/")
        .replace(':', '/')
        .trimEnd('/')
        .ifBlank { null }
        ?: return null
    return if (basePath.endsWith("/blockprintCat")) {
        basePath
    } else {
        "$basePath/blockprintCat"
    }
}

private fun Long.formatBytes(): String {
    if (this < 1024L) return "$this B"
    val units = listOf("KB", "MB", "GB")
    var value = this.toDouble() / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return "%.1f %s".format(value, units[unitIndex])
}

@Preview(showBackground = true)
@Composable
private fun SettingsStorageSectionPreview() {
    PreviewAppTheme {
        SettingsStorageSection(
            localBlueprintTreeUri = "content://com.android.externalstorage.documents/tree/primary%3ABlueprints",
            localBlueprintTreeDocumentId = "primary:Blueprints",
            isBackupRunning = false,
            isRestoreRunning = false,
            feedback = SettingsBackupRestoreFeedback.BackupSuccess(
                fileName = "blockprint-cat-backup-20260720-120000.zip",
                fileCount = 12,
                sizeBytes = 1_742_000L,
            ),
            onBlueprintDirectoryClick = {},
            onBackupClick = {},
            onRestoreClick = {},
            onDismissFeedback = {},
        )
    }
}
