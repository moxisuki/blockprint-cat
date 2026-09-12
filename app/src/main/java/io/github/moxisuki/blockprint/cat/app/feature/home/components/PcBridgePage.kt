package io.github.moxisuki.blockprint.cat.app.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcBridgeConnection
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcBridgeDefaultPort
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcBridgeSession
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcBridgeState
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcDiscoveredDevice
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcRemoteBlueprint
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcTaskPhase
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcTaskStatus
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcTransferTask
import io.github.moxisuki.blockprint.cat.app.feature.home.formatCount
import io.github.moxisuki.blockprint.cat.app.feature.home.formatFileSize
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

@Composable
internal fun PcBridgePage(
    bridgeState: PcBridgeState,
    host: String,
    port: String,
    token: String,
    searchQuery: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onDeviceClick: (PcDiscoveredDevice) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onDownloadClick: (PcRemoteBlueprint) -> Unit,
    onCancelTaskClick: (String) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remoteBlueprints by remember(bridgeState.blueprints, searchQuery) {
        derivedStateOf { bridgeState.blueprints.filterBySearch(searchQuery) }
    }
    val connected = bridgeState.connection is PcBridgeConnection.Connected

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .appScrollEndHaptic()
            .appMaxContentWidth(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 0.dp,
            end = 16.dp,
            bottom = 104.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "pc-status") {
            PcStatusCard(
                bridgeState = bridgeState,
                onDisconnectClick = onDisconnectClick,
                onRefreshClick = onRefreshClick,
            )
        }

        item(key = "pc-tasks") {
            AnimatedVisibility(
                visible = bridgeState.tasks.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(140)),
                exit = fadeOut(animationSpec = tween(140)),
            ) {
                PcTaskStack(
                    tasks = bridgeState.tasks,
                    onCancelTaskClick = onCancelTaskClick,
                )
            }
        }

        item(key = "pc-error") {
            AnimatedVisibility(
                visible = bridgeState.lastError != null,
                enter = fadeIn(animationSpec = tween(140)),
                exit = fadeOut(animationSpec = tween(140)),
            ) {
                val error = bridgeState.lastError
                if (error != null) {
                    PcErrorCard(
                        code = error.code,
                        message = error.message,
                        onDismiss = onDismissError,
                    )
                }
            }
        }

        if (!connected) {
            item(key = "pc-manual") {
                PcManualConnectCard(
                    host = host,
                    port = port,
                    token = token,
                    connecting = bridgeState.connection is PcBridgeConnection.Connecting,
                    onHostChange = onHostChange,
                    onPortChange = onPortChange,
                    onTokenChange = onTokenChange,
                    onConnectClick = onConnectClick,
                )
            }
            item(key = "pc-discovery-header") {
                SectionTitle(
                    title = stringResource(R.string.pc_bridge_discovery_title),
                    summary = discoverySummary(bridgeState),
                )
            }
            if (bridgeState.discoveredDevices.isEmpty()) {
                item(key = "pc-discovery-empty") {
                    PcEmptyPanel(
                        title = stringResource(R.string.bridge_no_devices),
                        summary = stringResource(R.string.bridge_scanning_hint_zero),
                    )
                }
            } else {
                items(
                    items = bridgeState.discoveredDevices,
                    key = { it.id },
                ) { device ->
                    PcDeviceCard(
                        device = device,
                        onClick = { onDeviceClick(device) },
                    )
                }
            }
        } else {
            item(key = "pc-remote-header") {
                SectionTitle(
                    title = stringResource(R.string.pc_bridge_remote_title),
                    summary = remoteSummary(bridgeState.session, remoteBlueprints.size),
                )
            }
            if (remoteBlueprints.isEmpty()) {
                item(key = "pc-remote-empty") {
                    PcEmptyPanel(
                        title = if (searchQuery.isBlank()) {
                            stringResource(R.string.home_pc_empty_title)
                        } else {
                            stringResource(R.string.home_search_empty_title)
                        },
                        summary = if (searchQuery.isBlank()) {
                            stringResource(R.string.home_pc_empty_hint)
                        } else {
                            stringResource(R.string.home_search_empty_hint)
                        },
                    )
                }
            } else {
                items(
                    items = remoteBlueprints,
                    key = { it.id },
                ) { blueprint ->
                    PcRemoteBlueprintCard(
                        blueprint = blueprint,
                        task = bridgeState.tasks.firstOrNull { it.blueprintId == blueprint.id },
                        onDownloadClick = { onDownloadClick(blueprint) },
                        onCancelTaskClick = onCancelTaskClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun PcStatusCard(
    bridgeState: PcBridgeState,
    onDisconnectClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    val connection = bridgeState.connection
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PcIconBadge(
                icon = when (connection) {
                    is PcBridgeConnection.Connected -> Icons.Filled.CheckCircle
                    is PcBridgeConnection.Connecting -> Icons.Filled.Link
                    is PcBridgeConnection.Failed -> Icons.Filled.ErrorOutline
                    PcBridgeConnection.Disconnected -> Icons.Filled.Computer
                },
                selected = connection is PcBridgeConnection.Connected,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connection.statusLabel(),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = connection.summary(bridgeState.session),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (connection is PcBridgeConnection.Connected) {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.action_refresh),
                        tint = MiuixTheme.colorScheme.onSurfaceContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
                TextButton(
                    text = stringResource(R.string.bridge_disconnect),
                    onClick = onDisconnectClick,
                )
            }
        }
    }
}

@Composable
private fun PcManualConnectCard(
    host: String,
    port: String,
    token: String,
    connecting: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onConnectClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(14.dp),
    ) {
        Text(
            text = stringResource(R.string.pc_bridge_manual_title),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = host,
                onValueChange = onHostChange,
                modifier = Modifier.weight(1.35f),
                label = stringResource(R.string.bridge_field_host),
                singleLine = true,
                textStyle = MiuixTheme.textStyles.body2,
            )
            TextField(
                value = port,
                onValueChange = onPortChange,
                modifier = Modifier.weight(0.75f),
                label = stringResource(R.string.bridge_field_port),
                singleLine = true,
                textStyle = MiuixTheme.textStyles.body2,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(R.string.bridge_field_token),
            singleLine = true,
            textStyle = MiuixTheme.textStyles.body2,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onConnectClick,
            enabled = !connecting && host.isNotBlank() && token.isNotBlank(),
        ) {
            if (connecting) {
                RotatingIcon(Icons.Filled.Refresh)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.bridge_connecting), maxLines = 1)
            } else {
                Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.bridge_action_connect), maxLines = 1)
            }
        }
    }
}

