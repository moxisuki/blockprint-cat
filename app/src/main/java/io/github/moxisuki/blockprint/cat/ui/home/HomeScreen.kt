package io.github.moxisuki.blockprint.cat.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.clickable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta

import io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeViewModel
import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionState
import io.github.moxisuki.blockprint.cat.ui.bridge.PcActionSheet
import io.github.moxisuki.blockprint.cat.ui.bridge.PcBlueprintCard
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.ui.management.BlueprintViewModel
import io.github.moxisuki.blockprint.cat.ui.management.ManagementEvent
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber

private const val PAGE_SIZE = 15

@Composable
fun HomeScreen(
    navController: NavController,
    bridgeVm: BridgeViewModel = hiltViewModel(),
    viewModel: HomeViewModel = hiltViewModel(),
    managementViewModel: BlueprintViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onRequestSafFolder: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onBlueprintSelected: ((BlueprintMeta) -> Unit)? = null,
    showFab: Boolean = true,
) {
    val managementState by managementViewModel.uiState.collectAsState()
    val scanning by viewModel.scanning.collectAsState()
    val connectionState by bridgeVm.connectionState.collectAsState()
    val isBridgeConnected = connectionState is ConnectionState.Connected
    val isBridgeConnecting = connectionState is ConnectionState.Connecting
    val pcSession = (connectionState as? ConnectionState.Connected)?.session
    val pcEntries = (connectionState as? ConnectionState.Connected)?.entries ?: emptyList()
    val pcHost = (connectionState as? ConnectionState.Connected)?.host ?: ""
    val pcPort = (connectionState as? ConnectionState.Connected)?.port ?: 0
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allBlueprints by viewModel.blueprints.collectAsState(initial = viewModel.blueprints.value)
    var visibleCount by remember { mutableIntStateOf(PAGE_SIZE) }

    val visibleBlueprints by remember {
        derivedStateOf { allBlueprints.take(visibleCount) }
    }
    val hasMore by remember {
        derivedStateOf { visibleBlueprints.size < allBlueprints.size }
    }

    var deleteTarget by remember { mutableStateOf<BlueprintMeta?>(null) }
    var renameTarget by remember { mutableStateOf<BlueprintMeta?>(null) }
    var renameText by remember { mutableStateOf("") }
    var sheetTarget by remember { mutableStateOf<RemoteBlueprint?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { managementViewModel.loadWithContext(context, it); visibleCount = PAGE_SIZE }
    }

    LaunchedEffect(managementState.error) {
        managementState.error?.let {
            snackbarHostState.showSnackbar(it)
            managementViewModel.clearError()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab switcher — outer Row centers the pill + refresh button.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                listOf(R.string.home_tab_local to 0, R.string.home_tab_pc to 1).forEachIndexed { _, (labelRes, tab) ->
                    val label = stringResource(labelRes)
                    Box(
                        modifier = Modifier
                            .then(
                                if (selectedTab == tab)
                                    Modifier.padding(3.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary)
                                else Modifier.padding(3.dp)
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedTab == tab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onRefresh(); visibleCount = PAGE_SIZE }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(AnimSpec.slide) { it * dir } + fadeIn(AnimSpec.content)) togetherWith (slideOutHorizontally(AnimSpec.slideExit) { -it * dir } + fadeOut(AnimSpec.fadeExit)) using SizeTransform(clip = false)
            },
            label = "tab",
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { tab ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> {
                        LocalBlueprintList(
                            allBlueprints = allBlueprints,
                            scanning = scanning,
                            visibleCount = visibleCount,
                            onVisibleCountChange = { visibleCount = it },
                            safFolderName = viewModel.safFolderName(),
                            onRequestSafFolder = onRequestSafFolder,
                            navController = navController,
                            onBlueprintSelected = onBlueprintSelected,
                            onDeleteTarget = { deleteTarget = it },
                            onRenameTarget = { bp -> renameTarget = bp; renameText = bp.fileName },
                            onUpload = { bp ->
                                scope.launch {
                                    val bytes = managementViewModel.readBytes(bp.uuid)
                                    if (bytes != null) {
                                        bridgeVm.requestUpload(bp.fileName, bytes, overwrite = true)
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.action_sync_started, bp.fileName)) }
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.action_sync_failed, bp.fileName)) }
                                    }
                                }
                            },
                            bridgeConnected = isBridgeConnected,
                            snackbarHostState = snackbarHostState,
                        )
                        if (showFab) {
                        FloatingActionButton(
                            onClick = { filePicker.launch(arrayOf("application/octet-stream", "*/*")) },
                            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        ) { Icon(Icons.Default.Add, contentDescription = "导入蓝图") }
                        }
                    }
                    1 -> {
                        if (pcSession == null && !isBridgeConnecting) {
                            Column(modifier = Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Cloud, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                                Spacer(Modifier.height(16.dp))
                                Text(stringResource(R.string.home_pc_not_connected), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Spacer(Modifier.height(4.dp))
                                Text(stringResource(R.string.home_connect_pc_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                PcHeader(state = connectionState, onRefresh = { bridgeVm.requestList() })
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (pcEntries.isEmpty() && isBridgeConnected) {
                                        EmptyPcState(modifier = Modifier.align(Alignment.Center))
                                    } else if (pcEntries.isEmpty() && !isBridgeConnected) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.align(Alignment.Center).size(20.dp))
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            itemsIndexed(pcEntries, key = { index, it -> "${it.fileName}_$index" }) { _, bp ->
                                                PcBlueprintCard(blueprint = bp, onClick = { sheetTarget = bp })
                                            }
                                            item { Spacer(Modifier.height(16.dp)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { bp ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text(stringResource(R.string.dialog_delete_title)) }, text = { Text(stringResource(R.string.dialog_delete_msg, bp.displayName)) },
            confirmButton = { TextButton(onClick = { managementViewModel.delete(context, bp.uuid); visibleCount = PAGE_SIZE; deleteTarget = null }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    renameTarget?.let { bp ->
        AlertDialog(onDismissRequest = { renameTarget = null }, title = { Text(stringResource(R.string.dialog_rename_title)) },
            text = {
                Column {
                    OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text(stringResource(R.string.dialog_rename_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.dialog_rename_preview, renameText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { if (renameText.isNotBlank() && renameText != bp.fileName) managementViewModel.rename(context, bp.uuid, renameText); renameTarget = null }) { Text(stringResource(R.string.action_confirm)) } },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    sheetTarget?.let { bp ->
        PcActionSheet(blueprint = bp, onDownloadOnly = { bridgeVm.requestDownload(bp.fileName) }, onViewOnly = { bridgeVm.requestDownload(bp.fileName) }, onDismiss = { sheetTarget = null })
    }
}

@Composable
private fun EmptyHomeState(modifier: Modifier = Modifier, onScanFolder: () -> Unit = {}, safFolderName: String? = null) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp).wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Icon(
            imageVector = androidx.compose.material.icons.Icons.Filled.Description,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            if (safFolderName != null) stringResource(R.string.home_empty_with_folder)
            else stringResource(R.string.home_empty_no_folder),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (safFolderName != null) {
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.home_empty_folder_label, safFolderName), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(20.dp))
        androidx.compose.material3.OutlinedButton(onClick = onScanFolder) {
            Text(if (safFolderName != null) stringResource(R.string.home_rescan_folder) else stringResource(R.string.home_pick_folder))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.home_pick_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(R.string.home_subdir_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun HomeBlueprintCard(blueprint: BlueprintMeta, onDetail: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit, onUpload: () -> Unit, connected: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onDetail() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // 第一行：标题 + 格式 chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        blueprint.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FormatChip(format = blueprint.format)
            }

            Spacer(Modifier.height(10.dp))

            // 第二行：作者 / 区域 / 方块数
            Text(
                stringResource(R.string.bp_card_author, blueprint.author.ifEmpty { stringResource(R.string.bp_card_author_unknown) }) +
                "  ·  " + stringResource(R.string.bp_card_region, blueprint.regionCount) +
                "  ·  " + stringResource(R.string.bp_card_blocks, formatNumber(blueprint.blockCount)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))

            // 第三行：操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onUpload, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.CloudUpload, stringResource(R.string.action_sync), modifier = Modifier.size(20.dp),
                        tint = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                }
                IconButton(onClick = onRename, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, stringResource(R.string.action_rename), modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Delete, stringResource(R.string.action_delete), modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun LocalBlueprintList(
    allBlueprints: List<BlueprintMeta>,
    scanning: Boolean,
    visibleCount: Int,
    onVisibleCountChange: (Int) -> Unit,
    safFolderName: String?,
    onRequestSafFolder: () -> Unit,
    navController: NavController,
    onBlueprintSelected: ((BlueprintMeta) -> Unit)?,
    onDeleteTarget: (BlueprintMeta) -> Unit,
    onRenameTarget: (BlueprintMeta) -> Unit,
    onUpload: (BlueprintMeta) -> Unit,
    bridgeConnected: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    val visibleBlueprints by remember(allBlueprints, visibleCount) {
        derivedStateOf { allBlueprints.take(visibleCount) }
    }
    val hasMore by remember(allBlueprints, visibleCount) {
        derivedStateOf { visibleCount < allBlueprints.size }
    }

    if (scanning) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.home_scanning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else if (allBlueprints.isEmpty()) {
        EmptyHomeState(
            onScanFolder = onRequestSafFolder,
            safFolderName = safFolderName,
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visibleBlueprints, key = { it.uuid }, contentType = { "bp" }) { bp ->
                HomeBlueprintCard(
                    blueprint = bp,
                    onDetail = {
                        if (onBlueprintSelected != null) {
                            onBlueprintSelected(bp)
                        } else {
                            navController.navigate(NavRoutes.detailRoute(bp.uuid))
                        }
                    },
                    onDelete = { onDeleteTarget(bp) },
                    onRename = { onRenameTarget(bp) },
                    onUpload = { onUpload(bp) },
                    connected = bridgeConnected,
                )
            }
            if (hasMore) {
                item(key = "load_more") {
                    LaunchedEffect(Unit) { onVisibleCountChange(visibleCount + PAGE_SIZE) }
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    }
                }
            }
            item(key = "bottom_spacer") { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun FormatChip(format: io.github.moxisuki.blockprint.core.SchematicFormat) {
    val bg = when (format) {
        io.github.moxisuki.blockprint.core.SchematicFormat.Litematica -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        io.github.moxisuki.blockprint.core.SchematicFormat.Sponge -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
    }
    val label = when (format) {
        io.github.moxisuki.blockprint.core.SchematicFormat.Litematica -> "Litematica"
        io.github.moxisuki.blockprint.core.SchematicFormat.Sponge -> "Sponge"
        else -> "NBT"
    }
    Box(Modifier.background(bg, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun PcHeader(state: ConnectionState, onRefresh: () -> Unit) {
    val isConnected = state is ConnectionState.Connected
    val isConnecting = state is ConnectionState.Connecting
    val session = (state as? ConnectionState.Connected)?.session
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(when { isConnected -> Color(0xFF4CAF50); isConnecting -> Color(0xFFFFC107); else -> Color(0xFF9E9E9E) }))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(session?.folderName ?: if (isConnecting) stringResource(R.string.bridge_connecting) else stringResource(R.string.home_pc_unknown), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (session != null) {
                Text("MC ${session.mcVersion} · ${session.loader} ${session.loaderVersion}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (state is ConnectionState.Connecting && state.host.isNotEmpty()) {
                Text("${state.host}:${state.port}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        IconButton(onClick = onRefresh, enabled = isConnected) { Icon(Icons.Default.Refresh, "刷新", tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) }
    }
}

@Composable
private fun EmptyPcState(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.home_pc_empty_title), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.home_pc_empty_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}