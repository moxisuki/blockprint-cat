package io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview

import io.github.moxisuki.pixelart.Block
import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.core.SchematicFormat
import io.github.moxisuki.blockprint.core.model.BlockPrintDocument
import org.junit.Test

/**
 * Pure JVM tests for [BlueprintBuilder] — no Android, no Hilt.
 * Mocks a 4×4 grid with two distinct blocks and verifies both region
 * dimension variants (WALL / FLAT) plus the fillRegions compaction pattern.
 */
class BlueprintBuilderTest {

    private val white = Block("white_wool", Triple(233, 236, 237), "wool")
    private val red = Block("red_wool", Triple(162, 47, 38), "wool")
    private val blue = Block("blue_wool", Triple(53, 57, 160), "wool")

    /**
     * 4×4 grid: row 0 = all red, row 1 = 2 white + 2 blue, row 2 = mixed,
     * row 3 = all null (air). The mix lets us verify palette deduplication
     * and the wall/flat orientation.
     */
    private fun grid(): Array<Array<Block?>> = arrayOf(
        arrayOf(red, red, red, red),
        arrayOf(white, white, blue, blue),
        arrayOf(blue, null, white, red),
        arrayOf(null, null, null, null),
    )

    @Test fun `WALL region has dims width x height x 1`() {
        val doc = BlueprintBuilder.buildDocument(
            grid = grid(),
            width = 4,
            height = 4,
            mode = BlueprintMode.WALL,
            format = SchematicFormat.Litematica,
            name = "wall_test",
        )
        val region = doc.regions.single()
        assertThat(region.width).isEqualTo(4)
        assertThat(region.height).isEqualTo(4)
        assertThat(region.depth).isEqualTo(1)
        // palette: [air, red, white, blue] in encounter order
        assertThat(region.palette.size).isEqualTo(4)
        assertThat(region.palette.get(0).name).isEqualTo("minecraft:air")
        assertThat(region.palette.entries.map { it.name }).containsExactly(
            "minecraft:air", "minecraft:red_wool", "minecraft:white_wool", "minecraft:blue_wool",
        ).inOrder()
        // WALL: image y=0 maps to region y=height-1 (墙顶). row 0 (all red) → y=3
        // y-major, depth=1, so index = y * W * D + z * W + x = y * 4 + x
        for (x in 0 until 4) {
            assertThat(region.getBlock(x, 3, 0)).isEqualTo(1)  // row 0 → y=3 → red
        }
        // image row 3 (all null) → region y=0 (墙底), so col 0 = air
        assertThat(region.getBlock(0, 0, 0)).isEqualTo(0)
    }

    @Test fun `FLAT region has dims width x 1 x height`() {
        val doc = BlueprintBuilder.buildDocument(
            grid = grid(),
            width = 4,
            height = 4,
            mode = BlueprintMode.FLAT,
            format = SchematicFormat.Litematica,
            name = "flat_test",
        )
        val region = doc.regions.single()
        assertThat(region.width).isEqualTo(4)
        assertThat(region.height).isEqualTo(1)
        assertThat(region.depth).isEqualTo(4)
        // image y=0 (row 0 = red) maps to region z = depth-1 = 3
        // red_wool palette idx = 1
        for (x in 0 until 4) {
            assertThat(region.getBlock(x, 0, 3)).isEqualTo(1)
        }
        // image y=3 (row 3 = all null) maps to region z=0 (air)
        for (x in 0 until 4) {
            assertThat(region.getBlock(x, 0, 0)).isEqualTo(0)
        }
    }

    @Test fun `document metadata is propagated`() {
        val doc: BlockPrintDocument = BlueprintBuilder.buildDocument(
            grid = grid(),
            width = 4,
            height = 4,
            mode = BlueprintMode.WALL,
            format = SchematicFormat.Litematica,
            name = "hello",
            author = "alice",
            description = "test region",
        )
        assertThat(doc.name).isEqualTo("hello")
        assertThat(doc.author).isEqualTo("alice")
        assertThat(doc.description).isEqualTo("test region")
        assertThat(doc.regions).hasSize(1)
    }

