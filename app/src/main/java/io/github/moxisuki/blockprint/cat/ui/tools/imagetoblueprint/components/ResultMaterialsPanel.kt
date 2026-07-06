package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R

@Composable
internal fun ResultMaterialsPanel(
    totalBlocks: Int,
    materialCounts: Map<String, Int>,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    when {
        errorMessage != null -> {
            Text(
                stringResource(R.string.itb_result_error, errorMessage),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = modifier,
            )
        }
        totalBlocks > 0 && materialCounts.isNotEmpty() -> {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.itb_result_total, totalBlocks),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.itb_result_materials),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                )
                val sorted = materialCounts.entries.sortedByDescending { it.value }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    sorted.take(10).forEach { (name, count) ->
                        val displayName = name.replace('_', ' ')
                            .split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "× $count",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (sorted.size > 10) {
                        Text(
                            "+ ${sorted.size - 10} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
        else -> {
            Text(
                stringResource(R.string.itb_no_result),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = modifier,
            )
        }
    }
}
