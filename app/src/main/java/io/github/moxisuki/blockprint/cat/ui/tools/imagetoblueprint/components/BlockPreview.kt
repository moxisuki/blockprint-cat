package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun BlockPreview(
    @DrawableRes drawableResId: Int,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    val pixelBitmap = rememberPixelArt(drawableResId)
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dimmed) 0.3f else 0.5f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (dimmed) 0.2f else 0.5f)),
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = pixelBitmap,
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .alpha(if (dimmed) 0.35f else 1f),
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
        )
    }
}

@Composable
private fun rememberPixelArt(@DrawableRes resId: Int): ImageBitmap {
    val context = LocalContext.current
    return remember(resId) {
        val original: Bitmap = androidx.core.content.ContextCompat.getDrawable(context, resId)
            ?.let { drawable ->
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(1),
                    drawable.intrinsicHeight.coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888,
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }
            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val scale = 8
        val scaled = Bitmap.createScaledBitmap(original, original.width * scale, original.height * scale, false)
        scaled.asImageBitmap()
    }
}
