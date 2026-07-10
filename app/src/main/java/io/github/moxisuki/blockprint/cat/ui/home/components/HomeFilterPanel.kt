package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
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
 *   │ [全部] [Lite] [Schem] [JSON]... │  ← format
 *   └─────────────────────────────────┘
 *
 * Note: category chips used to live here too but were promoted to a top
 * CategoryPager for swipe navigation. This panel is now search + format only.
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
