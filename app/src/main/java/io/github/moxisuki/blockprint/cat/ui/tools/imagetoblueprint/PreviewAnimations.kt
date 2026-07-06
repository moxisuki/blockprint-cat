package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

import androidx.compose.animation.core.CubicBezierEasing

/**
 * 统一的动画规格常量。所有动效时长/缓动都从本文件取，禁止在组件里写 magic number。
 * 数值见 docs/superpowers/specs/2026-07-06-image-to-blueprint-ui-redesign-design.md §5.1
 */
object PreviewAnimations {
    const val TINY = 120        // 图标缩放
    const val SHORT = 220       // 角标淡入、按钮反馈、刷一下高亮
    const val MEDIUM = 320      // 卡片入场、Hero 切换
    const val LONG = 480        // 列表交错完成

    val EasingStandard = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)
    val EasingEnter = CubicBezierEasing(0.0f, 0f, 0.2f, 1f)
    val EasingExit = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val EasingEmphasized = CubicBezierEasing(0.2f, 0f, 0.0f, 1f)
}
