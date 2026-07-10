package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow
import io.github.moxisuki.blockprint.cat.ui.format.FormatFilter

// Animation tuning constants — single source so all chip transitions feel coherent.
private const val CHIP_COLOR_TWEEN_MS = 160

/**
 * Unified filter panel for the Local tab. Renders inside the collapsible
 * BlueprintFilterBar (toggled by the filter icon in the AppBar).
 *
 * Layout (3 compact rows, no section labels — chips are self-explanatory):
 *   ┌─────────────────────────────────┐
 *   │ 🔍 搜索...                [×]  │  ← search
 *   ├─────────────────────────────────┤
 *   │ [全部 35] [城堡 12] [+]新建    │  ← categories + add button as last chip
 *   ├─────────────────────────────────┤
 *   │ [全部] [Lite] [Schem] [JSON]... │  ← format
 *   └─────────────────────────────────┘
 *
 * Performance: each child is a separate composable that takes stable params
 * + remembered lambdas, so changing one filter only recomposes the affected
 * chip — not the whole panel. Chips use [animateColorAsState] for smooth
 * selection transitions without animation interruption on rapid taps.
 */
@Composable
internal fun HomeFilterPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<CategoryRow>,
    selectedCategoryId: String?,
    onCategorySelect: (CategoryRow) -> Unit,
    onCategoryLongClick: (CategoryRow) -> Unit,
    onAddCategory: () -> Unit,
    selectedFormat: FormatFilter,
    onFormatChange: (FormatFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
        )
        CategoryChipRow(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelect = onCategorySelect,
            onCategoryLongClick = onCategoryLongClick,
            onAddCategory = onAddCategory,
        )
        FormatChipRow(
            selectedFormat = selectedFormat,
            onFormatChange = onFormatChange,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                stringResource(R.string.home_filter_search_hint),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        singleLine = true,
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        ),
    )
}

@Composable
private fun CategoryChipRow(
    categories: List<CategoryRow>,
    selectedCategoryId: String?,
    onCategorySelect: (CategoryRow) -> Unit,
    onCategoryLongClick: (CategoryRow) -> Unit,
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(
            items = categories,
            key = { row ->
                when (row) {
                    is CategoryRow.All -> "cat-all"
                    is CategoryRow.Real -> "cat-${row.entity.id}"
                }
            },
        ) { row ->
            val isSelected = when (row) {
                is CategoryRow.All -> selectedCategoryId == null
                is CategoryRow.Real -> selectedCategoryId == row.entity.id
            }
            CategoryFilterChip(
                label = when (row) {
                    is CategoryRow.All -> stringResource(R.string.home_category_all)
                    is CategoryRow.Real -> row.entity.name
                },
                count = row.count,
                selected = isSelected,
                onClick = remember(row) { { onCategorySelect(row) } },
                onLongClick = remember(row) { { onCategoryLongClick(row) } },
            )
        }
        item(key = "cat-add") {
            AddCategoryChip(
                onClick = onAddCategory,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val targetBg = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val targetFg = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
    val bg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(CHIP_COLOR_TWEEN_MS),
        label = "categoryChipBg",
    )
    val fg by animateColorAsState(
        targetValue = targetFg,
        animationSpec = tween(CHIP_COLOR_TWEEN_MS),
        label = "categoryChipFg",
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = "$label ${pluralStringResource(R.plurals.category_count, count, count)}",
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddCategoryChip(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.cd_category_add),
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = " " + stringResource(R.string.action_new),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun FormatChipRow(
    selectedFormat: FormatFilter,
    onFormatChange: (FormatFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Static list — hoisted to remember to avoid reallocating per recomposition.
    val formats = remember {
        listOf(
            FormatEntry(FormatFilter.All, R.string.home_filter_format_all),
            FormatEntry(FormatFilter.Litematica, R.string.format_filter_litematica),
            FormatEntry(FormatFilter.Schematic, R.string.format_filter_schematic),
            FormatEntry(FormatFilter.Json, R.string.format_filter_json),
            FormatEntry(FormatFilter.Nbt, R.string.format_filter_nbt),
        )
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(
            items = formats,
            key = { it.format },
        ) { entry ->
            FormatChipFilter(
                label = stringResource(entry.labelRes),
                selected = selectedFormat == entry.format,
                onClick = remember(entry.format) { { onFormatChange(entry.format) } },
            )
        }
    }
}

/** Wrapper pair so the formats list is a single immutable list of pairs. */
private data class FormatEntry(
    val format: FormatFilter,
    @StringRes val labelRes: Int,
)
