package io.github.moxisuki.blockprint.cat.ui.settings

import android.content.ContentUris
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var estimating by remember { mutableStateOf(true) }
    var compressedBytes by remember { mutableStateOf(0L) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var backupUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val files = runCatching { blueprintManager.estimateBackupSize() }.getOrNull()
            if (files != null) {
                totalFiles = files.first
                totalBytes = files.second
            }
        }
        estimating = false
    }

    fun startBackup() {
        backing = true
        resultMessage = null
        backupUri = null
        scope.launch {
            val res = blueprintManager.backupToZip { current, _ -> currentFile = current }
            res.onSuccess {
                compressedBytes = it.totalBytes
                resultMessage = "OK"
                // 查询 MediaStore 获取备份文件的 content URI
                backupUri = findBackupUri(context)
            }.onFailure { e ->
                resultMessage = e.message ?: ""
            }
            backing = false
        }
    }

    val isSuccess = resultMessage == "OK"

    AlertDialog(
        onDismissRequest = { if (!backing) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.backup_title))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (estimating) {
                    Text(
                        stringResource(R.string.cache_calculating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (resultMessage != null) {
                    if (isSuccess) {
                        ResultCard(
                            fileCount = totalFiles,
                            compressedBytes = compressedBytes,
                        )
                    } else {
                        Text(
                            stringResource(R.string.backup_failed, resultMessage!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else if (backing) {
                    ProgressCard(current = currentFile, total = totalFiles)
                } else if (totalFiles > 0) {
                    SummaryCard(fileCount = totalFiles, totalBytes = totalBytes)
                } else {
                    Text(
                        stringResource(R.string.backup_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            if (resultMessage != null && isSuccess) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
                    TextButton(onClick = {
                        backupUri?.let { uri ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        }
                    }) {
                        Icon(Icons.Outlined.IosShare, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_share))
                    }
                }
            } else if (resultMessage != null && !isSuccess) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
                    TextButton(onClick = { startBackup() }) { Text(stringResource(R.string.bp_action_regenerate)) }
                }
            } else {
                TextButton(
                    onClick = { startBackup() },
                    enabled = !backing && totalFiles > 0,
                ) {
                    Text(if (backing) stringResource(R.string.backup_in_progress) else stringResource(R.string.settings_backup_title))
                }
            }
        },
        dismissButton = {
            if (!backing && resultMessage == null) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

@Composable
private fun SummaryCard(fileCount: Int, totalBytes: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(R.string.backup_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.backup_count_label), style = MaterialTheme.typography.bodySmall)
                Text("$fileCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.backup_size_label), style = MaterialTheme.typography.bodySmall)
                Text(formatBytes(totalBytes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun ProgressCard(current: Int, total: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(R.string.backup_in_progress), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            val progress = if (total > 0) current.toFloat() / total else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "$current / $total  (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultCard(fileCount: Int, compressedBytes: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(stringResource(R.string.backup_done), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.backup_file_count, fileCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.backup_size, formatBytes(compressedBytes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.backup_location),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

private fun findBackupUri(context: android.content.Context): android.net.Uri? {
    val projection = arrayOf(MediaStore.Downloads._ID)
    val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
    val selectionArgs = arrayOf("BlockPrintCat-Backup-%")
    val sortOrder = "${MediaStore.Downloads.DATE_ADDED} DESC"
    return runCatching {
        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection, selection, selectionArgs, sortOrder,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
            } else null
        }
    }.getOrNull()
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
