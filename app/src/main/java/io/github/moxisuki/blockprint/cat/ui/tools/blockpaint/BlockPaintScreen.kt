@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package io.github.moxisuki.blockprint.cat.ui.tools.blockpaint

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockCatalog
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockEntry
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockGroup
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.components.getOrWarmPixelArt
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.components.prewarmPixelArt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.min

private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 6f

@Composable
fun BlockPaintScreen(
    navController: androidx.navigation.NavController,
    viewModel: BlockPaintViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val scale by viewModel.scale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    val exportPayload by viewModel.exportPayload.collectAsState()
    val paintingName by viewModel.currentPaintingName.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            prewarmPixelArt(
                context = context,
                resIds = BlockCatalog.all.map { it.drawableResId },
            )
        }
    }

    LaunchedEffect(state.resultBitmap) {
        if (state.resultBitmap != null && exportPayload == null) {
            viewModel.encodeForExport()?.let { encoded ->
                viewModel.setExportPayload(encoded)
                scope.launch { sheetState.show() }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BlockPaintBodyToolBar(viewModel = viewModel)

        BlockPickerStrip(
            selectedBlockId = state.selectedBlockId,
            onSelect = viewModel::selectBlock,
        )

        BlockPaintBody(
            modifier = Modifier.weight(1f, fill = true),
            state = state,
            context = context,
            scale = scale,
            panOffset = panOffset,
            onTransform = viewModel::transform,
            onPaint = viewModel::paint,
        )

        BlockPaintStatusRow(
            width = state.width,
            height = state.height,
            scale = scale,
            paintedCount = state.resultTotalBlocks,
            onSetSize = viewModel::setSize,
        )
    }

    val suffix = stringResource(R.string.bp_painting_suffix)
    io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.ExportBottomSheet(
        exportPayload = exportPayload,
        resultBitmap = state.resultBitmap,
        defaultSaveName = "${paintingName}_$suffix",
        sheetState = sheetState,
        context = context,
        onClose = { viewModel.setExportPayload(null) },
        onViewBlueprint = { uuid ->
            navController.navigate(io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes.detailRoute(uuid))
        },
    )

    BlockPaintPaintingListSheet(viewModel = viewModel)
}


/**
 * 工具栏 actions：放在共享 AppTopBar 的 customActions 槽里。
 * 用 hiltViewModel(viewModelStoreOwner = entry) 取 BlockPaint 路由的 VM，
 * 这样 actions（在 AppTopBar 渲染）和 BlockPaintScreen 共享同一个 VM 实例。
 */
@Composable
fun BlockPaintTopBarActions(viewModel: BlockPaintViewModel) {
    val state by viewModel.state.collectAsState()
    IconButton(
        onClick = viewModel::prepareExport,
        enabled = !state.isUpdating,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            Icons.Outlined.IosShare,
            contentDescription = stringResource(R.string.bp_export_short),
            tint = if (!state.isUpdating) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

/**
 * 给 AppNavGraph 用：拿到当前图画的名称作为 AppBar 标题。
 * entry 为 null 时返回 stringResource(R.string.tool_block_paint) 作为默认。
 */
@Composable
fun blockPaintTitleForEntry(viewModelStoreOwner: androidx.lifecycle.ViewModelStoreOwner?): String {
    val default = stringResource(R.string.tool_block_paint)
    if (viewModelStoreOwner == null) return default
    val vm: BlockPaintViewModel = androidx.hilt.navigation.compose.hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
    val name by vm.currentPaintingName.collectAsState()
    return name.ifBlank { default }
}


/**
 * Inline block picker strip below toolbar. Horizontal tabs for BlockGroup,
 * selected group's blocks shown in a horizontal LazyRow. No borders —
 * just icons with alpha-based selection.
 */
@Composable
internal fun BlockPickerStrip(
    selectedBlockId: String?,
    onSelect: (String?) -> Unit,
) {
    var selectedGroup by remember { mutableStateOf(BlockGroup.WOOL) }
    val blocks = remember(selectedGroup) { BlockCatalog.byGroup[selectedGroup].orEmpty() }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Group tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(BlockGroup.entries.size) { idx ->
                val group = BlockGroup.entries[idx]
                val isSelected = group == selectedGroup
                Text(
                    text = stringResource(group.labelRes),
                    modifier = Modifier
                        .clickable { selectedGroup = group }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Blocks row
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(blocks, key = { it.id }) { block ->
                val selected = block.id == selectedBlockId
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onSelect(block.id); },
                    contentAlignment = Alignment.Center,
                ) {
                    io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.components.BlockPreview(
                        drawableResId = block.drawableResId,
                        dimmed = !selected,
                        modifier = Modifier.size(36.dp),
                        withBorder = false,
                    )
                }
            }
        }
    }
}


/**
 * Body 工具栏（在 AppBar 下面）：画笔/橡皮切换、画笔选择、缩放三件、清空、图画列表。
 * 导出按钮放在 AppBar 里，避免这里图标过多。
 */
@Composable
private fun BlockPaintBodyToolBar(viewModel: BlockPaintViewModel) {
    val state by viewModel.state.collectAsState()
    val selectedEntry = remember(state.selectedBlockId) {
        BlockCatalog.all.firstOrNull { it.id == state.selectedBlockId }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
            IconToggleButton(
                checked = state.tool == PaintTool.Paint,
                onCheckedChange = { viewModel.setTool(PaintTool.Paint) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Outlined.Brush,
                    contentDescription = stringResource(R.string.bp_tool_paint),
                    tint = if (state.tool == PaintTool.Paint) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconToggleButton(
                checked = state.tool == PaintTool.Erase,
                onCheckedChange = { viewModel.setTool(PaintTool.Erase) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    Icons.Outlined.CleaningServices,
                    contentDescription = stringResource(R.string.bp_tool_erase),
                    tint = if (state.tool == PaintTool.Erase) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BrushSwatch(entry = selectedEntry, onClick = { })
            Spacer(Modifier.weight(1f))
            IconButton(onClick = viewModel::zoomOut, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Remove, contentDescription = stringResource(R.string.bp_zoom_out), tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = viewModel::zoomReset, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.RestartAlt, contentDescription = stringResource(R.string.bp_zoom_reset), tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = viewModel::zoomIn, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.bp_zoom_in), tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = viewModel::clearCanvas, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.bp_tool_clear), tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { viewModel.openPaintingList() }, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Outlined.CollectionsBookmark,
                    contentDescription = stringResource(R.string.bp_list_title),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
    }
}

/**
 * AppNavGraph 拿当前图画名：返回 String 供 AppTopBar 标题用。
 * 非 BlockPaint 页面返回 null（让 caller 用默认 topBarTitle）。
 */
@Composable
internal fun currentBlockPaintNameOrNull(
    flags: io.github.moxisuki.blockprint.cat.ui.navigation.NavGraphFlags,
    navController: androidx.navigation.NavController,
): String? {
    if (!flags.isBlockPaint) return null
    val entry = runCatching { navController.getBackStackEntry(io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes.BLOCK_PAINT) }.getOrNull()
        ?: return null
    val bpVm: BlockPaintViewModel = androidx.hilt.navigation.compose.hiltViewModel(viewModelStoreOwner = entry)
    val name by bpVm.currentPaintingName.collectAsState()
    return name.ifBlank { null }
}

/**
 * 图画列表 bottom sheet：展示所有 Room 里的图画，底部 "+" 新建。
 * 当前选中的图有高亮。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BlockPaintPaintingListSheet(
    viewModel: BlockPaintViewModel,
) {
    val showList by viewModel.showPaintingList.collectAsState()
    val paintings by viewModel.paintings.collectAsState()
    val currentId by viewModel.currentPaintingId.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showList) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closePaintingList() },
            sheetState = sheetState,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.bp_list_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                val ctx = LocalContext.current
                TextButton(onClick = {
                    val name = ctx.getString(
                        io.github.moxisuki.blockprint.cat.R.string.bp_default_painting_name,
                        paintings.size + 1,
                    )
                    viewModel.newPainting(name)
                }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.bp_list_new))
                }
            }
            if (paintings.isEmpty()) {
                Text(
                    stringResource(R.string.bp_list_empty),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(paintings, key = { it.id }) { p ->
                        PaintingListItem(
                            painting = p,
                            isCurrent = p.id == currentId,
                            onSelect = { viewModel.switchPainting(p.id) },
                            onRename = { newName -> viewModel.renamePainting(p.id, newName) },
                            onDelete = { viewModel.deletePainting(p.id) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PaintingListItem(
    painting: Painting,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showRename by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .background(containerColor)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                painting.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                "${painting.width} × ${painting.height}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isCurrent) {
            Icon(
                Icons.Sharp.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = null)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bp_list_rename)) },
                    onClick = {
                        showMenu = false
                        showRename = true
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bp_list_delete)) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                )
            }
        }
    }

    if (showRename) {
        var newName by remember(painting.id) { mutableStateOf(painting.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text(stringResource(R.string.bp_rename_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text(stringResource(R.string.bp_rename_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(newName)
                    showRename = false
                }) { Text(stringResource(R.string.bp_rename_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) {
                    Text(stringResource(R.string.bp_rename_cancel))
                }
            },
        )
    }
}

@Composable
private fun BrushSwatch(entry: BlockEntry?, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        if (entry != null) {
            io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.components.BlockPreview(entry.drawableResId, false, Modifier.size(36.dp), withBorder = false)
        } else {
            Icon(Icons.Outlined.Brush, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun BlockPaintBody(modifier: Modifier, state: BlockPaintState, context: Context, scale: Float, panOffset: Offset, onTransform: (Float, Offset) -> Unit, onPaint: (Int, Int) -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        val d = LocalDensity.current
        val vw = with(d) { maxWidth.toPx() }; val vh = with(d) { maxHeight.toPx() }
        val raw = min(vw / state.width, vh / state.height).coerceAtLeast(1f)
        val csi = raw.toInt().coerceAtLeast(1)
        val base = csi.toFloat()
        val cox = ((vw.toInt() - csi * state.width) / 2).toFloat()
        val coy = ((vh.toInt() - csi * state.height) / 2).toFloat()
        BlockCanvas(state.width, state.height, state.grid, context, scale, panOffset, base, cox, coy, onPaint, onTransform, Modifier.fillMaxSize())
    }
}

@Composable
private fun BlockPaintStatusRow(width: Int, height: Int, scale: Float, paintedCount: Int, onSetSize: (Int, Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 6.dp), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
        CompactSzStepper(stringResource(R.string.bp_width), width, BlockPaintState.MIN_SIZE, BlockPaintState.MAX_SIZE) { onSetSize(it, height) }
        CompactSzStepper(stringResource(R.string.bp_height), height, BlockPaintState.MIN_SIZE, BlockPaintState.MAX_SIZE) { onSetSize(width, it) }
        Spacer(Modifier.weight(1f))
        Text("%.1fx".format(scale), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (paintedCount > 0) ResultCountPill(paintedCount)
    }
}

@Composable
private fun CompactSzStepper(label: String, value: Int, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = { onValueChange((value - 1).coerceAtLeast(min)) }, modifier = Modifier.size(28.dp)) { Text("-", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface) }
        Text(value.toString(), modifier = Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        IconButton(onClick = { onValueChange((value + 1).coerceAtMost(max)) }, modifier = Modifier.size(28.dp)) { Text("+", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface) }
    }
}

@Composable
private fun ResultCountPill(count: Int) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(stringResource(R.string.bp_painted_count, count), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
internal fun BlockCanvas(width: Int, height: Int, grid: Array<Array<String?>>, context: Context, scale: Float, pan: Offset, base: Float, cox: Float, coy: Float, onPaint: (Int, Int) -> Unit, onTransform: (Float, Offset) -> Unit, modifier: Modifier) {
    val cs = rememberUpdatedState(scale); val cp = rememberUpdatedState(pan)
    val cb = rememberUpdatedState(base); val ccx = rememberUpdatedState(cox); val ccy = rememberUpdatedState(coy)
    val cPaint = rememberUpdatedState(onPaint); val cXform = rememberUpdatedState(onTransform)
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).pointerInput(width, height) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var lp: Offset? = null
            var lc: Offset? = null; var ld: Float? = null
            while (true) {
                val event = awaitPointerEvent(); val a = event.changes.filter { it.pressed }
                if (a.isEmpty()) break
                if (a.size >= 2) {
                    val p1 = a[0].position; val p2 = a[1].position
                    val ct = (p1 + p2) / 2f; val d = (p1 - p2).getDistance()
                    if (lc != null && ld != null && ld!! > 0f) {
                        val sd = d / ld!!; val ns = (cs.value * sd).coerceIn(MIN_SCALE, MAX_SCALE)
                        val az = ns / cs.value; val co = lc!!; val cn = ct
                        cXform.value(ns, (cp.value - co) * az + co + (cn - co))
                    }
                    lc = ct; ld = d; lp = null
                } else {
                    if (lc != null) { lc = null; ld = null; lp = null }
                    else {
                        val pos = a[0].position
                        if (lp == null || pos != lp) {
                            val (cx, cy) = cellHit(pos, width, height, cb.value, ccx.value, ccy.value, cs.value, cp.value)
                            if (cx in 0 until width && cy in 0 until height) { cPaint.value(cx, cy); a[0].consume() }
                            lp = pos
                        }
                    }
                }
            }
        }
    }.graphicsLayer { scaleX = scale; scaleY = scale; translationX = pan.x; translationY = pan.y }) {
        val emptyColor = MaterialTheme.colorScheme.surfaceVariant
        val gridColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(Modifier.fillMaxSize()) {
            for (x in 0 until width) for (y in 0 until height) {
                val bid = grid[x][y] ?: continue
                val e = BlockCatalog.all.firstOrNull { it.id == bid } ?: continue
                val bm = getOrWarmPixelArt(context, e.drawableResId)
                val sz = base.toInt().coerceAtLeast(1)
                drawImage(
                    image = bm,
                    dstOffset = IntOffset((cox + x * base).toInt(), (coy + y * base).toInt()),
                    dstSize = IntSize(sz, sz),
                )
            }
            for (x in 0 until width) for (y in 0 until height) if (grid[x][y] == null) drawRect(emptyColor, Offset(cox + x * base, coy + y * base), androidx.compose.ui.geometry.Size(base, base))
            for (i in 0..width) { val x = cox + i * base; drawLine(gridColor, Offset(x, coy), Offset(x, coy + base * height), 0.5f) }
            for (j in 0..height) { val y = coy + j * base; drawLine(gridColor, Offset(cox, y), Offset(cox + base * width, y), 0.5f) }
        }
    }
}

// TODO: zoom+pan hit-test 在缩放后仍有偏移，需校准 graphicsLayer 逆变换与 cellSize 整数化的交互
private fun cellHit(pos: Offset, gw: Int, gh: Int, bc: Float, cox: Float, coy: Float, s: Float, po: Offset): Pair<Int, Int> {
    if (bc <= 0f || s <= 0f) return 0 to 0
    val si = 1f / s
    // canvas 坐标 = 经过逆变换（去 pan、除 scale）后的像素位置
    val canvasX = (pos.x - po.x) * si
    val canvasY = (pos.y - po.y) * si
    // 格坐标：先算浮点，再用 kotlin.math.floor 取整
    val fx = (canvasX - cox) / bc
    val fy = (canvasY - coy) / bc
    val cx = kotlin.math.floor(fx.toDouble()).toInt()
    val cy = kotlin.math.floor(fy.toDouble()).toInt()
    return cx to cy
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
