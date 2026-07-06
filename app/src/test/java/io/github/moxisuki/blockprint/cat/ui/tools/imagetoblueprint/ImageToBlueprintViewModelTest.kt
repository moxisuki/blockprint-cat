package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class ImageToBlueprintViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `setImage resets previewMode to Source`() = runTest(testDispatcher) {
        val vm = ImageToBlueprintViewModel()
        vm.setBrightness(150)
        advanceTimeBy(220)
        advanceUntilIdle()
        vm.setImage(Uri.parse("file:///tmp/test.png"), 100, 100)
        assertThat(vm.state.value.previewMode).isEqualTo(PreviewMode.Source)
    }

    @Test fun `setBrightness marks isUpdating true during debounce window`() = runTest(testDispatcher) {
        val vm = ImageToBlueprintViewModel()
        // 不调用 setImage 避免触发 loadBitmap 路径
        vm.setBrightness(150)
        // 200ms 之内：正在 debounce，isUpdating 应该是 true
        advanceTimeBy(50)
        assertThat(vm.state.value.isUpdating).isTrue()
    }

    @Test fun `encodeForExport returns null when resultBitmap is null`() = runTest(testDispatcher) {
        val vm = ImageToBlueprintViewModel()
        // 没有选图，resultBitmap 为 null
        assertThat(vm.encodeForExport()).isNull()
    }
}