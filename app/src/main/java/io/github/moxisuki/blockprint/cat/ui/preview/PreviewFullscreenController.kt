package io.github.moxisuki.blockprint.cat.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * System-bar controller used while the 3D blueprint preview is in fullscreen
 * mode. Hides the status & navigation bars via
 * [WindowCompat.getInsetsController] + legacy translucent flags so the
 * preview SurfaceView fills the whole screen including the bar areas.
 *
 * Behaviour is identical to the `DisposableEffect` block that lived at
 * lines 276–305 of MainActivity.kt before the MainActivity split; the only
 * shape change is that [isFullscreen] is now a parameter instead of
 * captured from the surrounding composable, and [LocalView] /
 * [LocalConfiguration] are read inside this composable rather than
 * closed over.
 *
 * Why the legacy translucent flags: without them the system paints the
 * default theme background where the bars were — see the original comment.
 *
 * `BEHAVIOR_DEFAULT` is intentional (no immersive swipe gestures) so touches
 * still reach the app, only the bars are hidden.
 */
@Composable
internal fun PreviewFullscreenController(isFullscreen: Boolean) {
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    DisposableEffect(isFullscreen, configuration) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            if (isFullscreen) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                controller.systemBarsBehavior =
                    androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val w = (view.context as? android.app.Activity)?.window
            if (w != null) {
                w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                WindowCompat.getInsetsController(w, view).show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}