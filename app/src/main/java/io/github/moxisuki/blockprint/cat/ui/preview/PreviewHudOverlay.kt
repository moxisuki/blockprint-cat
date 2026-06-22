package io.github.moxisuki.blockprint.cat.ui.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R

/**
 * HUD-style loading overlay shown while SceneView initializes.
 *
 * Visual style: sci-fi targeting reticle — four corner brackets framing
 * the screen, monospace status text, glowing primary-color accents on
 * a translucent black backdrop. Replaces the previous plain spinner.
 */
@Composable
internal fun HudStartupOverlay(visible: Boolean) {
    if (!visible) return
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceDim = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.82f))
    ) {
        // 四角定位框
        HudCornerBracket(color = primary, align = Alignment.TopStart)
        HudCornerBracket(color = primary, align = Alignment.TopEnd)
        HudCornerBracket(color = primary, align = Alignment.BottomStart)
        HudCornerBracket(color = primary, align = Alignment.BottomEnd)

        // 中央状态区
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.preview_hud_status),
                style = MaterialTheme.typography.titleMedium,
                color = primary,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(
                color = primary,
                modifier = Modifier.size(40.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.preview_hud_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceDim,
                fontFamily = FontFamily.Monospace,
            )
        }

        // 底部引擎状态
        Text(
            text = stringResource(R.string.preview_hud_engine),
            style = MaterialTheme.typography.labelSmall,
            color = primary.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp),
        )
    }
}

/**
 * 单角 L 型定位框,用 Canvas 画两条线组成 L。
 * 通过 [align] 决定这是哪一角,自动翻转坐标。
 *
 * 声明为 BoxScope 的扩展,这样可以在外层 Box 中通过 Modifier.align(align)
 * 把自己放到对应角落。
 */
@Composable
private fun BoxScope.HudCornerBracket(
    color: Color,
    align: Alignment,
    length: Dp = 28.dp,
    stroke: Dp = 2.dp,
    margin: Dp = 32.dp,
) {
    Canvas(
        modifier = Modifier
            .align(align)
            .padding(margin)
            .size(length, length)
    ) {
        val s = stroke.toPx()
        val l = length.toPx()
        when (align) {
            Alignment.TopStart -> {
                drawLine(color, Offset(0f, 0f), Offset(l, 0f), s)          // 顶边
                drawLine(color, Offset(0f, 0f), Offset(0f, l), s)          // 左边
            }
            Alignment.TopEnd -> {
                drawLine(color, Offset(l, 0f), Offset(0f, 0f), s)          // 顶边
                drawLine(color, Offset(l, 0f), Offset(l, l), s)            // 右边
            }
            Alignment.BottomStart -> {
                drawLine(color, Offset(0f, l), Offset(l, l), s)            // 底边
                drawLine(color, Offset(0f, l), Offset(0f, 0f), s)          // 左边
            }
            Alignment.BottomEnd -> {
                drawLine(color, Offset(l, l), Offset(0f, l), s)            // 底边
                drawLine(color, Offset(l, l), Offset(l, 0f), s)            // 右边
            }
        }
    }
}