package io.github.moxisuki.blockprint.cat.ui.tools.blockpaint

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockCatalog
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BlockPaintRendererTest {

    @Test fun `renderToBitmap produces 1 to 1 pixel bitmap`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val grid = Array(8) { arrayOfNulls<String>(8) }
        grid[0][0] = "white_wool"
        val (bitmap, _) = BlockPaintRenderer.renderToBitmap(ctx, grid)
        assertThat(bitmap.width).isEqualTo(8)
        assertThat(bitmap.height).isEqualTo(8)
    }

    @Test fun `renderToBitmap counts only non-null cells in materials`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val grid = Array(4) { arrayOfNulls<String>(4) }
        grid[0][0] = "white_wool"
        grid[1][1] = "white_wool"
        grid[2][2] = "red_wool"
        val (_, materials) = BlockPaintRenderer.renderToBitmap(ctx, grid)
        assertThat(materials["white_wool"]).isEqualTo(2)
        assertThat(materials["red_wool"]).isEqualTo(1)
        assertThat(materials.size).isEqualTo(2)
    }

    @Test fun `renderToBitmap returns empty materials for empty grid`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val grid = Array(4) { arrayOfNulls<String>(4) }
        val (_, materials) = BlockPaintRenderer.renderToBitmap(ctx, grid)
        assertThat(materials).isEmpty()
    }

    @Test fun `colorFor unknown block returns fallback gray`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        // 即使是不存在的 blockId，colorFor 也应该返回兜底色（不抛）
        val argb = BlockPaintRenderer.colorFor(ctx, "does_not_exist")
        assertThat(argb).isNotEqualTo(0)
    }

    @Test fun `colorFor known block returns cached value on second call`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = BlockPaintRenderer.colorFor(ctx, "white_wool")
        val second = BlockPaintRenderer.colorFor(ctx, "white_wool")
        assertThat(first).isEqualTo(second)
    }

    @Test fun `BlockGroup enum has 11 entries in declared order`() {
        // 用户要求"方块列表按照分组排序"——BlockPalette 实际按 BlockGroup.entries 顺序
        // 展示（enum 声明顺序），所以验证这个顺序是稳定的
        assertThat(
            io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockGroup.entries.map { it.name },
        ).isEqualTo(
            listOf(
                "WOOL",
                "CONCRETE",
                "TERRACOTTA",
                "STONE",
                "SOIL",
                "WOOD",
                "JEWEL",
                "GLASS",
                "GLAZED",
                "LIGHT",
                "ORE",
            ),
        )
    }
}
