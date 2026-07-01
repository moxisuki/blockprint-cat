package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.ui.format.BadgeColor
import io.github.moxisuki.blockprint.cat.ui.format.FormatCatalog
import io.github.moxisuki.blockprint.cat.ui.format.formatShortLabelRes

/**
 * Format filter chip used by [BlueprintFilterBar] (Local tab toolbar). Visually
 * distinguishes the selected chip from the unselected ones by switching the
 * container colour to the theme primary. Stays compact (28dp tall) so it sits
 * cleanly inside the FlowRow below the search field.
 */
@Composable
internal fun FormatChipFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.height(28.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    )
}

/**
 * Small format badge attached to the right of each blueprint card title.
 * The chip background tint follows [FormatCatalog] → [BadgeColor] so each
 * format (Litematica / Schematic / BuildingHelper / etc.) gets a consistent
 * visual treatment across the Local list, community list, and detail views.
 *
 * Catalog currently emits Primary/Secondary/Outline; anything else (Tertiary,
 * future colours) falls back to the neutral Outline tint.
 */
@Composable
internal fun FormatChip(format: io.github.moxisuki.blockprint.core.SchematicFormat) {
    val display = FormatCatalog.from(format)
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
            stringResource(formatShortLabelRes(format)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}