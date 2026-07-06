package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.BlockFilter

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BlockFilterChips(
    activeFilters: Set<BlockFilter>,
    onToggleFilter: (BlockFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BlockFilter.entries.forEach { filter ->
            FilterChip(
                selected = activeFilters.contains(filter),
                onClick = { onToggleFilter(filter) },
                label = { Text(stringResource(filter.labelRes)) },
            )
        }
    }
}
