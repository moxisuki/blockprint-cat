package io.github.moxisuki.blockprint.cat.app.feature.home.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CategoryManageSheet(
    show: Boolean,
    categories: List<BlueprintCategoryFilter>,
    onCreateCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editorTarget by remember { mutableStateOf<String?>(null) }
    var editorValue by remember { mutableStateOf("") }
    var isEditorVisible by remember { mutableStateOf(false) }
    val editorTitle = if (editorTarget == null) {
        stringResource(R.string.cat_dialog_new_title)
    } else {
        stringResource(R.string.cat_dialog_edit_title, editorTarget.orEmpty())
    }
    val confirmLabel = if (editorTarget == null) {
        stringResource(R.string.cat_dialog_btn_create)
    } else {
        stringResource(R.string.cat_dialog_btn_save)
    }
    val imeInsets = WindowInsets.ime
    val navigationBarsInsets = WindowInsets.navigationBars

    LaunchedEffect(show) {
        if (!show) {
            isEditorVisible = false
            editorTarget = null
            editorValue = ""
        }
    }

    OverlayBottomSheet(
        show = show,
        modifier = modifier.offset {
            val imeBottomPx = imeInsets.getBottom(this)
            val navigationBottomPx = navigationBarsInsets.getBottom(this)
            val keyboardOffsetPx = if (isEditorVisible) {
                val keyboardHeightPx = (imeBottomPx - navigationBottomPx).coerceAtLeast(0)
                if (keyboardHeightPx > 0) {
                    keyboardHeightPx + 12.dp.roundToPx()
                } else {
                    0
                }
            } else {
                0
            }
            IntOffset(x = 0, y = -keyboardOffsetPx)
        },
        title = if (isEditorVisible) {
            editorTitle
        } else {
            stringResource(R.string.cat_dialog_manage_title)
        },
        onDismissRequest = {
            if (isEditorVisible) {
                isEditorVisible = false
                editorTarget = null
            } else {
                onDismissRequest()
            }
        },
        insideMargin = androidx.compose.ui.unit.DpSize(
            width = 16.dp,
            height = if (isEditorVisible) 12.dp else 18.dp,
        ),
        defaultWindowInsetsPadding = false,
    ) {
        if (isEditorVisible) {
            CategoryNameEditor(
                value = editorValue,
                confirmLabel = confirmLabel,
                onValueChange = { editorValue = it },
                onDismissRequest = {
                    isEditorVisible = false
                    editorTarget = null
                },
                onConfirm = {
                    val target = editorTarget
                    if (target == null) {
                        onCreateCategory(editorValue)
                    } else {
                        onRenameCategory(target, editorValue)
                    }
                    isEditorVisible = false
                    editorTarget = null
                },
            )
        } else {
            CategoryManageContent(
                categories = categories,
                onCreateClick = {
                    editorTarget = null
                    editorValue = ""
                    isEditorVisible = true
                },
                onEditClick = { category ->
                    editorTarget = category.id
                    editorValue = category.label
                    isEditorVisible = true
                },
                onDeleteClick = onDeleteCategory,
            )
        }
    }
}

@Composable
private fun CategoryManageContent(
    categories: List<BlueprintCategoryFilter>,
    onCreateClick: () -> Unit,
    onEditClick: (BlueprintCategoryFilter) -> Unit,
    onDeleteClick: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.cd_category_add),
                color = MiuixTheme.colorScheme.onPrimary,
                style = MiuixTheme.textStyles.button,
            )
        }

        if (categories.isEmpty()) {
            EmptyManageCard()
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                Column {
                    categories.forEachIndexed { index, category ->
                        CategoryManageRow(
                            category = category,
                            accent = category.accentColor(index),
                            readOnly = category.id == HomeCategoryId.Uncategorized,
                            onEditClick = { onEditClick(category) },
                            onDeleteClick = { onDeleteClick(category.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryManageRow(
    category: BlueprintCategoryFilter,
    accent: Color,
    readOnly: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(accent),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.label,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = pluralStringResource(R.plurals.category_count, category.count, category.count),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
        IconButton(
            onClick = onEditClick,
            enabled = !readOnly,
            minWidth = 36.dp,
            minHeight = 36.dp,
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.cat_dialog_btn_save),
                tint = actionTint(readOnly),
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(
            onClick = onDeleteClick,
            enabled = !readOnly,
            minWidth = 36.dp,
            minHeight = 36.dp,
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.cat_dialog_btn_delete_cat),
                tint = actionTint(readOnly),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CategoryNameEditor(
    value: String,
    confirmLabel: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.cat_dialog_label_name),
            singleLine = true,
            textStyle = MiuixTheme.textStyles.body1,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismissRequest,
            )
            TextButton(
                text = confirmLabel,
                onClick = onConfirm,
                enabled = value.trim().isNotBlank(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}

@Composable
private fun EmptyManageCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(18.dp),
    ) {
        Text(
            text = stringResource(R.string.cat_dialog_manage_empty),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
private fun actionTint(disabled: Boolean): Color =
    if (disabled) {
        MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.35f)
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }

@Composable
private fun BlueprintCategoryFilter.accentColor(index: Int): Color {
    val accents = listOf(
        MiuixTheme.colorScheme.primary,
        MiuixTheme.colorScheme.onSurfaceContainer,
        MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
    return accents[index % accents.size]
}

@Preview(showBackground = true)
@Composable
private fun CategoryManageSheetPreview() {
    PreviewAppTheme {
        CategoryManageSheet(
            show = true,
            categories = listOf(
                BlueprintCategoryFilter("Machine", "Machine", 2),
                BlueprintCategoryFilter("Survival", "Survival", 3),
            ),
            onCreateCategory = {},
            onRenameCategory = { _, _ -> },
            onDeleteCategory = {},
            onDismissRequest = {},
        )
    }
}
