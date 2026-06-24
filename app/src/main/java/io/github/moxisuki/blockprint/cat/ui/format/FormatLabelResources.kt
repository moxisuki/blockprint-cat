package io.github.moxisuki.blockprint.cat.ui.format

import androidx.annotation.StringRes
import io.github.moxisuki.blockprint.core.SchematicFormat
import io.github.moxisuki.blockprint.cat.R

/**
 * Map a [SchematicFormat] to its short-label string resource. Used by list
 * chips and any other UI site that shows a one-or-two-word format label.
 *
 * The mapping is intentionally separate from [FormatCatalog]: the catalog
 * stays Android-resource-free so it can be unit-tested without Robolectric,
 * and this function is the one place where i18n enters the format-display
 * flow. Adding a new [SchematicFormat] value? Add the enum case here AND a
 * matching `format_short_*` string resource — the `when` exhaustiveness will
 * tell you when you've forgotten the first.
 */
@StringRes
fun formatShortLabelRes(format: SchematicFormat): Int = when (format) {
    SchematicFormat.Litematica -> R.string.format_short_litematica
    SchematicFormat.Sponge -> R.string.format_short_worldedit
    SchematicFormat.Structure -> R.string.format_short_nbt
    SchematicFormat.PartialNbt -> R.string.format_short_nbt
    SchematicFormat.BuildingHelper -> R.string.format_short_building_helper
    SchematicFormat.Unknown -> R.string.format_short_unknown
}

/**
 * Map a [SchematicFormat] to its long-label string resource (used in detail
 * rows and convert-dialog radio rows, includes the file extension in
 * parens). Like [formatShortLabelRes], the catalog stays resource-free and
 * this function centralises the i18n mapping.
 */
@StringRes
fun formatLongLabelRes(format: SchematicFormat): Int = when (format) {
    SchematicFormat.Litematica -> R.string.format_long_litematica
    SchematicFormat.Sponge -> R.string.format_long_worldedit
    SchematicFormat.Structure -> R.string.format_long_nbt_vanilla
    SchematicFormat.PartialNbt -> R.string.format_long_nbt_generic
    SchematicFormat.BuildingHelper -> R.string.format_long_building_helper
    SchematicFormat.Unknown -> R.string.format_long_unknown
}
