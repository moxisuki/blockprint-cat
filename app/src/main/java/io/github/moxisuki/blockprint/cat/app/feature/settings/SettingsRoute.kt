package io.github.moxisuki.blockprint.cat.app.feature.settings

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.R

@Composable
fun SettingsRoute(
    onThemeClick: () -> Unit,
    onAboutClick: () -> Unit,
    onDebugClick: () -> Unit,
    onResourcePacksClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val backupPermissionDeniedMessage = stringResource(R.string.backup_permission_denied)
    val backupPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onAction(SettingsAction.BackupClicked)
        } else {
            viewModel.onAction(SettingsAction.BackupPermissionDenied(backupPermissionDeniedMessage))
        }
    }
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.onAction(
                SettingsAction.BlueprintDirectorySelected(
                    treeUri = uri.toString(),
                    treeDocumentId = runCatching {
                        DocumentsContract.getTreeDocumentId(uri)
                    }.getOrNull(),
                ),
            )
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onAction(
                SettingsAction.RestoreFilePicked(
                    uri = uri.toString(),
                    displayName = context.contentResolver.queryDisplayName(uri),
                ),
            )
        }
    }

    SettingsScreen(
        state = state,
        selectedLanguage = selectedLanguage,
        onAction = handleAction@{ action ->
            if (action == SettingsAction.BackupClicked) {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    backupPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    viewModel.onAction(action)
                }
                return@handleAction
            }
            when (action) {
                SettingsAction.ThemeSettingsClicked -> onThemeClick()
                SettingsAction.AboutClicked -> onAboutClick()
                is SettingsAction.LanguageSelected,
                is SettingsAction.CommunityEnabledChanged,
                is SettingsAction.McsCookiesChanged,
                SettingsAction.ClearMcsCookiesClicked,
                SettingsAction.Opened,
                SettingsAction.BlueprintDirectoryClicked,
                SettingsAction.BlueprintDirectoryPickerDismissed,
                is SettingsAction.BlueprintDirectorySelected,
                SettingsAction.BackupClicked,
                is SettingsAction.BackupPermissionDenied,
                is SettingsAction.RestoreFilePicked,
                SettingsAction.RestoreConfirmed,
                SettingsAction.RestoreConfirmDismissed,
                SettingsAction.BackupRestoreFeedbackDismissed,
                -> Unit
            }
            viewModel.onAction(action)
        },
        onPickBlueprintDirectory = {
            safLauncher.launch(null)
        },
        onRestoreBackup = {
            restoreLauncher.launch(
                arrayOf(
                    "application/zip",
                    "application/x-zip-compressed",
                    "*/*",
                ),
            )
        },
        onDebugClick = onDebugClick,
        onResourcePacksClick = onResourcePacksClick,
        modifier = modifier,
    )
}

private fun ContentResolver.queryDisplayName(uri: Uri): String? =
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (displayNameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(displayNameIndex)
        } else {
            null
        }
    }
