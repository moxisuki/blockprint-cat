package io.github.moxisuki.blockprint.cat.app.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Window width breakpoints for adaptive (tablet / wide-screen) layouts.
 * Values follow the Material window size class thresholds.
 */
@Immutable
enum class AppWindowWidthSize {
    Compact,
    Medium,
    Expanded,
    ;

    val isWide: Boolean
        get() = this != Compact

    companion object {
        fun fromWidthDp(widthDp: Int): AppWindowWidthSize = when {
            widthDp >= 840 -> Expanded
            widthDp >= 600 -> Medium
            else -> Compact
        }
    }
}

val LocalAppWindowWidthSize = compositionLocalOf { AppWindowWidthSize.Compact }

@Composable
fun rememberAppWindowWidthSize(): AppWindowWidthSize =
    AppWindowWidthSize.fromWidthDp(LocalConfiguration.current.screenWidthDp)

/**
 * Unified content column width on wide screens. Top app bar, grids, and
 * single-column pages all share this column so their edges stay aligned.
 */
private val MaxContentWidth = 1100.dp

/**
 * On wide screens, caps the content width to [MaxContentWidth] and centers it
 * horizontally. On compact screens this is a no-op.
 */
fun Modifier.appMaxContentWidth(): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val maxContentWidth = MaxContentWidth.roundToPx()
        if (constraints.maxWidth <= maxContentWidth) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        } else {
            val placeable = measurable.measure(
                constraints.copy(minWidth = 0, maxWidth = maxContentWidth),
            )
            layout(constraints.maxWidth, placeable.height) {
                placeable.placeRelative((constraints.maxWidth - placeable.width) / 2, 0)
            }
        }
    },
)
