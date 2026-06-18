package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.moxisuki.blockprint.cat.data.bridge.PairedDeviceEntity
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec
import kotlinx.coroutines.delay

@Composable
private fun rememberEnterVisible(index: Int): State<Boolean> {
    val visible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(40L * index)
        visible.value = true
    }
    return visible
}

@Composable
fun ConnectionScreen(
    bridgeVm: BridgeViewModel,
    onQrClick: () -> Unit = {},
    viewModel: ConnectionViewModel = hiltViewModel(),
) {
    val connectionState by bridgeVm.connectionState.collectAsState()
    val scanState by bridgeVm.scanState.collectAsState()
    val paired by viewModel.paired.collectAsState()

    val s1 = rememberEnterVisible(0)
    val s2 = rememberEnterVisible(1)
    val s3 = rememberEnterVisible(2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnimatedVisibility(
            visible = s1.value,
            enter = fadeIn(animationSpec = AnimSpec.enter) +
                    slideInVertically(animationSpec = AnimSpec.enterOffset) { -it / 8 },
        ) {
            SessionCard(
                state = connectionState,
                scanState = scanState,
                onScan = { bridgeVm.startScan() },
                onStopScan = { bridgeVm.stopScan() },
                onQrClick = onQrClick,
                onDisconnect = { bridgeVm.disconnect() },
                onDismissError = { bridgeVm.clearError() },
            )
        }
        AnimatedVisibility(
            visible = s2.value,
            enter = fadeIn(animationSpec = AnimSpec.enter) +
                    slideInVertically(animationSpec = AnimSpec.enterOffset) { -it / 8 },
        ) {
            val devices = (scanState as? ScanState.Scanning)?.devices ?: emptyList()
            DevicesSection(
                discoveries = devices,
                paired = paired,
                onConnectDiscovery = { host, port, token ->
                    bridgeVm.connect(host, port, token)
                },
                onConnectPaired = { d: PairedDeviceEntity -> bridgeVm.connect(d.host, d.wsPort, d.token) },
                onDelete = { d: PairedDeviceEntity -> viewModel.deletePaired(d.host, d.wsPort, d.folderName) },
                onRename = { d: PairedDeviceEntity, newLabel: String -> viewModel.renamePaired(d.host, d.wsPort, d.folderName, newLabel) },
                onTokenUpdate = { d: PairedDeviceEntity, newToken: String -> viewModel.updateToken(d.host, d.wsPort, d.folderName, newToken) },
            )
        }
        AnimatedVisibility(
            visible = s3.value && connectionState !is ConnectionState.Connected,
            enter = fadeIn(animationSpec = AnimSpec.enter) +
                    slideInVertically(animationSpec = AnimSpec.enterOffset) { -it / 8 },
        ) {
            ManualConnectPanel(
                disabled = connectionState is ConnectionState.Connected,
                onConnect = { host, port, token -> bridgeVm.connect(host, port, token) },
            )
        }
    }
}
