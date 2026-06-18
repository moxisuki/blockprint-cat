package io.github.moxisuki.blockprint.cat.ui.bridge

import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.background
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.clickable
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Box
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Column
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Row
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Spacer
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.height
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.padding
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.size
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.width
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.Icons
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.filled.MoreVert
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.Card
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.CardDefaults
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.Icon
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.MaterialTheme
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.Text
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.runtime.Composable
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.Alignment
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.Modifier
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.draw.clip
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.text.style.TextOverflow
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint

/**
 * Compact card for a [RemoteBlueprint] shown in HomeScreen's PC tab.
 *
 * Visual style mirrors the local [io.github.moxisuki.blockprint.cat.ui.home.HomeBlueprintCard]:
 * 4dp primary-color left bar + title row + two-line subtitle. No
 * format chip, no description — kept minimal per spec.
 */
@Composable
fun PcBlueprintCard(
    blueprint: RemoteBlueprint,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = blueprint.name.ifBlank { blueprint.fileName },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = blueprint.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${blueprint.width}×${blueprint.height}×${blueprint.depth}  ·  方块: ${formatNumber(blueprint.blocks)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}