package io.github.moxisuki.blockprint.cat.app.feature.tools.components

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap

private val toolBlockTextureCache = ConcurrentHashMap<Int, ImageBitmap>()

internal fun getToolBlockTexture(context: Context, @DrawableRes resId: Int): ImageBitmap =
    toolBlockTextureCache.getOrPut(resId) {
        decodeToolBlockTexture(context.applicationContext, resId)
    }

internal fun prewarmToolBlockTextures(context: Context, resIds: Collection<Int>) {
    val appContext = context.applicationContext
    resIds.forEach { resId ->
        if (!toolBlockTextureCache.containsKey(resId)) {
            toolBlockTextureCache[resId] = decodeToolBlockTexture(appContext, resId)
        }
    }
}

private fun decodeToolBlockTexture(context: Context, @DrawableRes resId: Int): ImageBitmap {
    val drawable = ContextCompat.getDrawable(context, resId)
    if (drawable == null) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth.coerceAtLeast(1),
        drawable.intrinsicHeight.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888,
    )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    val scale = 4
    return Bitmap.createScaledBitmap(bitmap, bitmap.width * scale, bitmap.height * scale, false).asImageBitmap()
}
