package io.github.moxisuki.blockprint.cat.ui.util

import androidx.compose.ui.graphics.Color

/**
 * Compose [Color] → Android `argb` int conversion used to paint
 * system bars (status / navigation bar tint).
 *
 * Behaviour is identical to the original `private fun Color.toArgb()`
 * that lived at the bottom of MainActivity.kt before the MainActivity
 * split; only the visibility changed (`private` → `internal`).
 */
internal fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)