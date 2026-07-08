package io.github.moxisuki.blockprint.cat.ui.tools.blockpaint

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BlockPaintViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `paint sets cell to selected block`() = runTest(testDispatcher) {
        val vm = BlockPaintViewModel()
        vm.selectBlock("white_wool")
        vm.paint(3, 5)
        assertThat(vm.state.value.grid[3][5]).isEqualTo("white_wool")
    }

    @Test fun `paint ignores out-of-bounds coordinates`() = runTest(testDispatcher) {
        val vm = BlockPaintViewModel()
        vm.paint(999, 999)
        vm.paint(-1, -1)
        // 没崩就行
        assertThat(vm.state.value.grid).isNotNull()
    }

    @Test fun `erase tool sets cell to null`() = runTest(testDispatcher) {
        val vm = BlockPaintViewModel()
        vm.selectBlock("white_wool")
        vm.paint(2, 2)
        assertThat(vm.state.value.grid[2][2]).isEqualTo("white_wool")
        vm.setTool(PaintTool.Erase)
        vm.paint(2, 2)
        assertThat(vm.state.value.grid[2][2]).isNull()
    }

    @Test fun `clearCanvas empties the entire grid`() = runTest(testDispatcher) {
        val vm = BlockPaintViewModel()
        vm.selectBlock("white_wool")
        vm.paint(1, 1)
        vm.paint(2, 3)
        vm.clearCanvas()
        val grid = vm.state.value.grid
        for (x in grid.indices) {
            for (y in grid[0].indices) {
                assertThat(grid[x][y]).isNull()
            }
        }
    }

    @Test fun `setSize grows grid and preserves overlapping content`() = runTest(testDispatcher) {
        val vm = BlockPaintViewModel()
        vm.selectBlock("white_wool")
        vm.paint(0, 0)
        vm.setSize(8, 8)  // 默认 32 → 缩到 8
        assertThat(vm.state.value.width).isEqualTo(8)
        assertThat(vm.state.value.height).isEqualTo(8)
        // (0,0) 还在 → 保留
        assertThat(vm.state.value.grid[0][0]).isEqualTo("white_wool")
    }

    @Test fun `setSize clamps to MIN and MAX bounds`() = runTest(testDispatcher) {
        val vm = BlockPaintViewModel()
        vm.setSize(1, 1)
        assertThat(vm.state.value.width).isEqualTo(BlockPaintState.MIN_SIZE)
        vm.setSize(9999, 9999)
        assertThat(vm.state.value.width).isEqualTo(BlockPaintState.MAX_SIZE)
    }

    @Test fun `selectBlock with unknown id falls back to null`() = runTest(testDispatcher) {
        val vm = BlockPaintViewModel()
        vm.selectBlock("not_a_real_block")
        assertThat(vm.state.value.selectedBlockId).isNull()
    }

    @Test fun `selectBlock switches to Paint tool automatically`() = runTest(testDispatcher) {
        val vm = BlockPaintViewModel()
        vm.setTool(PaintTool.Erase)
        vm.selectBlock("white_wool")
        assertThat(vm.state.value.tool).isEqualTo(PaintTool.Paint)
    }

    @Test fun `paint on same cell with same block is a no-op`() = runTest(testDispatcher) {
        val vm = BlockPaintViewModel()
        vm.selectBlock("white_wool")
        vm.paint(3, 3)
        val tsBefore = vm.state.value.lastUpdatedAt
        // 同一格、同一方块 → 不会触发 lastUpdatedAt 变化
        vm.paint(3, 3)
        assertThat(vm.state.value.lastUpdatedAt).isEqualTo(tsBefore)
    }
}
