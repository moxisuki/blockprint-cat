package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec

@Composable
fun SessionCard(
    state: ConnectionState,
    scanState: ScanState,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onQrClick: () -> Unit,
    onDisconnect: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showError by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    val baseCardColor by animateColorAsState(
        targetValue = when (state) {
            is ConnectionState.Connected -> if (isDark) ConnectedCardBgDark else ConnectedCardBgLight
            is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
            is ConnectionState.Connecting -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = AnimSpec.crossfadeColor,
        label = "cardBg",
    )
    val baseContentColor = when (state) {
        is ConnectionState.Connected -> if (isDark) ConnectedContentDark else ConnectedContentLight
        is ConnectionState.Error -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier.animateContentSize(tween(300)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = baseCardColor,
            contentColor = baseContentColor,
        ),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Crossfade(
                targetState = state to scanState,
                animationSpec = tween(260),
                label = "sessionBody",
            ) { (connState, scanSt) ->
                when {
                    connState is ConnectionState.Connected -> ConnectedBody(
                        state = connState,
                        onDisconnect = onDisconnect,
                    )
                    connState is ConnectionState.Connecting -> ConnectingBody(state = connState)
                    connState is ConnectionState.Error -> ErrorBody(
                        state = connState,
                        onScan = onScan,
                        onQrClick = onQrClick,
                        onDismiss = {
                            showError = false
                            onDismissError()
                        },
                    )
                    scanSt is ScanState.Scanning -> ScanningBody(
                        devicesCount = scanSt.devices.size,
                        onStopScan = onStopScan,
                    )
                    else -> DisconnectedBody(
                        onScan = onScan,
                        onQrClick = onQrClick,
                    )
                }
            }
        }
    }

    val errorMsg = (state as? ConnectionState.Error)?.message
    AnimatedVisibility(visible = showError && errorMsg != null) {
        ErrorSheet(
            message = errorMsg ?: "",
            onDismiss = {
                showError = false
                onDismissError()
            },
        )
    }
}

private val ConnectedCardBgLight = Color(0xFFE8F5E9)
private val ConnectedCardBgDark = Color(0xFF143818)
private val ConnectedContentLight = Color(0xFF1B5E20)
private val ConnectedContentDark = Color(0xFFC8E6C9)

@Composable
private fun ConnectingBody(state: ConnectionState.Connecting) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.bridge_session_status_connecting), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ErrorBody(
    state: ConnectionState.Error,
    onScan: () -> Unit,
    onQrClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SessionStatusBadge(state = state, onErrorIconClick = onDismiss)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
        ) {
            Text(
                state.message,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.bridge_action_scan))
        }
        OutlinedButton(onClick = onQrClick, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.bridge_action_scan_qr))
        }
    }
}

@Composable
private fun ScanningBody(devicesCount: Int, onStopScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PulsingIcon(Icons.Default.Sensors, MaterialTheme.colorScheme.primary, 40.dp)
        Text(stringResource(R.string.bridge_scanning_hint_count, devicesCount), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onStopScan, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.bridge_action_stop_scan))
        }
    }
}

@Composable
private fun DisconnectedBody(onScan: () -> Unit, onQrClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Default.Computer,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
        Text(stringResource(R.string.bridge_connect_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.bridge_action_scan))
        }
        OutlinedButton(onClick = onQrClick, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.bridge_action_scan_qr))
        }
    }
}

@Composable
private fun PulsingIcon(icon: ImageVector, tint: Color, size: Dp) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val alpha by infinite.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val scale by infinite.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Icon(
        icon, contentDescription = null,
        tint = tint.copy(alpha = alpha),
        modifier = Modifier.size(size * scale),
    )
}
