package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.DiscoveryPayload
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity

@Composable
fun DevicesSection(
    discoveries: List<DiscoveryPayload>,
    paired: List<PairedDeviceEntity>,
    onConnectDiscovery: (host: String, port: Int, token: String) -> Unit,
    onConnectPaired: (PairedDeviceEntity) -> Unit,
    onDelete: (PairedDeviceEntity) -> Unit,
    onRename: (PairedDeviceEntity, String) -> Unit,
    onTokenUpdate: (PairedDeviceEntity, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val validDiscoveries = discoveries.filter { it.host.isNotBlank() && it.wsPort > 0 }
    val validPaired = paired.filter { it.host.isNotBlank() && it.wsPort > 0 }

    val pairedKeys = validPaired.map { it.host to it.wsPort }.toSet()
    val newDiscoveries = validDiscoveries.filter { (it.host to it.wsPort) !in pairedKeys }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PairedDevicesCard(
            paired = validPaired,
            onConnect = onConnectPaired,
            onDelete = onDelete,
            onRename = onRename,
            onTokenUpdate = onTokenUpdate,
        )
        if (newDiscoveries.isNotEmpty()) {
            DiscoveredDevicesCard(
                discoveries = newDiscoveries,
                onConnect = onConnectDiscovery,
            )
        }
    }
}

@Composable
private fun PairedDevicesCard(
    paired: List<PairedDeviceEntity>,
    onConnect: (PairedDeviceEntity) -> Unit,
    onDelete: (PairedDeviceEntity) -> Unit,
    onRename: (PairedDeviceEntity, String) -> Unit,
    onTokenUpdate: (PairedDeviceEntity, String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                icon = Icons.Default.Computer,
                tint = MaterialTheme.colorScheme.primary,
                title = stringResource(R.string.bridge_section_paired),
                count = paired.size,
            )
            Spacer(Modifier.height(8.dp))
            if (paired.isEmpty()) {
                EmptyHint(stringResource(R.string.bridge_section_paired_empty))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(paired, key = { "p:${it.host}:${it.wsPort}:${it.folderName}" }) { p ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 4 },
                        ) {
                            PairedRow(
                                dev = p,
                                onConnect = { onConnect(p) },
                                onDelete = { onDelete(p) },
                                onRename = { newLabel -> onRename(p, newLabel) },
                                onTokenUpdate = { newToken -> onTokenUpdate(p, newToken) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveredDevicesCard(
    discoveries: List<DiscoveryPayload>,
    onConnect: (host: String, port: Int, token: String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                icon = Icons.Default.Sensors,
                tint = MaterialTheme.colorScheme.tertiary,
                title = stringResource(R.string.bridge_section_discovered),
                count = discoveries.size,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.bridge_section_discovered_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(discoveries, key = { "d:${it.host}:${it.wsPort}" }) { dev ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 4 },
                    ) {
                        DiscoveredRow(
                            dev = dev,
                            onConnect = onConnect,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    title: String,
    count: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(32.dp),
        )
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
