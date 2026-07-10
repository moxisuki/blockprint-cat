package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow
import io.github.moxisuki.blockprint.cat.ui.format.FormatFilter

/**
 * Unified filter panel for the Local tab. Renders inside the collapsible
 * BlueprintFilterBar (toggled by the filter icon in the AppBar). Combines:
 *   - search field (debounced by caller)
 *   - category chips row (with counts, + new button, long-press to edit)
 *   - format chips row
 *
 * Categories were previously a separate rail above this panel; folding them
 * in keeps the screen compact and lets the user collapse everything with a
 * single tap.
 */
@OptIn(ExperimentalFoundationApi::class)
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
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
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = if (query.isNotEmpty()) {{
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }} else null,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        // Category section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.home_filter_label_category),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onAddCategory,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_category_add),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        LazyRow(
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
                    onClick = { onCategorySelect(row) },
                    onLongClick = { onCategoryLongClick(row) },
                )
            }
        }

        // Format section
        Text(
            text = stringResource(R.string.home_filter_label_format),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FormatChipFilter(
                label = stringResource(R.string.home_filter_format_all),
                selected = selectedFormat == FormatFilter.All,
            ) { onFormatChange(FormatFilter.All) }
            FormatChipFilter(
                label = stringResource(R.string.format_filter_litematica),
                selected = selectedFormat == FormatFilter.Litematica,
            ) { onFormatChange(FormatFilter.Litematica) }
            FormatChipFilter(
                label = stringResource(R.string.format_filter_schematic),
                selected = selectedFormat == FormatFilter.Schematic,
            ) { onFormatChange(FormatFilter.Schematic) }
            FormatChipFilter(
                label = stringResource(R.string.format_filter_json),
                selected = selectedFormat == FormatFilter.Json,
            ) { onFormatChange(FormatFilter.Json) }
            FormatChipFilter(
                label = stringResource(R.string.format_filter_nbt),
                selected = selectedFormat == FormatFilter.Nbt,
            ) { onFormatChange(FormatFilter.Nbt) }
        }
    }
}

/**
 * Category filter chip. Shows label + pluralized count. Long-press is
 * supported so users can edit the category. Custom-painted (not FilterChip)
 * because Material3 FilterChip has no built-in onLongClick.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategoryFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
             else MaterialTheme.colorScheme.onSurface
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
        )
    }
}
