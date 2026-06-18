package io.github.moxisuki.blockprint.cat.ui.bridge

import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Column
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Spacer
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.height
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.padding
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.width
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.Icons
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.filled.Download
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.filled.Visibility
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.HorizontalDivider
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.Icon
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.MaterialTheme
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.ModalBottomSheet
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.Text
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.TextButton
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.runtime.Composable
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.Modifier
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.text.style.TextOverflow
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint

/**
 * BottomSheet shown when the user taps a [PcBlueprintCard].
 *
 * Two actions, both currently invoke the same download path (per spec):
 *  - "下载到本地" — same callback, distinct UX wording
 *  - "仅查看"    — same callback, distinct UX wording
 *
 * Kept as separate callbacks so future behavior can diverge
 * (e.g. "仅查看" might skip persistence if the file is already local).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PcActionSheet(
    blueprint: RemoteBlueprint,
    onDownloadOnly: () -> Unit,
    onViewOnly: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                text = blueprint.name.ifBlank { blueprint.fileName },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = blueprint.fileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${blueprint.width}×${blueprint.height}×${blueprint.depth}  ·  方块: ${formatNumber(blueprint.blocks)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            TextButton(
                onClick = {
                    onDownloadOnly()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pc_action_download))
            }
            TextButton(
                onClick = {
                    onViewOnly()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.pc_action_view))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}