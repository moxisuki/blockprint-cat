package io.github.moxisuki.blockprint.cat.data.blueprint

import org.junit.Assert.assertEquals
import org.junit.Test

class OutputFileNameTest {

    @Test
    fun litematica_appends_converted_and_litematic_ext() {
        assertEquals(
            "my_castle_converted.litematic",
            outputFileName("my_castle.litematic", "litematic"),
        )
    }

    @Test
    fun sponge_uses_schem_extension_not_schematic() {
        assertEquals(
            "my_castle_converted.schem",
            outputFileName("my_castle.litematic", "schem"),
        )
    }

    @Test
    fun structure_uses_nbt_extension() {
        assertEquals(
            "my_castle_converted.nbt",
            outputFileName("my_castle.litematic", "nbt"),
        )
    }

    @Test
    fun buildingHelper_uses_json_extension() {
        assertEquals(
            "my_castle_converted.json",
            outputFileName("my_castle.litematic", "json"),
        )
    }

    @Test
    fun name_with_no_extension_keeps_stem() {
        assertEquals(
            "my_castle_converted.schem",
            outputFileName("my_castle", "schem"),
        )
    }

    @Test
    fun name_with_dotted_stem_keeps_full_stem() {
        // substringBeforeLast('.', name) treats the whole prefix as the stem,
        // even if the stem itself contains dots.
        assertEquals(
            "v1.0.castle_converted.schem",
            outputFileName("v1.0.castle.litematic", "schem"),
        )
    }
}