@Composable
private fun PcDeviceCard(
    device: PcDiscoveredDevice,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PcIconBadge(icon = Icons.Filled.Wifi, selected = false)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${device.host}:${device.port}",
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = device.mcVersion.ifBlank { device.loader.ifBlank { device.deviceName } },
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SmallChip(text = device.deviceName.ifBlank { device.version })
        }
    }
}

@Composable
private fun PcRemoteBlueprintCard(
    blueprint: PcRemoteBlueprint,
    task: PcTransferTask?,
    onDownloadClick: () -> Unit,
    onCancelTaskClick: (String) -> Unit,
) {
    val active = task?.active == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(14.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PcIconBadge(icon = Icons.Filled.Computer, selected = false)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = blueprint.displayName,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = blueprint.fileName,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SmallChip(text = blueprint.format.ifBlank { "Unknown" })
                    SmallChip(text = blueprint.source)
                    SmallChip(text = "${blueprint.width}x${blueprint.height}x${blueprint.depth}")
                    SmallChip(text = formatFileSize(blueprint.sizeBytes))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.pc_bridge_blueprint_meta,
                        formatCount(blueprint.blocks),
                        blueprint.regions,
                    ),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = {
                    if (task?.active == true) {
                        onCancelTaskClick(task.taskId)
                    } else if (!active) {
                        onDownloadClick()
                    }
                },
            ) {
                if (active) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.pc_bridge_cancel_task),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = stringResource(R.string.pc_bridge_download),
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
        if (task != null) {
            Spacer(modifier = Modifier.height(10.dp))
            TaskInlineProgress(task = task)
        }
    }
}

