package io.github.moxisuki.blockprint.cat.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.saveable.rememberSaveable
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta

import io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeViewModel
import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionState
import io.github.moxisuki.blockprint.cat.ui.bridge.PcActionSheet
import io.github.moxisuki.blockprint.cat.ui.bridge.PcBlueprintCard
import io.github.moxisuki.blockprint.cat.ui.bridge.TransferProgressBar
import io.github.moxisuki.blockprint.cat.ui.format.BadgeColor
import io.github.moxisuki.blockprint.cat.ui.format.FormatCatalog
import io.github.moxisuki.blockprint.cat.ui.format.FormatFilter
import io.github.moxisuki.blockprint.cat.ui.format.formatShortLabelRes
import io.github.moxisuki.blockprint.cat.ui.management.BlueprintViewModel
import io.github.moxisuki.blockprint.cat.ui.management.ManagementEvent
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber

private const val PAGE_SIZE = 15
private val SliderInset = 3.dp
private val CapsuleWidth = 160.dp

/**
 * True if [name] contains any character outside the "safe" set for WorldEdit
 * (Sponge) schematic files: ASCII letters, digits, underscore, dot, hyphen.
 * WorldEdit rejects anything else (Chinese characters, spaces, special
 * punctuation), so the upload step must warn the user before sending.
 */
private fun hasUnsafeWorldEditChars(name: String): Boolean =
    !name.matches(Regex("^[A-Za-z0-9_.\\-]+$"))

/**
 * Produce a WorldEdit-safe filename by **removing** any character outside
 * the safe set (ASCII letters, digits, underscore, dot, hyphen). Strips
 * unsafe chars rather than replacing them with `_` — "樱花小屋.schem"
 * becomes "小屋.schem" (or just ".schem" if all leading chars are unsafe).
 * Also collapses runs of dots and trims leading/trailing dots so the result
 * is a valid filename stem, not just a prefix of dots.
 */
private fun safeWorldEditName(name: String): String {
    val stripped = name.replace(Regex("[^A-Za-z0-9_.\\-]"), "")
    val collapsed = stripped.replace(Regex("\\.+"), ".")
    return collapsed.trim('.', '_', '-')
}

