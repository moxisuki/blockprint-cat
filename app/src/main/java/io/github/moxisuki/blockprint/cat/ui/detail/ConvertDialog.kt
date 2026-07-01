package io.github.moxisuki.blockprint.cat.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.format.FormatCatalog
import io.github.moxisuki.blockprint.cat.ui.format.formatLongLabelRes

/**
 * Format-conversion dialog used by both phone and pad detail screens.
 * Show / hide + selected target index are owned by the caller. When
 * [visible] is false this returns immediately, so callers don't need to
 * branch on the visibility flag themselves.
 *
 * Confirms the convert action via [onConfirm]; the actual conversion is
 * delegated to the BridgeViewModel in the caller.
 */
@Composable
internal fun ConvertDialog(
    visible: Boolean,
    currentFormat: io.github.moxisuki.blockprint.core.SchematicFormat,
    selected: Int,
    onSelectedChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_convert_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.detail_convert_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                FormatCatalog.convertTargetsExcluding(currentFormat).forEachIndexed { idx, display ->
                    ConvertTargetRow(
                        label = stringResource(formatLongLabelRes(display.schematicFormat)),
                        selected = selected == idx,
                        onClick = { onSelectedChange(idx) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(stringResource(R.string.action_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Single radio row inside the convert dialog. */
@Composable
internal fun ConvertTargetRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