@Composable
private fun PcTaskStack(
    tasks: List<PcTransferTask>,
    onCancelTaskClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(
            title = stringResource(R.string.pc_bridge_tasks_title),
            summary = stringResource(R.string.pc_bridge_tasks_summary, tasks.count { it.active }),
        )
        tasks.forEach { task ->
            PcTaskCard(
                task = task,
                onCancelTaskClick = onCancelTaskClick,
            )
        }
    }
}

@Composable
private fun PcTaskCard(
    task: PcTransferTask,
    onCancelTaskClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(14.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PcIconBadge(
                icon = if (task.status == PcTaskStatus.Failed) Icons.Filled.ErrorOutline else Icons.Filled.Download,
                selected = task.status == PcTaskStatus.Done,
                small = true,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.phaseLabel(),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = task.fileName.ifBlank { task.blueprintId.orEmpty() },
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = task.progress,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (task.active) {
                IconButton(onClick = { onCancelTaskClick(task.taskId) }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.pc_bridge_cancel_task),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskInlineProgress(task: PcTransferTask) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = task.phaseLabel(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = task.progressPercentLabel(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
        LinearProgressIndicator(
            progress = task.progress,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PcErrorCard(
    code: String,
    message: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(14.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = code,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_dismiss),
                    tint = MiuixTheme.colorScheme.onSurfaceContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    summary: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 2.dp, end = 2.dp, bottom = 2.dp),
    ) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = summary,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PcEmptyPanel(
    title: String,
    summary: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(18.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = summary,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
private fun PcIconBadge(
    icon: ImageVector,
    selected: Boolean,
    small: Boolean = false,
) {
    val size = if (small) 32.dp else 40.dp
    val iconSize = if (small) 17.dp else 21.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (selected) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MiuixTheme.colorScheme.surface
                },
            )
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.10f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun SmallChip(
    text: String,
    color: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
) {
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MiuixTheme.colorScheme.surface)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RotatingIcon(icon: ImageVector) {
    val transition = rememberInfiniteTransition(label = "pcBridgeSpin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
        ),
        label = "pcBridgeSpinValue",
    )
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = rotation },
    )
}

@Composable
private fun PcBridgeConnection.statusLabel(): String = when (this) {
    is PcBridgeConnection.Connected -> stringResource(R.string.bridge_connected)
    is PcBridgeConnection.Connecting -> stringResource(R.string.bridge_connecting)
    is PcBridgeConnection.Failed -> stringResource(R.string.bridge_status_error)
    PcBridgeConnection.Disconnected -> stringResource(R.string.bridge_not_connected)
}

@Composable
private fun PcBridgeConnection.summary(session: PcBridgeSession?): String = when (this) {
    is PcBridgeConnection.Connected -> {
        val sessionName = session?.compactName.orEmpty()
        if (sessionName.isBlank()) {
            "$host:$port"
        } else {
            "$host:$port · $sessionName"
        }
    }
    is PcBridgeConnection.Connecting -> "$host:$port"
    is PcBridgeConnection.Failed -> message
    PcBridgeConnection.Disconnected -> stringResource(R.string.bridge_disconnected_hint)
}

@Composable
private fun discoverySummary(state: PcBridgeState): String =
    if (state.discoveryActive) {
        stringResource(R.string.bridge_scanning_hint_count, state.discoveredDevices.size)
    } else {
        stringResource(R.string.bridge_section_discovered_empty)
    }

@Composable
private fun remoteSummary(session: PcBridgeSession?, count: Int): String =
    if (session != null) {
        stringResource(R.string.pc_bridge_remote_summary, count, session.folderName.ifBlank { session.mcVersion })
    } else {
        stringResource(R.string.pc_bridge_remote_summary_unknown, count)
    }

@Composable
private fun PcTransferTask.phaseLabel(): String = when (phase) {
    PcTaskPhase.Queued -> stringResource(R.string.pc_task_queued)
    PcTaskPhase.Opening -> stringResource(R.string.pc_task_opening)
    PcTaskPhase.Transferring -> stringResource(R.string.pc_task_transferring)
    PcTaskPhase.Verifying -> stringResource(R.string.pc_task_verifying)
    PcTaskPhase.Importing -> stringResource(R.string.pc_task_importing)
    PcTaskPhase.Done -> stringResource(R.string.pc_task_done)
    PcTaskPhase.Failed -> errorMessage?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.pc_task_failed)
    PcTaskPhase.Cancelled -> stringResource(R.string.pc_task_cancelled)
}

private fun PcTransferTask.progressPercentLabel(): String =
    "${(progress * 100f).toInt().coerceIn(0, 100)}%"

private fun List<PcRemoteBlueprint>.filterBySearch(query: String): List<PcRemoteBlueprint> {
    val normalized = query.trim().lowercase(Locale.getDefault())
    if (normalized.isBlank()) return this
    return filter { blueprint ->
        listOf(
            blueprint.displayName,
            blueprint.fileName,
            blueprint.author,
            blueprint.source,
            blueprint.format,
        ).any { it.lowercase(Locale.getDefault()).contains(normalized) }
    }
}

@Preview(showBackground = true)
@Composable
private fun PcBridgePagePreview() {
    PreviewAppTheme {
        PcBridgePage(
            bridgeState = PcBridgeState(
                connection = PcBridgeConnection.Connected("192.168.1.42", PcBridgeDefaultPort),
                session = PcBridgeSession(
                    instanceId = "preview-link",
                    deviceName = "BlockPrint Link",
                    mcVersion = "1.21.1",
                    loader = "neoforge",
                    loaderVersion = "21.1.233",
                    folderName = "1.21.1-NeoForge",
                ),
                blueprints = previewRemoteBlueprints(),
                tasks = listOf(
                    PcTransferTask(
                        taskId = "task-preview",
                        transferId = "transfer-preview",
                        kind = "blueprint.download",
                        blueprintId = "schematics/spawn_hub.litematic",
                        fileName = "spawn_hub.litematic",
                        phase = PcTaskPhase.Transferring,
                        status = PcTaskStatus.Running,
                        bytesDone = 5_200_000L,
                        bytesTotal = 12_400_000L,
                        progress = 0.42f,
                    ),
                ),
            ),
            host = "192.168.1.42",
            port = PcBridgeDefaultPort.toString(),
            token = "okk2",
            searchQuery = "",
            onHostChange = {},
            onPortChange = {},
            onTokenChange = {},
            onDeviceClick = {},
            onConnectClick = {},
            onDisconnectClick = {},
            onRefreshClick = {},
            onDownloadClick = {},
            onCancelTaskClick = {},
            onDismissError = {},
        )
    }
}

private fun previewRemoteBlueprints(): List<PcRemoteBlueprint> = listOf(
    PcRemoteBlueprint(
        id = "schematics/spawn_hub.litematic",
        fileName = "spawn_hub.litematic",
        format = "Litematica",
        name = "Spawn Hub",
        width = 64,
        height = 42,
        depth = 58,
        blocks = 82044,
        author = "Steve",
        description = "",
        minecraftDataVersion = 3953,
        version = 6,
        regions = 4,
        source = "schematics",
        sizeBytes = 12_400_000L,
        lastModifiedAt = 1_721_000_000_000L,
    ),
)
