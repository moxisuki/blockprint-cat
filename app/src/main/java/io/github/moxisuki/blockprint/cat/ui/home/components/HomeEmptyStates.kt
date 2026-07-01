package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R

/**
 * "No blueprints yet" placeholder shown in the Local tab when the SAF folder
 * has no `.litematic` / `.schem` / similar files. Surfaces the resolved SAF
 * folder name (if any) so the user knows where to drop new schematics, and
 * surfaces a button that re-opens the folder picker.
 */
@Composable
internal fun EmptyHomeState(
    modifier: Modifier = Modifier,
    onScanFolder: () -> Unit = {},
    safFolderName: String? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp).wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (safFolderName != null) stringResource(R.string.home_empty_with_folder)
            else stringResource(R.string.home_empty_no_folder),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (safFolderName != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.home_empty_folder_label, safFolderName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onScanFolder) {
            Text(
                if (safFolderName != null) stringResource(R.string.home_rescan_folder)
                else stringResource(R.string.home_pick_folder)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.home_pick_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.home_subdir_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

/**
 * "No PC schematics yet" placeholder shown in the PC tab when the bridge is
 * Connected but the server returned an empty list. Differs from [EmptyHomeState]
 * in that no folder-picker button is offered — the user needs to put files on
 * the PC side first.
 */
@Composable
internal fun EmptyPcState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.home_pc_empty_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.home_pc_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}
