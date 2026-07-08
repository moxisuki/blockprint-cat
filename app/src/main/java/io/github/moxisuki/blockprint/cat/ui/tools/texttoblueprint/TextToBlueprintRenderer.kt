package io.github.moxisuki.blockprint.cat.ui.tools.texttoblueprint

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlueprintUiDefaults

/**
 * 把一段文字按指定字号 / 最大宽度渲染成黑白小图。
 *
 * 设计目标：
 *  - 自己控制渲染管线，不依赖用户输入的图片——只有"字符形状"作为前景，
 *    其余画布像素为白色背景。engine 拿到这张图后用 alpha 通道识别前景，
 *    黑色像素（前景）会变成具体方块，背景会变成空气。
 *  - 文字按 \n 自然换行；同时按 maxWidthPx 在单行内强制换行（中文友好）。
 *  - 输出 Bitmap 永远至少 16x16（避免 engine 在空图上抛错）。
 *
 * 这是我们自己实现的渲染逻辑（区别于 ITB 走用户图片路径），后续 TtbViewModel
 * 会把渲染结果作为 source 喂给 PixelArtConverter，targetWidth 由用户
 * 单独控制（这是"支持改分辨率"）。
 */
internal object TextToBlueprintRenderer {

    /**
     * @param text 多行文字
     * @param fontSizeSp 字号（sp，等价于 pixel 当 density=1.0）
     * @param maxWidthPx 单行超过这个像素数就强制换行
     * @return 白底黑字 Bitmap
     */
    fun render(
        text: String,
        fontSizeSp: Float,
        maxWidthPx: Int = BlueprintUiDefaults.TEXT_MAX_WIDTH_PX,
    ): Bitmap {
        // 抗锯齿关掉更接近像素艺术风，字符边缘不模糊；
        // 但是要识别成有效像素，字符轮廓要清晰，所以仍然需要 drawText 渲染。
        val paint = Paint().apply {
            this.textSize = fontSizeSp
            isAntiAlias = false
            color = BlueprintUiDefaults.TEXT_FOREGROUND_ARGB
            typeface = Typeface.MONOSPACE
        }

        val lineHeight = (paint.fontSpacing * 1.2f).toInt().coerceAtLeast(1)

        // 1) 先按 \n 切，再按 maxWidthPx 在每段内按字符硬换行
        val wrappedLines = text.split("\n").flatMap { para ->
            wrapLine(para, paint, maxWidthPx.toFloat())
        }.ifEmpty { listOf(" ") }

        // 2) 测量画布尺寸：取所有行宽度最大值
        val widths = wrappedLines.map { paint.measureText(it) }
        val maxLineWidth = widths.max().coerceAtLeast(8f)
        val canvasW = (maxLineWidth + 16f).toInt().coerceAtLeast(16)
        val canvasH = (lineHeight * wrappedLines.size + 16).coerceAtLeast(16)

        val bitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        // 用 ARGB_8888 + eraseColor：背景为纯白前景 alpha=0，
        // engine 看到 alpha=0 的像素会按"空气方块"处理。
        bitmap.eraseColor(BlueprintUiDefaults.TEXT_BACKGROUND_ARGB)
        val canvas = Canvas(bitmap)

        // drawText 的 y 坐标是基线位置，所以从 textSize - descent() 开始
        var y = paint.textSize - paint.descent()
        for (line in wrappedLines) {
            canvas.drawText(line, 8f, y, paint)
            y += lineHeight
        }
        return bitmap
    }

    /**
     * 把单段文字按字符宽度累加切到不超 [maxWidthPx] 为止。
     * 字符宽度粗略估计为 paint.measureText("字")，中文一字约等于两个英文字符。
     */
    private fun wrapLine(line: String, paint: Paint, maxWidthPx: Float): List<String> {
        if (line.isEmpty()) return listOf("")
        val out = mutableListOf<String>()
        var current = StringBuilder()
        for (ch in line) {
            val candidate = current.toString() + ch
            if (paint.measureText(candidate) > maxWidthPx && current.isNotEmpty()) {
                out += current.toString()
                current = StringBuilder().append(ch)
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) out += current.toString()
        return out
    }
}
