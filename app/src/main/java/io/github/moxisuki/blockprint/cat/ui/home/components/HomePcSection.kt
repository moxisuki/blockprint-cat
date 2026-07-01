package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionState

/**
 * Status row at the top of the PC tab. Shows a traffic-light dot (green =
 * Connected, amber = Connecting, grey = Disconnected / Error) plus the
 * session folder name and MC / loader version when available, otherwise the
 * target host:port (during Connecting) or the localised "unknown" label.
 */
@Composable
internal fun PcHeader(state: ConnectionState) {
    val isConnected = state is ConnectionState.Connected
    val isConnecting = state is ConnectionState.Connecting
    val session = (state as? ConnectionState.Connected)?.session
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when {
                        isConnected -> Color(0xFF4CAF50)
                        isConnecting -> Color(0xFFFFC107)
                        else -> Color(0xFF9E9E9E)
                    }
                )
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session?.folderName
                    ?: if (isConnecting) stringResource(R.string.bridge_connecting)
                    else stringResource(R.string.home_pc_unknown),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (session != null) {
                Text(
                    "MC ${session.mcVersion} · ${session.loader} ${session.loaderVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (state is ConnectionState.Connecting && state.host.isNotEmpty()) {
                Text(
                    "${state.host}:${state.port}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
