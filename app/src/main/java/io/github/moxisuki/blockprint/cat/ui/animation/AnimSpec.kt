package io.github.moxisuki.blockprint.cat.ui.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

object AnimSpec {
    val slide = spring<IntOffset>(stiffness = 360f, dampingRatio = Spring.DampingRatioMediumBouncy)
    val slideExit = spring<IntOffset>(stiffness = 300f, dampingRatio = Spring.DampingRatioNoBouncy)

    val fade = spring<Float>(stiffness = 360f, dampingRatio = Spring.DampingRatioMediumBouncy)
    val fadeExit = spring<Float>(stiffness = 300f, dampingRatio = Spring.DampingRatioNoBouncy)

    val content = spring<Float>(stiffness = 300f, dampingRatio = Spring.DampingRatioMediumBouncy)

    val title = spring<Float>(stiffness = 280f, dampingRatio = Spring.DampingRatioNoBouncy)

    val micro = spring<Float>(stiffness = 350f, dampingRatio = Spring.DampingRatioNoBouncy)

    /** Pad 端侧边栏内容切换：滑入 + 淡入，tween 更稳定。 */
    val padSlide = tween<IntOffset>(300)
    val padSlideOut = tween<IntOffset>(250)
    val padFade = tween<Float>(300)
    val padFadeOut = tween<Float>(250)

    /** 连接页 4 区入场 fade。 */
    val enter: FiniteAnimationSpec<Float> = tween(220, easing = FastOutSlowInEasing)

    /** 连接页 4 区入场 slide 位移（与 `enter` 配对）。 */
    val enterOffset: FiniteAnimationSpec<IntOffset> = tween(220, easing = FastOutSlowInEasing)

    /** SessionCard 三态切换 crossfade。 */
    val crossfade: FiniteAnimationSpec<Float> = tween(200, easing = FastOutSlowInEasing)

    /** SessionCard 颜色平滑过渡（背景/状态点颜色 crossfade）。 */
    val crossfadeColor: FiniteAnimationSpec<androidx.compose.ui.graphics.Color> =
        tween(200, easing = FastOutSlowInEasing)

    /** LazyColumn item 进入 fade（与 animateItem 配对）。 */
    val listItemEnter: FiniteAnimationSpec<Float> = tween(240, easing = FastOutSlowInEasing)

    /** LazyColumn item 重排 slide（reorder 时）。 */
    val listItemPlacement: FiniteAnimationSpec<IntOffset> =
        spring(stiffness = 360f, dampingRatio = Spring.DampingRatioNoBouncy)

    /** 状态点呼吸（仅 SessionCard connecting 时用）：0.4f ↔ 1.0f，1.2s 往返。 */
    val dotPulse: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(1200, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
    )

    /**
     * HomeScreen 顶部 tab（Local/PC）切换的统一弹簧规格。
     *
     * 同一条 spec 同时用于：
     *  - 点胶囊：pagerState.animateScrollToPage(animationSpec = tabSwitch)
     *  - 滑动 settle：PagerDefaults.flingBehavior(snapAnimationSpec = tabSwitch)
     *
     * dampingRatio = MediumBouncy (0.5) → 末端弹一下（~20% 过冲）。
     * stiffness    = 360f → ~350ms 内 settle。
     *
     * 重要：胶囊高亮滑块和文字颜色读 currentPageOffsetFraction 实时插值，
     * 绝对不要再叠 animateFloatAsState / animateColorAsState —— pager
     * 内部已经用这条 spec 驱动 fraction，叠两层会变成"弹簧之上做弹簧"，
     * 肉眼能看出晃两下。
     */
    val tabSwitch = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 360f,
    )
}