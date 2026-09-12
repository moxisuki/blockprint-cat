package io.github.moxisuki.blockprint.cat.app.feature.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val JoystickBaseSize = 144.dp
private val JoystickThumbSize = 56.dp

@Composable
internal fun PreviewJoystick(
    onMove: (forward: Float, right: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val baseRadiusPx = with(density) { JoystickBaseSize.toPx() / 2f }
    val thumbRadiusPx = with(density) { JoystickThumbSize.toPx() / 2f }
    val maxOffsetPx = baseRadiusPx - thumbRadiusPx
    var thumbX by remember { mutableFloatStateOf(0f) }
    var thumbY by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(JoystickBaseSize)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        dragging = false
                        thumbX = 0f
                        thumbY = 0f
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        dragging = false
                        thumbX = 0f
                        thumbY = 0f
                        onMove(0f, 0f)
                    },
                    onDrag = { change, _ ->
                        val x = change.position.x - baseRadiusPx
                        val y = change.position.y - baseRadiusPx
                        val distance = sqrt(x * x + y * y)
                        val scale = if (distance > maxOffsetPx) maxOffsetPx / distance else 1f
                        thumbX = x * scale
                        thumbY = y * scale
                        onMove(
                            (-thumbY / maxOffsetPx).coerceIn(-1f, 1f),
                            (thumbX / maxOffsetPx).coerceIn(-1f, 1f),
                        )
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(JoystickThumbSize)
                .offset { IntOffset(thumbX.roundToInt(), thumbY.roundToInt()) }
                .clip(CircleShape)
                .background(
                    MiuixTheme.colorScheme.primary.copy(alpha = if (dragging) 0.78f else 0.58f),
                ),
        )
    }
}
