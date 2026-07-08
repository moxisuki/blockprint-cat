package io.github.moxisuki.blockprint.cat.ui.tools.blockpaint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockCatalog
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockEntry
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.components.findPixelArt
import androidx.compose.ui.graphics.asAndroidBitmap

/**
 * 把 BlockPaint 网格渲染成 1:1 像素 bitmap，用于：
 *  1) 导出预览（与最终蓝图每个 cell 1 个方块对齐）
 *  2) 编码 ExportPayloadCodec（v2 schema 必需 bitmap）
 *
 * 设计要点：
 *  - bitmap 维度 = state.width × state.height，**每个 cell 1 像素**（用户要求 1×1 方块格子）
 *  - 用方块的"平均色"填一个像素——通过把 [io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.components.findPixelArt]
 *    返回的位图 downscale 到 1×1 拿到 ARGB 平均值
 *  - 用模块级 cache 避免重复 downscale（140+ 方块，只 decode 一次）
 *  - 空气格 (null) → ARGB 透明
 */
internal object BlockPaintRenderer {

    /**
     * 渲染 grid 到 1:1 bitmap。
     *
     * @return Pair(bitmap, materialsCount)。materialsCount 是 grid 里非 null 元素的 ID→数量
     *         统计，会作为导出 payload 的 materials 字段。
     */
    fun renderToBitmap(
        context: Context,
        grid: Array<Array<String?>>,
    ): Pair<Bitmap, Map<String, Int>> {
        val w = grid.size
        val h = if (w > 0) grid[0].size else 0
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // 背景空气：alpha=0 — 防止在 Engine 重建时被误识别成方块
        canvas.drawColor(Color.TRANSPARENT)

        val materials = linkedMapOf<String, Int>()
        val paint = Paint().apply { isAntiAlias = false }

        for (x in 0 until w) {
            for (y in 0 until h) {
                val blockId = grid[x][y] ?: continue
                val argb = colorFor(context, blockId)
                paint.color = argb
                // 1×1 方块格子：每个 cell 1 像素
                canvas.drawPoint(x.toFloat(), y.toFloat(), paint)
                materials[blockId] = (materials[blockId] ?: 0) + 1
            }
        }
        return bitmap to materials
    }

    // -------- 方块 → 平均色 cache --------

    private val colorCache = HashMap<String, Int>()

    /**
     * 取 [blockId] 对应方块图像的平均色（ARGB）。第一次访问会从 BlockCatalog 找
     * 资源并 downscale 到 1×1，之后从 cache 拿。
     *
     * 找不到时返回灰色 (0xFF808080) 作为兜底，避免 bitmap 出现黑点被识别成方块。
     */
    fun colorFor(context: Context, blockId: String): Int {
        colorCache[blockId]?.let { return it }
        val entry: BlockEntry = BlockCatalog.all.firstOrNull { it.id == blockId }
            ?: return FALLBACK_COLOR.also { colorCache[blockId] = it }
        // BlockPreview 已有 32x32 / 16x16 的预热图，findPixelArt 不会触发 IO
        val cached = findPixelArt(entry.drawableResId)
        val argb = if (cached != null) {
            // 用一张 1×1 的 downscale 算平均色；ARGB_8888 → 1×1 → 取中心像素即可代表均值
            val small = Bitmap.createScaledBitmap(cached.asAndroidBitmap(), 1, 1, true)
            val pixel = small.getPixel(0, 0)
            small.recycle()
            pixel
        } else {
            FALLBACK_COLOR
        }
        colorCache[blockId] = argb
        return argb
    }

    private const val FALLBACK_COLOR: Int = 0xFF808080.toInt()
}
