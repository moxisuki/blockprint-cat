package io.github.moxisuki.blockprint.cat.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.category.CategoryEntity

@Composable
fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorIdx: Int, patternIdx: Int) -> Unit,
) {
    CategoryEditorDialog(
        title = stringResource(R.string.cat_dialog_new_title),
        initialName = "",
        initialColorIdx = 0,
        initialPatternIdx = 0,
        showMeta = false,
        confirmLabel = stringResource(R.string.cat_dialog_btn_create),
        destructiveLabel = null,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onDestructive = null,
    )
}

@Composable
fun EditCategoryDialog(
    category: CategoryEntity,
    blueprintCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorIdx: Int, patternIdx: Int) -> Unit,
    onDelete: () -> Unit,
) {
    CategoryEditorDialog(
        title = stringResource(R.string.cat_dialog_edit_title, category.name),
        initialName = category.name,
        initialColorIdx = category.colorIdx,
        initialPatternIdx = category.patternIdx,
        showMeta = true,
        metaText = pluralStringResource(R.plurals.category_count, blueprintCount, blueprintCount),
        confirmLabel = stringResource(R.string.cat_dialog_btn_save),
        destructiveLabel = stringResource(R.string.cat_dialog_btn_delete_cat),
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        onDestructive = onDelete,
    )
}

@Composable
private fun CategoryEditorDialog(
    title: String,
    initialName: String,
    initialColorIdx: Int,
    initialPatternIdx: Int,
    showMeta: Boolean,
    metaText: String? = null,
    confirmLabel: String,
    destructiveLabel: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorIdx: Int, patternIdx: Int) -> Unit,
    onDestructive: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var colorIdx by rememberSaveable { mutableStateOf(initialColorIdx) }
    var patternIdx by rememberSaveable { mutableStateOf(initialPatternIdx) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.cat_dialog_label_name),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.cat_dialog_label_color),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(168.dp),
                ) {
                    items(CategoryCover.palette.size) { idx ->
                        CoverSwatch(
                            colorIdx = idx,
                            patternIdx = patternIdx,
                            selected = colorIdx == idx,
                            onClick = { colorIdx = idx },
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.cat_dialog_label_pattern),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.height(4.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(168.dp),
                ) {
                    items(CategoryCover.patterns.size) { idx ->
                        CoverSwatch(
                            colorIdx = colorIdx,
                            patternIdx = idx,
                            selected = patternIdx == idx,
                            onClick = { patternIdx = idx },
                        )
                    }
                }

                if (showMeta && metaText != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                    ) {
                        Text(text = metaText, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotEmpty()) onConfirm(trimmed, colorIdx, patternIdx)
                },
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            Row {
                if (destructiveLabel != null && onDestructive != null) {
                    TextButton(onClick = onDestructive) {
                        Text(destructiveLabel, color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

@Composable
private fun CoverSwatch(
    colorIdx: Int,
    patternIdx: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick),
    ) {
        CategoryCoverView(
            colorIdx = colorIdx,
            patternIdx = patternIdx,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        )
    }
}