    @Test fun `fillRegions merges adjacent same-color cells horizontally`() {
        // 1x4 grid: red, red, blue, blue  → expect 2 fills (red x=0..1, blue x=2..3)
        val g = arrayOf(
            arrayOf<Block?>(red, red, blue, blue),
        )
        val fills = BlueprintBuilder.fillRegions(g, width = 4, height = 1, mode = BlueprintMode.WALL)
        assertThat(fills).hasSize(2)
        assertThat(fills[0]).isEqualTo(
            FillRegion(x1 = 0, y1 = 64, z1 = 0, x2 = 1, y2 = 64, z2 = 0, blockName = "minecraft:red_wool"),
        )
        assertThat(fills[1]).isEqualTo(
            FillRegion(x1 = 2, y1 = 64, z1 = 0, x2 = 3, y2 = 64, z2 = 0, blockName = "minecraft:blue_wool"),
        )
    }

    @Test fun `fillRegions skips air cells`() {
        // 1x5: red, red, null, blue, blue  → 2 fills (red x=0..1, blue x=3..4).
        // Null cells are marked processed but not emitted as fills — they're
        // already the region's default (air).
        val g = arrayOf(
            arrayOf<Block?>(red, red, null, blue, blue),
        )
        val fills = BlueprintBuilder.fillRegions(g, width = 5, height = 1, mode = BlueprintMode.WALL)
        assertThat(fills).hasSize(2)
        assertThat(fills[0]).isEqualTo(
            FillRegion(x1 = 0, y1 = 64, z1 = 0, x2 = 1, y2 = 64, z2 = 0, blockName = "minecraft:red_wool"),
        )
        assertThat(fills[1]).isEqualTo(
            FillRegion(x1 = 3, y1 = 64, z1 = 0, x2 = 4, y2 = 64, z2 = 0, blockName = "minecraft:blue_wool"),
        )
    }

    @Test fun `fillRegions FLAT mode flips image y to region z`() {
        // 3x2: row 0 = red red red, row 1 = blue blue blue
        // FLAT mode: image y=0 → region z=depth-1=1; image y=1 → region z=0
        val g = arrayOf(
            arrayOf<Block?>(red, red, red),
            arrayOf<Block?>(blue, blue, blue),
        )
        val fills = BlueprintBuilder.fillRegions(g, width = 3, height = 2, mode = BlueprintMode.FLAT)
        // Expect 2 fills: one red at z=1, one blue at z=0
        assertThat(fills).hasSize(2)
        val redFill = fills.single { it.blockName == "minecraft:red_wool" }
        val blueFill = fills.single { it.blockName == "minecraft:blue_wool" }
        // red row 0 → FLAT z = depth-1-y = 2-1-0 = 1
        assertThat(redFill.z1).isEqualTo(1)
        assertThat(redFill.z2).isEqualTo(1)
        // blue row 1 → FLAT z = 2-1-1 = 0
        assertThat(blueFill.z1).isEqualTo(0)
        assertThat(blueFill.z2).isEqualTo(0)
    }

    @Test fun `WALL region rawBlocks match grid via getBlock`() {
        // 2x2: row 0 = red white, row 1 = blue null
        // WALL: image y=0 → region y=1 (墙顶), y=1 → y=0 (墙底)
        val g = arrayOf(
            arrayOf<Block?>(red, white),
            arrayOf<Block?>(blue, null),
        )
        val region = BlueprintBuilder.buildDocument(
            grid = g, width = 2, height = 2,
            mode = BlueprintMode.WALL,
            format = SchematicFormat.Litematica,
            name = "x",
        ).regions.single()
        // palette = [air, red, white, blue]  → indices 0,1,2,3
        // image row 0 (red, white) → region y=1
        assertThat(region.getBlock(0, 1, 0)).isEqualTo(1) // red at top
        assertThat(region.getBlock(1, 1, 0)).isEqualTo(2) // white at top
        // image row 1 (blue, null) → region y=0
        assertThat(region.getBlock(0, 0, 0)).isEqualTo(3) // blue at bottom
        assertThat(region.getBlock(1, 0, 0)).isEqualTo(0) // air at bottom
    }
}