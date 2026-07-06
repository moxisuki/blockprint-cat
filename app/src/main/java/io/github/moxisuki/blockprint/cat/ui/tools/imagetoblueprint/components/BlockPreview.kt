package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components

import android.content.Context
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
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap

/**
 * 模块级位图 cache。`remember(resId)` 是 per-Composable 的——同一 resId 在
 * 不同 BlockPreview 重组时会重复 decode+scale。这是打开"方块选择"明显卡顿
 * 的根因。改成 ConcurrentHashMap 后任何 Composable 第一次 decode 后所有后续
 * 访问都 hit cache。
 *
 * 预热：在 ImageToBlueprintScreen 用 LaunchedEffect + Dispatchers.IO 把
 * 140 个方块全部 decode 一遍（~100-200ms 在后台），之后展开任意组都是 0 延迟。
 */
private val pixelArtCache = ConcurrentHashMap<Int, ImageBitmap>()

/** 非 Composable 入口，给预热协程用。 */
fun prewarmPixelArt(context: Context, resIds: Collection<Int>) {
    val appContext = context.applicationContext
    for (resId in resIds) {
        if (!pixelArtCache.containsKey(resId)) {
            pixelArtCache[resId] = decodePixelArt(appContext, resId)
        }
    }
}

private fun decodePixelArt(context: Context, @DrawableRes resId: Int): ImageBitmap {
    val original: Bitmap = ContextCompat.getDrawable(context, resId)
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
    // scale 4 = 32x32 for 8x8 source, 64x64 for 16x16 source
    val scale = 4
    return Bitmap.createScaledBitmap(original, original.width * scale, original.height * scale, false)
        .asImageBitmap()
}

@Composable
internal fun BlockPreview(
    @DrawableRes drawableResId: Int,
    dimmed: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    // cache-first：已 decode 直接拿；没有则同步 decode（模块级 ConcurrentHashMap 保证单次）
    val pixelBitmap = pixelArtCache.getOrPut(drawableResId) {
        decodePixelArt(LocalContext.current, drawableResId)
    }
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
