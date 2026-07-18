package io.github.moxisuki.blockprint.cat.app.core.design

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import top.yukonga.miuix.kmp.anim.DecelerateEasing
import top.yukonga.miuix.kmp.anim.SinOutEasing

object AppMotion {
    object Duration {
        const val TabEnterMillis = 240
        const val TabExitMillis = 160
        const val TabVisibilityMillis = 220
        const val ContentEnterMillis = 200
        const val ContentExitMillis = 150
        const val ChildEnterMillis = 220
        const val ChildExitMillis = 160
        const val AppBarMillis = 150
    }

    object Scale {
        const val TabHidden = 0.985f
        const val TabVisible = 1f
    }

    fun topLevelTabTransition(): ContentTransform =
        fadeIn(
            initialAlpha = 0.86f,
            animationSpec = topLevelEnterSpec(Duration.TabEnterMillis),
        ) + scaleIn(
            initialScale = 0.985f,
            animationSpec = topLevelEnterSpec(Duration.TabEnterMillis),
        ) togetherWith fadeOut(
            targetAlpha = 0.0f,
            animationSpec = fadeExitSpec(Duration.TabExitMillis),
        ) + scaleOut(
            targetScale = 1.006f,
            animationSpec = fadeExitSpec(Duration.TabExitMillis),
        )

    fun contentFadeTransition(): ContentTransform =
        fadeIn(animationSpec = fadeEnterSpec(Duration.ContentEnterMillis)) togetherWith
            fadeOut(animationSpec = fadeExitSpec(Duration.ContentExitMillis))

    fun contentForwardTransition(): ContentTransform =
        fadeIn(
            initialAlpha = 0.92f,
            animationSpec = fadeEnterSpec(Duration.ChildEnterMillis),
        ) + slideInHorizontally(
            initialOffsetX = { it / 5 },
            animationSpec = spatialOffsetSpec(Duration.ChildEnterMillis),
        ) togetherWith fadeOut(
            targetAlpha = 0.0f,
            animationSpec = fadeExitSpec(Duration.ChildExitMillis),
        ) + slideOutHorizontally(
            targetOffsetX = { -it / 12 },
            animationSpec = spatialOffsetSpec(Duration.ChildExitMillis),
        )

    fun contentBackTransition(): ContentTransform =
        fadeIn(
            initialAlpha = 0.92f,
            animationSpec = fadeEnterSpec(Duration.ChildEnterMillis),
        ) + slideInHorizontally(
            initialOffsetX = { -it / 12 },
            animationSpec = spatialOffsetSpec(Duration.ChildEnterMillis),
        ) togetherWith fadeOut(
            targetAlpha = 0.0f,
            animationSpec = fadeExitSpec(Duration.ChildExitMillis),
        ) + slideOutHorizontally(
            targetOffsetX = { it / 5 },
            animationSpec = spatialOffsetSpec(Duration.ChildExitMillis),
        )

    fun appBarTransition(showingChildPage: Boolean): ContentTransform {
        val enterOffset: (Int) -> Int = if (showingChildPage) {
            { -it / 8 }
        } else {
            { it / 10 }
        }
        val exitOffset: (Int) -> Int = if (showingChildPage) {
            { -it / 10 }
        } else {
            { it / 8 }
        }

        return (
            fadeIn(animationSpec = fadeEnterSpec(Duration.AppBarMillis)) +
                slideInVertically(
                    initialOffsetY = enterOffset,
                    animationSpec = spatialOffsetSpec(Duration.AppBarMillis),
                )
            ) togetherWith (
                fadeOut(animationSpec = fadeExitSpec(Duration.AppBarMillis)) +
                    slideOutVertically(
                        targetOffsetY = exitOffset,
                        animationSpec = spatialOffsetSpec(Duration.AppBarMillis),
                    )
                )
    }

    fun topLevelEnterSpec(durationMillis: Int = Duration.TabEnterMillis): FiniteAnimationSpec<Float> =
        tween(durationMillis = durationMillis, easing = DecelerateEasing(1.5f))

    fun topLevelVisibilitySpec(): FiniteAnimationSpec<Float> =
        tween(durationMillis = Duration.TabVisibilityMillis, easing = DecelerateEasing(1.5f))

    fun fadeEnterSpec(durationMillis: Int = Duration.ContentEnterMillis): FiniteAnimationSpec<Float> =
        tween(durationMillis = durationMillis, easing = SinOutEasing)

    fun fadeExitSpec(durationMillis: Int = Duration.ContentExitMillis): FiniteAnimationSpec<Float> =
        tween(durationMillis = durationMillis, easing = SinOutEasing)

    private fun spatialOffsetSpec(durationMillis: Int): FiniteAnimationSpec<IntOffset> =
        tween(durationMillis = durationMillis, easing = DecelerateEasing(1.5f))

}
