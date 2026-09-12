package io.github.moxisuki.blockprint.cat.app.feature.settings.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import io.github.moxisuki.blockprint.cat.app.core.cache.AppCacheCategory
import io.github.moxisuki.blockprint.cat.app.core.cache.AppCacheStats
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.feature.settings.SettingsBackupRestoreFeedback
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
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
    isCacheManagerVisible: Boolean,
    cacheStats: AppCacheStats?,
    isCacheStatsLoading: Boolean,
    isCacheClearing: Boolean,
    pendingCacheClearCategory: AppCacheCategory?,
    cacheErrorMessage: String?,
    onCacheClick: () -> Unit,
    onCacheDismiss: () -> Unit,
    onCacheRefresh: () -> Unit,
    onCacheClearRequested: (AppCacheCategory) -> Unit,
    onCacheClearConfirmed: () -> Unit,
    onCacheClearDismissed: () -> Unit,
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
            summary = cacheStats?.let {
                stringResource(R.string.cache_total_summary, it.totalStorageBytes.formatBytes())
            } ?: stringResource(R.string.settings_cache_subtitle),
            onClick = onCacheClick,
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

    CacheManagementSheet(
        show = isCacheManagerVisible,
        stats = cacheStats,
        isStatsLoading = isCacheStatsLoading,
        isClearing = isCacheClearing,
        errorMessage = cacheErrorMessage,
        onDismissRequest = onCacheDismiss,
        onRefresh = onCacheRefresh,
        onClearRequested = onCacheClearRequested,
    )
    CacheClearDialog(
        category = pendingCacheClearCategory,
        onDismissRequest = onCacheClearDismissed,
        onConfirm = onCacheClearConfirmed,
    )
}

@Composable
private fun CacheManagementSheet(
    show: Boolean,
    stats: AppCacheStats?,
    isStatsLoading: Boolean,
    isClearing: Boolean,
    errorMessage: String?,
    onDismissRequest: () -> Unit,
    onRefresh: () -> Unit,
    onClearRequested: (AppCacheCategory) -> Unit,
) {
    OverlayBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.settings_cache_title),
        startAction = {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismissRequest,
            )
        },
        endAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onRefresh,
                ) {
                    Icon(
                        imageVector = MiuixIcons.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = { onClearRequested(AppCacheCategory.All) },
                ) {
                    Icon(
                        imageVector = MiuixIcons.Delete,
                        contentDescription = stringResource(R.string.action_clear),
                        tint = MiuixTheme.colorScheme.error,
                    )
                }
            }
        },
        insideMargin = androidx.compose.ui.unit.DpSize(16.dp, 18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                isStatsLoading -> Text(
                    text = stringResource(R.string.cache_calculating),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )

                stats != null -> {
                    CacheCategoryCard(
                        title = stringResource(R.string.cache_room_title),
                        summary = stringResource(
                            R.string.cache_room_stats,
                            stats.blueprintRecordCount,
                            stats.blueprintStorageBytes.formatBytes(),
                        ),
                        isClearing = isClearing,
                        onClear = { onClearRequested(AppCacheCategory.BlueprintMetadata) },
                    )
                    CacheCategoryCard(
                        title = stringResource(R.string.cache_render_title),
                        summary = stringResource(
                            R.string.cache_render_stats,
                            "${stats.renderFileCount} · ${stats.renderStorageBytes.formatBytes()}",
                        ),
                        isClearing = isClearing,
                        onClear = { onClearRequested(AppCacheCategory.RenderResources) },
                    )
                    CacheCategoryCard(
                        title = stringResource(R.string.cache_glb_title),
                        summary = stringResource(
                            R.string.cache_glb_stats,
                            stats.previewModelFileCount,
                            stats.previewModelStorageBytes.formatBytes(),
                        ),
                        isClearing = isClearing,
                        onClear = { onClearRequested(AppCacheCategory.PreviewModels) },
                    )
                    CacheCategoryCard(
                        title = stringResource(R.string.cache_temp_title),
                        summary = stringResource(
                            R.string.cache_temp_stats,
                            stats.temporaryFileCount,
                            stats.temporaryStorageBytes.formatBytes(),
                        ),
                        isClearing = isClearing,
                        onClear = { onClearRequested(AppCacheCategory.Temporary) },
                    )
                }
            }
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MiuixTheme.colorScheme.error,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            Text(
                text = stringResource(R.string.cache_note),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

@Composable
private fun CacheCategoryCard(
    title: String,
    summary: String,
    isClearing: Boolean,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                )
                Text(
                    text = if (isClearing) stringResource(R.string.cache_clearing) else summary,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = stringResource(R.string.action_clear),
                    tint = MiuixTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CacheClearDialog(
    category: AppCacheCategory?,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val message = when (category) {
        AppCacheCategory.BlueprintMetadata -> stringResource(R.string.cache_room_confirm_msg)
        AppCacheCategory.RenderResources -> stringResource(R.string.cache_render_confirm_msg)
        AppCacheCategory.PreviewModels -> stringResource(R.string.cache_glb_confirm_msg)
        AppCacheCategory.Temporary -> stringResource(R.string.cache_temp_confirm_msg)
        AppCacheCategory.All -> stringResource(R.string.cache_all_confirm_msg)
        null -> ""
    }
    OverlayDialog(
        show = category != null,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.settings_cache_title),
        summary = message,
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
                text = stringResource(R.string.action_confirm_clear),
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColorsPrimary(),
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
            isCacheManagerVisible = false,
            cacheStats = null,
            isCacheStatsLoading = false,
            isCacheClearing = false,
            pendingCacheClearCategory = null,
            cacheErrorMessage = null,
            onCacheClick = {},
            onCacheDismiss = {},
            onCacheRefresh = {},
            onCacheClearRequested = {},
            onCacheClearConfirmed = {},
            onCacheClearDismissed = {},
        )
    }
}
