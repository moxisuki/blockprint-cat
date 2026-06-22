package io.github.moxisuki.blockprint.cat.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val JOYSTICK_RADIUS_DP = 72
private const val THUMB_RADIUS_DP = 32

@Composable
internal fun WalkJoystick(
    onMove: (forward: Float, right: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseR = with(LocalDensity.current) { JOYSTICK_RADIUS_DP.dp.toPx() }
    val thumbR = with(LocalDensity.current) { THUMB_RADIUS_DP.dp.toPx() }
    var tx by remember { mutableFloatStateOf(0f) }; var ty by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val sf = MaterialTheme.colorScheme.surface; val onSf = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .size((JOYSTICK_RADIUS_DP * 2).dp)
            .clip(CircleShape).background(sf.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false; tx = 0f; ty = 0f; onMove(0f, 0f) },
                    onDragCancel = { dragging = false; tx = 0f; ty = 0f; onMove(0f, 0f) },
                    onDrag = { change, _ ->
                        // 不 consume：让 SceneView 也能收到触摸(多指兼容)
                        val rx = change.position.x - baseR; val ry = change.position.y - baseR
                        val d = sqrt(rx * rx + ry * ry); val maxD = baseR - thumbR
                        val s = if (d > 0.01f) d.coerceAtMost(maxD) / d else 0f
                        tx = rx * s; ty = ry * s
                        onMove(-(ty / maxD), tx / maxD)
                    },
                )
            },
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Box(Modifier.size(thumbR.dp).offset { IntOffset(tx.roundToInt(), ty.roundToInt()) }
            .clip(CircleShape).background(onSf.copy(alpha = if (dragging) 0.55f else 0.35f)))
    }
}
