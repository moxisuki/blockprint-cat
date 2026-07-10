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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta
import io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint
import io.github.moxisuki.blockprint.cat.data.category.CategoryEntity
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeViewModel
import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionState
import io.github.moxisuki.blockprint.cat.ui.bridge.PcActionSheet
import io.github.moxisuki.blockprint.cat.ui.bridge.PcBlueprintCard
import io.github.moxisuki.blockprint.cat.ui.bridge.TransferProgressBar
import io.github.moxisuki.blockprint.cat.ui.category.CategoryMoveDialog
import io.github.moxisuki.blockprint.cat.ui.category.EditCategoryDialog
import io.github.moxisuki.blockprint.cat.ui.category.MultiSelectAppBar
import io.github.moxisuki.blockprint.cat.ui.category.MultiSelectBottomBar
import io.github.moxisuki.blockprint.cat.ui.category.NewCategoryDialog
import io.github.moxisuki.blockprint.cat.ui.format.FormatFilter
import io.github.moxisuki.blockprint.cat.ui.home.MultiSelectState
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.ui.home.components.EmptyPcState
import io.github.moxisuki.blockprint.cat.ui.home.components.PcHeader
import io.github.moxisuki.blockprint.cat.ui.home.util.hasUnsafeWorldEditChars
import io.github.moxisuki.blockprint.cat.ui.home.util.safeWorldEditName
import io.github.moxisuki.blockprint.cat.ui.management.BlueprintViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val PAGE_SIZE = 15
private val SliderInset = 3.dp
private val CapsuleWidth = 160.dp

