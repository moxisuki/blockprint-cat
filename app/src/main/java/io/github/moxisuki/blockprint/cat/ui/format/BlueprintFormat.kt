package io.github.moxisuki.blockprint.cat.ui.format

import io.github.moxisuki.blockprint.core.SchematicFormat

/** Category used by the home-screen format filter row. Mirrors the existing
 *  `FormatFilter` enum in `HomeScreen.kt` so filter logic stays compatible. */
enum class FormatFilter { All, Litematica, Schematic, Nbt, Json }

/** Color tier used by the format chip. Maps to Material 3 color roles. */
enum class BadgeColor { Primary, Secondary, Outline, Tertiary }

/**
 * UI-facing description of one [SchematicFormat]. Single source of truth —
 * every screen that displays a format (list chip, detail row, convert
 * dialog) reads from [FormatCatalog.from]. Do not hand-roll `when`
 * branches in UI code.
 *
 * - [shortLabel] is used in chip labels and filter chips (1-2 words).
 * - [longLabel] is used in the detail "Format" row and convert dialog rows
 *   (includes the file extension in parens).
 * - [fileExtension] is the literal string to pass to `BlueprintManager.convert`
 *   for the output filename. Empty string for [SchematicFormat.Unknown].
 * - [badgeColor] is the chip background tier (Material 3 color role).
 * - [filterCategory] groups formats into the home-screen filter categories.
 */
data class FormatDisplay(
    val schematicFormat: SchematicFormat,
    val shortLabel: String,
    val longLabel: String,
    val fileExtension: String,
    val badgeColor: BadgeColor,
    val filterCategory: FormatFilter,
)

/**
 * Centralised mapping from [SchematicFormat] to [FormatDisplay].
 *
 * Display rules:
 * - Litematica → "Litematica" / "Litematica (.litematic)"
 * - Sponge     → "WorldEdit" / "WorldEdit Schematic (.schem)"
 * - Structure  → "NBT" / "Vanilla (.nbt)"
 * - PartialNbt → "NBT" / "Generic (.nbt)"
 * - BuildingHelper → "Building Helper" / "Building Helper (.json)"
 * - Unknown    → "Unknown" / "Unknown"
 *
 * NBT-family (Structure / PartialNbt / Unknown) all share the "NBT" short
 * label and the [FormatFilter.Nbt] category. They differ in the long label.
 *
 * The "schematic" (Sponge v2) extension is intentionally NOT exposed —
 * core 0.1.28 only writes Sponge v3 (`.schem`); exposing a v2 option
 * would create a file whose extension lies about its content.
 */
object FormatCatalog {

    fun from(format: SchematicFormat): FormatDisplay = when (format) {
        SchematicFormat.Litematica -> FormatDisplay(
            schematicFormat = SchematicFormat.Litematica,
            shortLabel = "Litematica",
            longLabel = "Litematica (.litematic)",
            fileExtension = "litematic",
            badgeColor = BadgeColor.Primary,
            filterCategory = FormatFilter.Litematica,
        )
        SchematicFormat.Sponge -> FormatDisplay(
            schematicFormat = SchematicFormat.Sponge,
            shortLabel = "WorldEdit",
            longLabel = "WorldEdit Schematic (.schem)",
            fileExtension = "schem",
            badgeColor = BadgeColor.Secondary,
            filterCategory = FormatFilter.Schematic,
        )
        SchematicFormat.Structure -> FormatDisplay(
            schematicFormat = SchematicFormat.Structure,
            shortLabel = "NBT",
            longLabel = "Vanilla (.nbt)",
            fileExtension = "nbt",
            badgeColor = BadgeColor.Outline,
            filterCategory = FormatFilter.Nbt,
        )
        SchematicFormat.PartialNbt -> FormatDisplay(
            schematicFormat = SchematicFormat.PartialNbt,
            shortLabel = "NBT",
            longLabel = "Generic (.nbt)",
            fileExtension = "nbt",
            badgeColor = BadgeColor.Outline,
            filterCategory = FormatFilter.Nbt,
        )
        SchematicFormat.BuildingHelper -> FormatDisplay(
            schematicFormat = SchematicFormat.BuildingHelper,
            shortLabel = "Building Helper",
            longLabel = "Building Helper (.json)",
            fileExtension = "json",
            badgeColor = BadgeColor.Outline,
            filterCategory = FormatFilter.Json,
        )
        SchematicFormat.Unknown -> FormatDisplay(
            schematicFormat = SchematicFormat.Unknown,
            shortLabel = "Unknown",
            longLabel = "Unknown",
            fileExtension = "",
            badgeColor = BadgeColor.Outline,
            filterCategory = FormatFilter.Nbt,
        )
    }

    /**
     * The writable convert targets (Litematica / Sponge / Structure),
     * excluding the format that matches [current]. PartialNbt and Unknown
     * are NOT valid convert targets — they are read-side categories that
     * BlueprintConverter rejects — so they never appear here.
     *
     * BuildingHelper(.json) is also currently excluded — bidirectional
     * conversion involving BuildingHelper is temporarily disabled (see
     * caller-side `current == BuildingHelper` → empty-list guard). When
     * it's re-enabled, just add `SchematicFormat.BuildingHelper` back to
     * the [writable] list below.
     *
     * Used by the convert dialog to render its radio rows. The caller is
     * responsible for showing these labels via `stringResource` (for i18n)
     * using [FormatDisplay.shortLabel] / [FormatDisplay.longLabel] /
     * [FormatDisplay.fileExtension] as the keys.
     */
    fun convertTargetsExcluding(current: SchematicFormat): List<FormatDisplay> {
        val writable = listOf(
            SchematicFormat.Litematica,
            SchematicFormat.Sponge,
            SchematicFormat.Structure,
        )
        // BuildingHelper is excluded as a convert target AND as a convert
        // source — i.e. when the current format is BuildingHelper, no
        // conversion is offered. Until the bidirectional BuildingHelper
        // round-trip is re-enabled, callers should treat an empty list as
        // "no convert available for this blueprint" and hide the entry point.
        if (current == SchematicFormat.BuildingHelper) return emptyList()
        return writable
            .filter { it != current }
            .map { from(it) }
    }
}
