package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.flow

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewDebounceTest {

    @Test
    fun `pushes within 200ms collapse to single emission`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val debounce = PreviewDebounce(scope, debounceMs = 200)

        val emissions = mutableListOf<Unit>()
        val job = launch(dispatcher) {
            debounce.flow.toList(emissions)
        }

        debounce.push()
        debounce.push()
        debounce.push()
        advanceTimeBy(220)
        advanceUntilIdle()

        assertThat(emissions).hasSize(1)
        job.cancel()
    }

    @Test
    fun `continuous pushes every 50ms do not emit during the run`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val debounce = PreviewDebounce(scope, debounceMs = 200)

        val emissions = mutableListOf<Unit>()
        val job = launch(dispatcher) {
            debounce.flow.toList(emissions)
        }

        repeat(5) { i ->
            debounce.push()
            advanceTimeBy(50) // 50ms < 200ms debounce
        }
        advanceTimeBy(220) // 最后一次静默 200ms 才发射
        advanceUntilIdle()

        assertThat(emissions).hasSize(1)
        job.cancel()
    }

    @Test
    fun `two distinct bursts produce two emissions`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val debounce = PreviewDebounce(scope, debounceMs = 200)

        val emissions = mutableListOf<Unit>()
        val job = launch(dispatcher) {
            debounce.flow.toList(emissions)
        }

        debounce.push()
        advanceTimeBy(220) // 第一次发射
        debounce.push()
        advanceTimeBy(220) // 第二次发射
        advanceUntilIdle()

        assertThat(emissions).hasSize(2)
        job.cancel()
    }

    @Test
    fun `rapid pushes emit exactly once with all accumulated triggers collapsed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val debounce = PreviewDebounce(scope, debounceMs = 200)

        val emissions = mutableListOf<Unit>()
        val job = launch(dispatcher) {
            debounce.flow.toList(emissions)
        }

        repeat(10) {
            debounce.push()
            advanceTimeBy(20) // 每次 20ms，远小于 200ms debounce
        }
        advanceTimeBy(220)
        advanceUntilIdle()

        assertThat(emissions).hasSize(1)
        job.cancel()
    }
}