package io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 通用 N-参数防抖器。每次 push() 一次，200ms 内无新 push 才发射 Unit。
 *
 * 用法：每个 setter 在最后调用 debouncer.push()，发射后由 ViewModel 触发实际转换。
 *
 * 实现说明：用 MutableSharedFlow + replay=1。replay=1 是关键——
 * 当订阅者在 push() 之后才订阅（test 中 launch 协程还未实际运行时），
 * 订阅者会立即拿到最近一次 push，debounce 再等 200ms 才发射，
 * 行为符合"200ms 内无新 push 才发射"的语义。
 */
class PreviewDebounce(
    scope: CoroutineScope,
    private val debounceMs: Long = 200L,
) {
    private val trigger = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        // 保持 trigger 处于 hot 状态。replay=1 让任何订阅者拿到最近一次 push。
        trigger
            .debounce(debounceMs)
            .onEach { /* 不需要做任何事——订阅者从外部 flow 属性订阅 */ }
            .launchIn(scope)
    }

    val flow: Flow<Unit> = trigger.debounce(debounceMs)

    fun push() {
        trigger.tryEmit(Unit)
    }
}