@Composable
fun HomeScreen(
    navController: NavController,
    bridgeVm: BridgeViewModel = hiltViewModel(),
    viewModel: HomeViewModel = hiltViewModel(),
    managementViewModel: BlueprintViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onRequestSafFolder: () -> Unit = {},
    onRefresh: (tab: Int) -> Unit = {},
    onBlueprintSelected: ((BlueprintMeta) -> Unit)? = null,
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
    val inFlight by bridgeVm.isTaskInFlight.collectAsState()
    val canTransfer = !inFlight && isBridgeConnected
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
    var pendingUploadAfterRename by remember { mutableStateOf<BlueprintMeta?>(null) }
    var worldEditWarningBlueprint by remember { mutableStateOf<BlueprintMeta?>(null) }
    var sheetTarget by remember { mutableStateOf<RemoteBlueprint?>(null) }
    // 单一数据源：pagerState 是真理。savedInitialPage 仅承担 rememberSaveable
    // 持久化职责（旋转 / 进程重启时恢复 tab）。
    var savedInitialPage by rememberSaveable { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = savedInitialPage) { 2 }
    LaunchedEffect(pagerState.currentPage) {
        savedInitialPage = pagerState.currentPage
    }
    // 只读派生别名，避免下游 selectedTab == 0 等调用点全部改名。
    val selectedTab = pagerState.currentPage

    // 点胶囊 → 推 pager。和滑动 settle 走同一条 AnimSpec.tabSwitch，
    // 让两种交互的动效性格完全一致。
    val onTabClick: (Int) -> Unit = { target ->
        scope.launch {
            pagerState.animateScrollToPage(
                target,
                animationSpec = AnimSpec.tabSwitch,
            )
        }
    }
    var localFilterVisible by rememberSaveable { mutableStateOf(false) }
    var localFilterQuery by rememberSaveable { mutableStateOf("") }
    var localFilterFormat by rememberSaveable { mutableStateOf(FormatFilter.All) }

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
        // Tab switcher on the left, action buttons clustered on the right.
        // Same outer Row so the surfaceVariant pill + the icons share one
        // visual baseline; spacer-weight pushes them apart. Putting the
        // Upload/Filter inside the same Row as the pill made the capsule
        // grow to ~75% of a 360dp screen — splitting keeps it compact.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 胶囊：BoxWithConstraints 容器（用来测自身宽度算滑块 offset）
            //  - 底层：等宽滑块（宽 = 容器宽 / 2，offset 跟 currentPageOffsetFraction）
            //  - 上层：Row 两个 tab 文字（weight(1f) 等宽，颜色 lerp 算出）
            //
            // 滑块 offset 和文字颜色直接读 pagerState 的 fraction，不叠任何
            // animateAsState —— pager 内部已经用 AnimSpec.tabSwitch 驱动这个
            // fraction，再叠会变成"弹簧之上做弹簧"。
            BoxWithConstraints(
                modifier = Modifier
                    .width(CapsuleWidth)
                    .height(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            ) {
                val tabWidthDp = maxWidth / 2
                val tabWidthPx = with(LocalDensity.current) { tabWidthDp.toPx() }

                // pagePos：0.0 = 完全在 tab 0，1.0 = 完全在 tab 1，
                // 中间值（含负数 / >1，boundary 反向滑时）由 coerceIn 兜
                val pagePos by remember(pagerState) {
                    derivedStateOf {
                        pagerState.currentPage + pagerState.currentPageOffsetFraction
                    }
                }
                // Clamp to [0, 1] for the slider — pager overscroll / bounce can push
                // fraction outside the range; without clamping, the slider would fly
                // out of the capsule. Coverage values (below) are already in range
                // because they're derived from a clamped input.
                val pagePosClamped = pagePos.coerceIn(0f, 1f)

                // 底层滑块。padding(SliderInset) (3dp 上下) 让 slider 视觉上和原版"选中 Box 外缘 3dp"对齐。
                // slider 28dp + padding 6dp = 34dp 撑起 BoxWithConstraints；这样 capsule 高度
                // 始终 34dp，跟按钮组高度变化（expandVertically/shrinkVertically）解耦，
                // 切换 tab 时 capsule 不会上下抖动。
                val sliderOffsetPx = pagePosClamped * tabWidthPx
                Box(
                    modifier = Modifier
                        .padding(SliderInset)
                        .width(tabWidthDp - SliderInset * 2)
                        .height(28.dp)
                        .offset { IntOffset(sliderOffsetPx.roundToInt(), 0) }
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )

                // 上层：两个 tab 文字，等宽 + clickable + 颜色 lerp
                val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                val selectedColor = MaterialTheme.colorScheme.onPrimary
                val coverage0 = 1f - pagePosClamped
                val coverage1 = pagePosClamped
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(R.string.home_tab_local to 0, R.string.home_tab_pc to 1).forEach { (labelRes, tab) ->
                        val label = stringResource(labelRes)
                        val coverage = if (tab == 0) coverage0 else coverage1
                        val textColor = lerp(unselectedColor, selectedColor, coverage)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onTabClick(tab) }
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // Local-tab action cluster: Refresh + Upload + Filter. All three
            // share ONE AnimatedVisibility so the fade in/out timing is
            // exactly synchronised — same enter/exit spec, same trigger
            // (selectedTab == 0), no per-button round-trip offset to fight
            // the pager's own page transition.
            //
            // Refresh on PC tab is intentionally NOT shown — bridge events
            // push list updates automatically, so a manual refresh there
            // would either be redundant or race against an in-flight event.
            val refreshRotation by rememberInfiniteTransition(label = "refresh")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                    ),
                    label = "refreshRotation",
                )
            val scope = rememberCoroutineScope()
            var refreshing by remember { mutableStateOf(false) }
            val refreshTint by animateColorAsState(
                targetValue = if (refreshing)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(durationMillis = 180),
                label = "refreshTint",
            )
            AnimatedVisibility(
                visible = selectedTab == 0,
                // Don't shrink on exit — that would let the row's height collapse,
                // pulling the pill out of verticalAlignment.CenterVertically and
                // making the capsule jitter as the user swipes between tabs.
                enter = fadeIn(animationSpec = tween(180)),
                exit = fadeOut(animationSpec = tween(140)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (refreshing) return@IconButton
                            visibleCount = PAGE_SIZE
                            refreshing = true
                            onRefresh(0)
                            scope.launch {
                                delay(1_000)
                                refreshing = false
                            }
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (refreshing) refreshRotation else 0f),
                            tint = refreshTint,
                        )
                    }
                    IconButton(onClick = { filePicker.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = "导入蓝图",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { localFilterVisible = !localFilterVisible }, modifier = Modifier.size(32.dp)) {
                        val filterActive = localFilterQuery.isNotEmpty() || localFilterFormat != FormatFilter.All
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "筛选",
                            modifier = Modifier.size(18.dp),
                            tint = if (localFilterVisible || filterActive) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        val transfers by bridgeVm.transfers.collectAsState()
        androidx.compose.animation.AnimatedVisibility(
            visible = transfers.isNotEmpty(),
            enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(300))
                + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(220)),
            exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(350))
                + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(280)),
        ) {
            TransferProgressBar(
                transfers = transfers,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // HorizontalPager gives swipe-to-switch-tab between Local (page 0) and
        // PC (page 1). Pill click scrolls the pager; swipe settle updates the
        // pill highlight. See the two LaunchedEffects above for the sync.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            key = { it }, // page 0 → LocalBlueprintList, page 1 → PC content
            pageSpacing = 0.dp,
            userScrollEnabled = true,
            // Keep the adjacent page composed off-screen. Without this,
            // switching tabs disposes the off-screen page and the next
            // switch re-composes it from scratch — that's where the
            // perceived "jank" on pill-tap comes from (LazyColumn state
            // resets, header rebinds, scroll position lost).
            beyondViewportPageCount = 1,
            // 滑动松手 settle 走同一条 AnimSpec.tabSwitch 弹簧，
            // 与 onTabClick 的 animateScrollToPage 完全一致——这就是
            // "点击和滑动动效统一"的最关键一行。decay（fling 惯性）
            // 保持默认，物理摩擦模型不归弹簧管。
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = AnimSpec.tabSwitch,
            ),
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (page) {
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
                                val isSponge = bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Sponge
                                if (isSponge && hasUnsafeWorldEditChars(bp.fileName)) {
                                    // Don't upload yet — show the warning dialog and let the user choose.
                                    worldEditWarningBlueprint = bp
                                } else {
                                    scope.launch {
                                        val bytes = managementViewModel.readBytes(bp.uuid)
                                        if (bytes != null) {
                                            bridgeVm.requestUpload(bp.fileName, bytes, overwrite = true)
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.action_sync_failed, bp.fileName)) }
                                        }
                                    }
                                }
                            },
                            bridgeConnected = isBridgeConnected,
                            canTransfer = canTransfer,
                            snackbarHostState = snackbarHostState,
                            filterVisible = localFilterVisible,
                            filterQuery = localFilterQuery,
                            onFilterQueryChange = { localFilterQuery = it; visibleCount = PAGE_SIZE },
                            filterFormat = localFilterFormat,
                            onFilterFormatChange = { localFilterFormat = it; visibleCount = PAGE_SIZE },
                        )
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
                                PcHeader(state = connectionState)
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
        AlertDialog(onDismissRequest = { renameTarget = null; pendingUploadAfterRename = null }, title = { Text(stringResource(R.string.dialog_rename_title)) },
            text = {
                Column {
                    OutlinedTextField(value = renameText, onValueChange = { renameText = it }, label = { Text(stringResource(R.string.dialog_rename_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.dialog_rename_preview, renameText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
            TextButton(onClick = {
                val wasPendingUpload = pendingUploadAfterRename
                val newName = renameText.trim()
                if (newName.isNotBlank() && newName != bp.fileName) {
                    managementViewModel.rename(context, bp.uuid, newName)
                }
                renameTarget = null
                // If this rename was triggered by the upload safety check, kick off
                // the upload with the new name as soon as the dialog closes.
                if (wasPendingUpload != null) {
                    pendingUploadAfterRename = null
                    scope.launch {
                        val bytes = managementViewModel.readBytes(wasPendingUpload.uuid)
                        if (bytes != null) {
                            bridgeVm.requestUpload(newName, bytes, overwrite = true)
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.action_sync_failed, newName)) }
                        }
                    }
                }
            }, enabled = canTransfer) {
                Text(stringResource(R.string.action_confirm))
            }
        },
            dismissButton = { TextButton(onClick = { renameTarget = null; pendingUploadAfterRename = null }) { Text(stringResource(R.string.action_cancel)) } })
    }
    worldEditWarningBlueprint?.let { bp ->
        AlertDialog(
            onDismissRequest = { worldEditWarningBlueprint = null },
            title = { Text(stringResource(R.string.upload_worldedit_warn_dialog_title)) },
            text = {
                Text(stringResource(R.string.upload_worldedit_warn_dialog_message, bp.fileName))
            },
            confirmButton = {
                // "Rename" — pre-fill the existing rename dialog with a safe suggestion.
                TextButton(onClick = {
                    worldEditWarningBlueprint = null
                    renameTarget = bp
                    renameText = safeWorldEditName(bp.fileName)
                    pendingUploadAfterRename = bp
                }) { Text(stringResource(R.string.dialog_rename_title)) }
            },
            dismissButton = {
                // "Upload anyway" — skip the rename; the PC side will likely fail,
                // but the user is asking for it explicitly.
                Row {
                    TextButton(onClick = {
                        worldEditWarningBlueprint = null
                        scope.launch {
                            val bytes = managementViewModel.readBytes(bp.uuid)
                            if (bytes != null) {
                                bridgeVm.requestUpload(bp.fileName, bytes, overwrite = true)
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.action_sync_failed, bp.fileName)) }
                            }
                        }
                    }, enabled = canTransfer) { Text(stringResource(R.string.action_upload_anyway)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { worldEditWarningBlueprint = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            },
        )
    }
    sheetTarget?.let { bp ->
        PcActionSheet(blueprint = bp, onDownload = { bridgeVm.requestDownload(bp.fileName, bp.source) }, enabled = canTransfer, onDismiss = { sheetTarget = null })
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
private fun HomeBlueprintCard(blueprint: BlueprintMeta, onDetail: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit, onUpload: () -> Unit, connected: Boolean = false, canTransfer: Boolean = true) {
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
                IconButton(onClick = onUpload, enabled = canTransfer, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.CloudUpload, stringResource(R.string.action_sync), modifier = Modifier.size(20.dp),
                        tint = if (canTransfer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
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
    canTransfer: Boolean,
    snackbarHostState: SnackbarHostState,
    filterVisible: Boolean,
    filterQuery: String,
    onFilterQueryChange: (String) -> Unit,
    filterFormat: FormatFilter,
    onFilterFormatChange: (FormatFilter) -> Unit,
) {
    // Debounce search query (avoid re-filtering on every keystroke)
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(filterQuery) {
        kotlinx.coroutines.delay(120)
        debouncedQuery = filterQuery
    }

    val filtered = remember(allBlueprints, debouncedQuery, filterFormat) {
        val q = debouncedQuery.trim().lowercase()
        allBlueprints.filter { bp ->
            val matchesQuery = q.isEmpty() ||
                bp.displayName.lowercase().contains(q) ||
                bp.fileName.lowercase().contains(q)
            val matchesFormat = when (filterFormat) {
                FormatFilter.All -> true
                FormatFilter.Litematica -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Litematica
                FormatFilter.Schematic -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Sponge
                FormatFilter.Nbt -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Structure ||
                                     bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.PartialNbt ||
                                     bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Unknown
                FormatFilter.Json -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.BuildingHelper
            }
            matchesQuery && matchesFormat
        }
    }
    val visibleBlueprints by remember(filtered, visibleCount) {
        derivedStateOf { filtered.take(visibleCount) }
    }
    val hasMore by remember(filtered, visibleCount) {
        derivedStateOf { visibleCount < filtered.size }
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
        Column(modifier = Modifier.fillMaxSize()) {
            androidx.compose.animation.AnimatedVisibility(
                visible = filterVisible,
                enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(220))
                    + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(180)),
                exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(280))
                    + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(220)),
            ) {
                BlueprintFilterBar(
                    query = filterQuery,
                    onQueryChange = onFilterQueryChange,
                    selected = filterFormat,
                    onSelectedChange = onFilterFormatChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (visibleBlueprints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.home_filter_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                            canTransfer = canTransfer,
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
    }
}

@Composable
private fun BlueprintFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selected: FormatFilter,
    onSelectedChange: (FormatFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.home_filter_search_hint), style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = if (query.isNotEmpty()) {{
                androidx.compose.material3.IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }} else null,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FormatChipFilter(label = stringResource(R.string.home_filter_format_all), selected = selected == FormatFilter.All) { onSelectedChange(FormatFilter.All) }
            FormatChipFilter(label = stringResource(R.string.format_filter_litematica), selected = selected == FormatFilter.Litematica) { onSelectedChange(FormatFilter.Litematica) }
            FormatChipFilter(label = stringResource(R.string.format_filter_schematic), selected = selected == FormatFilter.Schematic) { onSelectedChange(FormatFilter.Schematic) }
            FormatChipFilter(label = stringResource(R.string.format_filter_json), selected = selected == FormatFilter.Json) { onSelectedChange(FormatFilter.Json) }
            FormatChipFilter(label = stringResource(R.string.format_filter_nbt), selected = selected == FormatFilter.Nbt) { onSelectedChange(FormatFilter.Nbt) }
        }
    }
}

@Composable
private fun FormatChipFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.height(28.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    )
}

@Composable
private fun FormatChip(format: io.github.moxisuki.blockprint.core.SchematicFormat) {
    val display = FormatCatalog.from(format)
    val bg = when (display.badgeColor) {
        BadgeColor.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        BadgeColor.Secondary -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        // Catalog currently emits Primary/Secondary/Outline; anything else
        // (Tertiary, future colors) gets the neutral Outline tint.
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
    }
    Box(
        Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            stringResource(formatShortLabelRes(format)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun PcHeader(state: ConnectionState) {
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