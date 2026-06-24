package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint
import io.github.moxisuki.blockprint.cat.ui.format.BadgeColor
import io.github.moxisuki.blockprint.cat.ui.format.FormatCatalog
import io.github.moxisuki.blockprint.cat.ui.format.formatShortLabelRes
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import io.github.moxisuki.blockprint.core.SchematicFormat

/**
 * Map a [RemoteBlueprint] from the PC side to a [SchematicFormat].
 *
 * The PC-side BlockPrint Link mod sends the `format` field as the
 * canonical `SchematicFormat.name()` value (`Litematica` / `Sponge` /
 * `Structure` / `BuildingHelper` / `PartialNbt` / `Unknown`). We parse
 * it directly with `SchematicFormat.valueOf`. For unrecognized values
 * (forward-compat with new enum variants or older mod versions),
 * fall back to inspecting the file extension on [RemoteBlueprint.fileName].
 */
private fun formatOf(blueprint: RemoteBlueprint): SchematicFormat {
    runCatching { return SchematicFormat.valueOf(blueprint.format) }
    val name = blueprint.fileName.lowercase()
    return when {
        name.endsWith(".litematic") -> SchematicFormat.Litematica
        name.endsWith(".schem") || name.endsWith(".schematic") -> SchematicFormat.Sponge
        name.endsWith(".nbt") -> SchematicFormat.Structure
        name.endsWith(".json") -> SchematicFormat.BuildingHelper
        else -> SchematicFormat.Unknown
    }
}

/**
 * Compact card for a [RemoteBlueprint] shown in HomeScreen's PC tab.
 *
 * Visual style mirrors the local [io.github.moxisuki.blockprint.cat.ui.home.HomeBlueprintCard]:
 * 4dp primary-color left bar + title row + two-line subtitle + format chip
 * on the right. The chip reads the PC-side `format` field, which the
 * BlockPrint Link mod sends as the canonical [SchematicFormat] enum name.
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
            Spacer(Modifier.width(8.dp))
            val display = FormatCatalog.from(formatOf(blueprint))
            val bg = when (display.badgeColor) {
                BadgeColor.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                BadgeColor.Secondary -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
            }
            Box(
                Modifier
                    .background(bg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text(
                    stringResource(formatShortLabelRes(display.schematicFormat)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
