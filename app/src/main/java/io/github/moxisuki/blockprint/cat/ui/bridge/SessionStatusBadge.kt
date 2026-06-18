package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec

@Composable
fun SessionStatusBadge(
    state: ConnectionState,
    onErrorIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotColor by animateColorAsState(
        targetValue = when (state) {
            is ConnectionState.Connected -> Color(0xFF4CAF50)
            is ConnectionState.Connecting -> Color(0xFFFFC107)
            is ConnectionState.Error -> MaterialTheme.colorScheme.error
            is ConnectionState.Disconnected -> Color(0xFF9E9E9E)
        },
        animationSpec = AnimSpec.crossfadeColor,
        label = "dotColor",
    )

    val infinite = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = AnimSpec.dotPulse,
        label = "pulseAlpha",
    )
    val isConnecting = state is ConnectionState.Connecting
    val dotAlpha = if (isConnecting) pulseAlpha else 1f

    val errorMessage = (state as? ConnectionState.Error)?.message

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .drawBehind {
                    if (isConnecting) {
                        drawCircle(
                            color = dotColor.copy(alpha = pulseAlpha),
                            radius = 14.dp.toPx(),
                        )
                    }
                    drawCircle(
                        color = dotColor.copy(alpha = dotAlpha),
                        radius = size.minDimension / 2,
                    )
                },
        )
        Spacer(Modifier.width(8.dp))
        AnimatedContent(
            targetState = when (state) {
                is ConnectionState.Connected -> stringResource(R.string.bridge_session_status_connected)
                is ConnectionState.Connecting -> stringResource(R.string.bridge_session_status_connecting)
                is ConnectionState.Error -> stringResource(R.string.bridge_status_error)
                is ConnectionState.Disconnected -> stringResource(R.string.bridge_session_status_disconnected)
            },
            transitionSpec = {
                fadeIn(animationSpec = AnimSpec.crossfade) togetherWith
                    fadeOut(animationSpec = AnimSpec.crossfade)
            },
            label = "statusText",
        ) { text ->
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
        if (errorMessage != null) {
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onErrorIconClick) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = stringResource(R.string.bridge_status_error),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
