package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.PackProgress
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton

@Composable
internal fun VanillaPackCard(
    entry: ResourcePackEntry?,
    onDownload: () -> Unit,
    onRedownload: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.resourcepacks_section_vanilla))
            val status = when {
                entry != null -> stringResource(
                    R.string.resourcepacks_vanilla_installed,
                    entry.version,
                    entry.fileCount,
                    formatBytes(entry.totalSize),
                )
                else -> stringResource(R.string.resourcepacks_vanilla_not_installed)
            }
            Text(status)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    entry != null -> {
                        TextButton(onClick = onRedownload, text = stringResource(R.string.render_redownload))
                        TextButton(onClick = onDelete, text = stringResource(R.string.action_delete))
                    }
                    else -> TextButton(onClick = onDownload, text = stringResource(R.string.render_check_update))
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes.toDouble(); var i = 0
    while (value >= 1024.0 && i < units.lastIndex) { value /= 1024.0; i++ }
    return "%.1f %s".format(value, units[i])
}
