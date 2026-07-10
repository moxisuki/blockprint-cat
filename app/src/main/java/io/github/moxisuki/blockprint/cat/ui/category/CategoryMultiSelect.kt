package io.github.moxisuki.blockprint.cat.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R

// TODO(i18n): `cat_move_dialog_title` is used here as a hack to extract the
// "移动" / "Move" prefix; consider adding a dedicated `cat_multi_move` key for
// cleanliness so the wording isn't tied to the dialog title format.

/**
 * Top app-bar shown while multi-select is active.
 *
 * Lays out: [Cancel] icon, "已选 N 项 / N selected" label, [Select-all] toggle.
 * Uses a faint primary tint so the user can tell at a glance that they're in
 * selection mode (the rest of the screen is unchanged).
 */
@Composable
fun MultiSelectAppBar(
    selectedCount: Int,
    allSelected: Boolean,
    onCancel: () -> Unit,
    onToggleSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.cat_multi_count, selectedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            IconButton(onClick = onToggleSelectAll) {
                Icon(
                    imageVector = if (allSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = "Select all",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Bottom action bar shown while multi-select is active.
 *
 * Two equally-weighted buttons:
 *  - Move (primary tint, opens move dialog)
 *  - Delete (error tint, triggers delete confirm)
 */
@Composable
fun MultiSelectBottomBar(
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Use the dialog-title string minus its "%1$d 项到" / " items to" tail
            // as a stand-in for "移动" / "Move". Once a dedicated key is added,
            // swap this out.
            val moveLabel = stringResource(R.string.cat_move_dialog_title, 0)
                .replace(Regex(""".*?(?=[^%\d])"""), "")
                .ifBlank { "Move" }
            MultiBarButton(
                icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                label = moveLabel,
                onClick = onMove,
                primary = true,
                modifier = Modifier.weight(1f),
            )
            MultiBarButton(
                icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                label = stringResource(R.string.action_delete),
                onClick = onDelete,
                primary = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MultiBarButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (primary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error.copy(alpha = 0.0f)
    }
    val fg = if (primary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg),
        color = bg,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            icon()
            Text(
                text = "  $label",
                color = fg,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}