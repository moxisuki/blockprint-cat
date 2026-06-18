package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import kotlinx.coroutines.launch

@Composable
fun BackupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, BackupEntryPoint::class.java)
    }
    val blueprintManager = entryPoint.blueprintManager()
    val scope = rememberCoroutineScope()
    var backing by remember { mutableStateOf(false) }
    var currentFile by remember { mutableIntStateOf(0) }
    var totalFiles by remember { mutableIntStateOf(0) }
    var totalBytes by remember { mutableStateOf(0L) }
    var compressedBytes by remember { mutableStateOf(0L) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val files = runCatching { blueprintManager.estimateBackupSize() }.getOrNull()
        if (files != null) {
            totalFiles = files.first
            totalBytes = files.second
        }
    }

    fun startBackup() {
        backing = true
        resultMessage = null
        scope.launch {
            val res = blueprintManager.backupToZip { current, _ -> currentFile = current }
            res.onSuccess {
                compressedBytes = it.totalBytes
                resultMessage = "OK"
            }.onFailure { e ->
                resultMessage = e.message ?: ""
            }
            backing = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!backing) onDismiss() },
        title = { Text(stringResource(R.string.backup_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (resultMessage != null) {
                    val isSuccess = resultMessage == "OK"
                    Column {
                        Text(
                            if (isSuccess) stringResource(R.string.backup_done) else stringResource(R.string.backup_failed, resultMessage!!),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (compressedBytes > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.backup_file_count_x, totalFiles), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.backup_size_x, formatBytes(compressedBytes)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.backup_location_x), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                } else if (backing) {
                    Column {
                        Text(stringResource(R.string.backup_in_progress), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { if (totalFiles > 0) currentFile.toFloat() / totalFiles else 0f }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$currentFile / $totalFiles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column {
                        Text(stringResource(R.string.backup_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (totalFiles > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.backup_count_label), style = MaterialTheme.typography.bodySmall)
                                Text("$totalFiles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.backup_size_label), style = MaterialTheme.typography.bodySmall)
                                Text(formatBytes(totalBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.backup_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (resultMessage != null) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            } else {
                TextButton(onClick = { startBackup() }, enabled = !backing && totalFiles > 0) {
                    Text(if (backing) stringResource(R.string.backup_in_progress) else stringResource(R.string.settings_backup_title))
                }
            }
        },
        dismissButton = {
            if (!backing) TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupEntryPoint {
    fun blueprintManager(): BlueprintManager
}
