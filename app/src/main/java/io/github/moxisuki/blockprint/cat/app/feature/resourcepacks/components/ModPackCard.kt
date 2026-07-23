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
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton

@Composable
internal fun ModPackCard(
    entry: ResourcePackEntry,
    onRedownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.displayName)
            val status = when {
                entry.fileCount == 0 -> stringResource(R.string.resourcepacks_mod_empty_pack)
                else -> "v${entry.version} · ${entry.fileCount} files · ${entry.namespaces.joinToString(",")}"
            }
            Text(status)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = onRedownload, text = stringResource(R.string.render_redownload))
                TextButton(onClick = onDelete, text = stringResource(R.string.action_delete))
            }
        }
    }
}
