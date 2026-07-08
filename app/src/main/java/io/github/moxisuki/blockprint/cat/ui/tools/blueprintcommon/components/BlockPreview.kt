package io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.components

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
 * 预热：在 ImageToBlueprintScreen / TextToBlueprintScreen 用 LaunchedEffect +
 * Dispatchers.IO 把 140 个方块全部 decode 一遍（~100-200ms 在后台），之后展开任意
 * 组都是 0 延迟。
 */
private val pixelArtCache = ConcurrentHashMap<Int, ImageBitmap>()

/** 公开查询：返回已 cache 的位图，没 cache 过则返回 null（不会触发同步 decode）。 */
fun findPixelArt(@androidx.annotation.DrawableRes resId: Int): ImageBitmap? =
    pixelArtCache[resId]

/** 非 Composable 版本的 getOrPut，给 Canvas 渲染用（不能调 @Composable）。 */
fun getOrWarmPixelArt(context: Context, @androidx.annotation.DrawableRes resId: Int): ImageBitmap =
    pixelArtCache.getOrPut(resId) { decodePixelArt(context.applicationContext, resId) }

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
    withBorder: Boolean = true,
) {
    val shape = RoundedCornerShape(6.dp)
    val pixelBitmap = pixelArtCache.getOrPut(drawableResId) {
        decodePixelArt(LocalContext.current, drawableResId)
    }
    Box(
        modifier = modifier
            .size(32.dp)
            .then(
                if (withBorder) {
                    Modifier
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (dimmed) 0.3f else 0.5f))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (dimmed) 0.2f else 0.5f)),
                            shape,
                        )
                } else Modifier
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
