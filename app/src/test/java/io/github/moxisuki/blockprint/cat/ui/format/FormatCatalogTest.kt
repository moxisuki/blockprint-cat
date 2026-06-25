package io.github.moxisuki.blockprint.cat.ui.format

import io.github.moxisuki.blockprint.core.SchematicFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatCatalogTest {

    @Test
    fun litematica_maps_to_litematica_short_litematic_long() {
        val d = FormatCatalog.from(SchematicFormat.Litematica)
        assertEquals("Litematica", d.shortLabel)
        assertEquals("Litematica (.litematic)", d.longLabel)
        assertEquals("litematic", d.fileExtension)
        assertEquals(BadgeColor.Primary, d.badgeColor)
        assertEquals(FormatFilter.Litematica, d.filterCategory)
    }

    @Test
    fun sponge_maps_to_worldedit_short_and_schematic_long() {
        val d = FormatCatalog.from(SchematicFormat.Sponge)
        assertEquals("WorldEdit", d.shortLabel)
        assertEquals("WorldEdit Schematic (.schem)", d.longLabel)
        assertEquals("schem", d.fileExtension)
        assertEquals(BadgeColor.Secondary, d.badgeColor)
        assertEquals(FormatFilter.Schematic, d.filterCategory)
    }

    @Test
    fun structure_maps_to_nbt_vanilla() {
        val d = FormatCatalog.from(SchematicFormat.Structure)
        assertEquals("NBT", d.shortLabel)
        assertEquals("Vanilla (.nbt)", d.longLabel)
        assertEquals("nbt", d.fileExtension)
        assertEquals(BadgeColor.Outline, d.badgeColor)
        assertEquals(FormatFilter.Nbt, d.filterCategory)
    }

    @Test
    fun partialNbt_maps_to_nbt_generic() {
        val d = FormatCatalog.from(SchematicFormat.PartialNbt)
        assertEquals("NBT", d.shortLabel)
        assertEquals("Generic (.nbt)", d.longLabel)
        assertEquals("nbt", d.fileExtension)
        assertEquals(BadgeColor.Outline, d.badgeColor)
        assertEquals(FormatFilter.Nbt, d.filterCategory)
    }

    @Test
    fun buildingHelper_maps_to_json() {
        val d = FormatCatalog.from(SchematicFormat.BuildingHelper)
        assertEquals("Building Helper", d.shortLabel)
        assertEquals("Building Helper (.json)", d.longLabel)
        assertEquals("json", d.fileExtension)
        assertEquals(BadgeColor.Outline, d.badgeColor)
        assertEquals(FormatFilter.Json, d.filterCategory)
    }

    @Test
    fun unknown_maps_to_unknown_label_no_extension() {
        val d = FormatCatalog.from(SchematicFormat.Unknown)
        assertEquals("Unknown", d.shortLabel)
        assertEquals("Unknown", d.longLabel)
        assertEquals("", d.fileExtension)
        assertEquals(BadgeColor.Outline, d.badgeColor)
        assertEquals(FormatFilter.Nbt, d.filterCategory)
    }

    @Test
    fun convertTargetsExcluding_litematica_source_omits_litematica() {
        val targets = FormatCatalog.convertTargetsExcluding(SchematicFormat.Litematica)
        val formats = targets.map { it.schematicFormat }
        assertEquals(2, targets.size)
        assertTrue(SchematicFormat.Sponge in formats)
        assertTrue(SchematicFormat.Structure in formats)
        assertTrue(SchematicFormat.BuildingHelper !in formats)
        assertTrue(SchematicFormat.Litematica !in formats)
    }

    @Test
    fun convertTargetsExcluding_partialNbt_source_returns_three_valid_targets() {
        // PartialNbt is NOT in the writable list (it's a read-side category),
        // so passing it as current means we offer the 3 writable targets.
        // BuildingHelper is currently disabled as a convert target — see
        // `FormatCatalog.convertTargetsExcluding` for the re-enable recipe.
        val targets = FormatCatalog.convertTargetsExcluding(SchematicFormat.PartialNbt)
        val formats = targets.map { it.schematicFormat }
        assertEquals(3, targets.size)
        assertTrue(SchematicFormat.Litematica in formats)
        assertTrue(SchematicFormat.Sponge in formats)
        assertTrue(SchematicFormat.Structure in formats)
        assertTrue(SchematicFormat.BuildingHelper !in formats)
        assertTrue(SchematicFormat.PartialNbt !in formats)
    }

    @Test
    fun convertTargetsExcluding_buildingHelper_source_returns_empty() {
        // BuildingHelper is currently disabled as both a convert source AND
        // a convert target. The UI treats an empty list as "no convert
        // available" and hides the entry point.
        val targets = FormatCatalog.convertTargetsExcluding(SchematicFormat.BuildingHelper)
        assertTrue(targets.isEmpty())
    }

    @Test
    fun convertTargetsExcluding_unknown_source_returns_three_valid_targets() {
        // Unknown is NOT a valid convert target itself, but as a source we
        // offer the user the 3 writable formats. BuildingHelper excluded.
        val targets = FormatCatalog.convertTargetsExcluding(SchematicFormat.Unknown)
        assertEquals(3, targets.size)
    }
}
