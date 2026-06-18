package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity

@Composable
fun LazyItemScope.PairedRow(
    dev: PairedDeviceEntity,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onTokenUpdate: (String) -> Unit,
) {
    var showDelete by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showToken by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateItem(
                fadeInSpec = io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec.listItemEnter,
                placementSpec = io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec.listItemPlacement,
            )
            .clickable(onClick = onConnect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Computer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(dev.label ?: dev.host, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${dev.host}:${dev.wsPort}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { showToken = true }) {
            Icon(Icons.Default.Key, contentDescription = stringResource(R.string.bridge_edit_token), modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = { showRename = true }) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.bridge_action_rename))
        }
        IconButton(onClick = { showDelete = true }) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.bridge_action_delete))
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.bridge_action_delete_confirm_title)) },
            text = { Text(stringResource(R.string.bridge_action_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDelete = false
                }) { Text(stringResource(R.string.bridge_action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showRename) {
        var name by remember { mutableStateOf(dev.label ?: "") }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text(stringResource(R.string.bridge_rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(name.trim())
                    showRename = false
                }) { Text(stringResource(R.string.bridge_action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showToken) {
        var token by remember(dev.token) { mutableStateOf(dev.token) }
        AlertDialog(
            onDismissRequest = { showToken = false },
            title = { Text(stringResource(R.string.bridge_edit_token_title)) },
            text = {
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.bridge_edit_token_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = token.trim()
                        if (t.isNotBlank()) onTokenUpdate(t)
                        showToken = false
                    },
                    enabled = token.isNotBlank(),
                ) { Text(stringResource(R.string.bridge_action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showToken = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