/**
 * Home tab entry. After the Task #4 split this file owns only the chrome:
 * the pill-style Local/PC tab switcher at the top, the Local-tab action
 * cluster (refresh / upload / filter), the transfer progress bar strip, the
 * [HorizontalPager] wiring between the two pages, and the four dialogs
 * (delete / rename / world-edit warning / PC action sheet).
 *
 * Everything else moved out:
 *   - Local list body  → LocalBlueprintList ( this file's sibling HomeLocalList.kt )
 *   - Local card       → ui/home/components/HomeBlueprintCard.kt
 *   - Filter bar       → ui/home/HomeLocalList.kt (BlueprintFilterBar)
 *   - Filter chips     → ui/home/components/HomeFilterChips.kt
 *   - PC section       → ui/home/components/HomePcSection.kt (PcHeader)
 *   - Empty states     → ui/home/components/HomeEmptyStates.kt
 *   - WorldEdit utils  → ui/home/util/HomeNameUtils.kt
 *
 * Signature unchanged: same 8 named parameters, same defaults.
 */
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
    val inFlight by bridgeVm.isTaskInFlight.collectAsState()
    val canTransfer = !inFlight && isBridgeConnected
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allBlueprints by viewModel.displayedBlueprints.collectAsStateWithLifecycle()
    val multi by viewModel.multi.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val safFolderName = viewModel.safFolderName()
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

    // Category dialog state
    var showNewDialog by rememberSaveable { mutableStateOf(false) }
    var editCategoryFor by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteCategoryFor by remember { mutableStateOf<CategoryEntity?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }
    // 单一数据源：pagerState 是真理。savedInitialPage 仅承担 rememberSaveable
    // 持久化职责（旋转 / 进程重启时恢复 tab）。
    var savedInitialPage by rememberSaveable { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = savedInitialPage) { 2 }
    LaunchedEffect(pagerState.currentPage) {
        savedInitialPage = pagerState.currentPage
    }
    val selectedTab = pagerState.currentPage

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (val m = multi) {
            MultiSelectState.Off -> Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            BoxWithConstraints(
                modifier = Modifier
                    .width(CapsuleWidth)
                    .height(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            ) {
                val tabWidthDp = maxWidth / 2
                // Cache the dp→px conversion: maxWidth is stable per BoxWithConstraints
                // re-measure, but `with(LocalDensity.current) {...}` is a Composable call.
                // Hoist the density read into a normal val (Composable context) and let
                // the remember lambda only do the pure conversion.
                val density = LocalDensity.current
                val tabWidthPx = remember(maxWidth, density) {
                    with(density) { tabWidthDp.toPx() }
                }

                val pagePos by remember(pagerState) {
                    derivedStateOf {
                        pagerState.currentPage + pagerState.currentPageOffsetFraction
                    }
                }
                val pagePosClamped = pagePos.coerceIn(0f, 1f)

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
            val refreshRotation by rememberInfiniteTransition(label = "refresh")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                    ),
                    label = "refreshRotation",
                )
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
                            contentDescription = stringResource(R.string.cd_refresh),
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (refreshing) refreshRotation else 0f),
                            tint = refreshTint,
                        )
                    }
                    IconButton(
                        onClick = {
                            filePicker.launch(arrayOf("application/octet-stream", "*/*"))
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = stringResource(R.string.home_cd_import),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { localFilterVisible = !localFilterVisible },
                        modifier = Modifier.size(32.dp),
                    ) {
                        val filterActive = localFilterQuery.isNotEmpty() || localFilterFormat != FormatFilter.All
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.home_cd_filter),
                            modifier = Modifier.size(18.dp),
                            tint = if (localFilterVisible || filterActive) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
            is MultiSelectState.On -> MultiSelectAppBar(
                selectedCount = m.selected.size,
                allSelected = m.selected.size == allBlueprints.size,
                onCancel = viewModel::exitMulti,
                onToggleSelectAll = viewModel::selectAll,
            )
        }

        val transfers by bridgeVm.transfers.collectAsState()
        AnimatedVisibility(
            visible = transfers.isNotEmpty(),
            enter = androidx.compose.animation.expandVertically(animationSpec = tween(300))
                + androidx.compose.animation.fadeIn(animationSpec = tween(220)),
            exit = androidx.compose.animation.shrinkVertically(animationSpec = tween(350))
                + androidx.compose.animation.fadeOut(animationSpec = tween(280)),
        ) {
            TransferProgressBar(
                transfers = transfers,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            key = { it },
            pageSpacing = 0.dp,
            userScrollEnabled = true,
            beyondViewportPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = AnimSpec.tabSwitch,
            ),
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (page) {
                    0 -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            LocalBlueprintList(
                                modifier = Modifier.weight(1f),
                                allBlueprints = allBlueprints,
                                scanning = scanning,
                                visibleCount = visibleCount,
                                onVisibleCountChange = { visibleCount = it },
                                safFolderName = safFolderName,
                                onRequestSafFolder = onRequestSafFolder,
                                navController = navController,
                                onBlueprintSelected = { bp ->
                                    // In multi-select mode, tap toggles selection instead of opening detail.
                                    if (multi is MultiSelectState.On) {
                                        viewModel.toggleSelected(bp.uuid)
                                    } else if (onBlueprintSelected != null) {
                                        onBlueprintSelected.invoke(bp)
                                    } else {
                                        navController.navigate(NavRoutes.detailRoute(bp.uuid))
                                    }
                                },
                                onDeleteTarget = { deleteTarget = it },
                                onRenameTarget = { bp -> renameTarget = bp; renameText = bp.fileName },
                                onUpload = { bp ->
                                    val isSponge = bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Sponge
                                    if (isSponge && hasUnsafeWorldEditChars(bp.fileName)) {
                                        worldEditWarningBlueprint = bp
                                    } else {
                                        scope.launch {
                                            val bytes = managementViewModel.readBytes(bp.uuid)
                                            if (bytes != null) {
                                                bridgeVm.requestUpload(bp.fileName, bytes, overwrite = true)
                                            } else {
                                                snackbarHostState.showSnackbar(
                                                    context.getString(R.string.action_sync_failed, bp.fileName)
                                                )
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
                                categories = categories,
                                selectedCategoryId = selectedCategoryId,
                                onCategorySelect = { row ->
                                    when (row) {
                                        is CategoryRow.All -> viewModel.selectCategory(null)
                                        is CategoryRow.Real -> viewModel.selectCategory(row.entity.id)
                                    }
                                },
                                onCategoryLongClick = { row ->
                                    if (row is CategoryRow.Real) editCategoryFor = row.entity
                                },
                                onAddCategory = { showNewDialog = true },
                                onLongPress = viewModel::enterMultiSelect,
                                isSelected = { uuid ->
                                    (multi as? MultiSelectState.On)?.selected?.contains(uuid) == true
                                },
                            )
                        }
                    }
                    1 -> {
                        if (pcSession == null && !isBridgeConnecting) {
                            Column(
                                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Default.Cloud, null,
                                    Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.home_pc_not_connected),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.home_connect_pc_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                PcHeader(state = connectionState)
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (pcEntries.isEmpty() && isBridgeConnected) {
                                        EmptyPcState(modifier = Modifier.align(Alignment.Center))
                                    } else if (pcEntries.isEmpty() && !isBridgeConnected) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.align(Alignment.Center).size(20.dp),
                                        )
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
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

        val m = multi
        if (m is MultiSelectState.On) {
            MultiSelectBottomBar(
                onMove = { showMoveDialog = true },
                onDelete = {
                    scope.launch {
                        m.selected.forEach { uuid ->
                            managementViewModel.delete(context, uuid)
                        }
                        visibleCount = PAGE_SIZE
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    deleteTarget?.let { bp ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_msg, bp.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        managementViewModel.delete(context, bp.uuid)
                        visibleCount = PAGE_SIZE
                        deleteTarget = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    renameTarget?.let { bp ->
        AlertDialog(
            onDismissRequest = { renameTarget = null; pendingUploadAfterRename = null },
            title = { Text(stringResource(R.string.dialog_rename_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(stringResource(R.string.dialog_rename_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.dialog_rename_preview, renameText),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val wasPendingUpload = pendingUploadAfterRename
                        val newName = renameText.trim()
                        if (newName.isNotBlank() && newName != bp.fileName) {
                            managementViewModel.rename(context, bp.uuid, newName)
                        }
                        renameTarget = null
                        if (wasPendingUpload != null) {
                            pendingUploadAfterRename = null
                            scope.launch {
                                val bytes = managementViewModel.readBytes(wasPendingUpload.uuid)
                                if (bytes != null) {
                                    bridgeVm.requestUpload(newName, bytes, overwrite = true)
                                } else {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.action_sync_failed, newName)
                                    )
                                }
                            }
                        }
                    },
                    enabled = canTransfer,
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null; pendingUploadAfterRename = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
    worldEditWarningBlueprint?.let { bp ->
        AlertDialog(
            onDismissRequest = { worldEditWarningBlueprint = null },
            title = { Text(stringResource(R.string.upload_worldedit_warn_dialog_title)) },
            text = {
                Text(stringResource(R.string.upload_worldedit_warn_dialog_message, bp.fileName))
            },
            confirmButton = {
                TextButton(onClick = {
                    worldEditWarningBlueprint = null
                    renameTarget = bp
                    renameText = safeWorldEditName(bp.fileName)
                    pendingUploadAfterRename = bp
                }) { Text(stringResource(R.string.dialog_rename_title)) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            worldEditWarningBlueprint = null
                            scope.launch {
                                val bytes = managementViewModel.readBytes(bp.uuid)
                                if (bytes != null) {
                                    bridgeVm.requestUpload(bp.fileName, bytes, overwrite = true)
                                } else {
                                    snackbarHostState.showSnackbar(
                                        context.getString(R.string.action_sync_failed, bp.fileName)
                                    )
                                }
                            }
                        },
                        enabled = canTransfer,
                    ) { Text(stringResource(R.string.action_upload_anyway)) }
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

    // --- Category dialogs ---

    if (showNewDialog) {
        NewCategoryDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { name, color, pattern ->
                scope.launch { viewModel.createCategory(name, color, pattern) }
                showNewDialog = false
            },
        )
    }

    editCategoryFor?.let { cat ->
        EditCategoryDialog(
            category = cat,
            blueprintCount = viewModel.counts.value[cat.id] ?: 0,
            onDismiss = { editCategoryFor = null },
            onConfirm = { name, color, pattern ->
                scope.launch {
                    viewModel.renameCategory(cat.id, name)
                    viewModel.changeCover(cat.id, color, pattern)
                }
                editCategoryFor = null
            },
            onDelete = { deleteCategoryFor = cat },
        )
    }

    deleteCategoryFor?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteCategoryFor = null },
            title = { Text(stringResource(R.string.cat_dialog_delete_title, cat.name)) },
            text = {
                Text(
                    stringResource(
                        R.string.cat_dialog_delete_body,
                        viewModel.counts.value[cat.id] ?: 0,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteCategory(cat.id)
                            viewModel.selectCategory(null)
                        }
                        deleteCategoryFor = null
                        editCategoryFor = null
                    },
                ) {
                    Text(
                        stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryFor = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showMoveDialog) {
        val m = multi as? MultiSelectState.On
        if (m != null) {
            CategoryMoveDialog(
                count = m.selected.size,
                rows = viewModel.categories.value.filterIsInstance<CategoryRow.Real>(),
                onDismiss = { showMoveDialog = false },
                onPick = { picked ->
                    val targetId = (picked as? CategoryRow.Real)?.entity?.id
                    scope.launch { viewModel.moveSelectedTo(targetId) }
                    showMoveDialog = false
                },
            )
        }
    }
}
