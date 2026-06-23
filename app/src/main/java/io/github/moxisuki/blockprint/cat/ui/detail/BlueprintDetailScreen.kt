package io.github.moxisuki.blockprint.cat.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.data.blueprint.FullBlueprint
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import io.github.moxisuki.blockprint.cat.ui.render.GlbResourceManager
import io.github.moxisuki.blockprint.core.MinecraftVersions
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import io.github.moxisuki.blockprint.cat.data.IconIndexResolver
import io.github.moxisuki.blockprint.cat.glb.GlbGenerator
import io.github.moxisuki.blockprint.cat.data.vanilla.LangManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun BlueprintDetailScreen(
    uuid: String,
    navController: NavController,
    onTitleChange: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: DetailViewModel = hiltViewModel(),
    bridgeViewModel: BridgeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    // 转换 dialog 的 state（与 Pad 端 BlueprintDetailContent 独立但实现一致）
    var showConvertDialog by remember { mutableStateOf(false) }
    var convertSelected by remember { mutableIntStateOf(0) }
    val currentFormat = uiState.fullBlueprint?.meta?.format
        ?: io.github.moxisuki.blockprint.core.SchematicFormat.Unknown
    val openConvertDialog = {
        val currentTargetIndex = when (currentFormat) {
            io.github.moxisuki.blockprint.core.SchematicFormat.Litematica -> 0
            io.github.moxisuki.blockprint.core.SchematicFormat.Sponge -> 1
            io.github.moxisuki.blockprint.core.SchematicFormat.Structure,
            io.github.moxisuki.blockprint.core.SchematicFormat.PartialNbt -> 3
            else -> -1
        }
        convertSelected = (0..3).firstOrNull { it != currentTargetIndex } ?: 0
        showConvertDialog = true
    }
    val convertRunning by bridgeViewModel.convertInFlight.collectAsState()
    val runConvert = {
        val (target, ext) = when (convertSelected) {
            0 -> io.github.moxisuki.blockprint.core.SchematicFormat.Litematica to "litematic"
            1 -> io.github.moxisuki.blockprint.core.SchematicFormat.Sponge to "schem"
            2 -> io.github.moxisuki.blockprint.core.SchematicFormat.Sponge to "schematic"
            3 -> io.github.moxisuki.blockprint.core.SchematicFormat.Structure to "nbt"
            else -> error("Unexpected convert index: $convertSelected")
        }
        showConvertDialog = false
        val targetUuid = uiState.fullBlueprint?.meta?.uuid
        if (targetUuid != null) {
            bridgeViewModel.convertBlueprint(targetUuid, target, ext)
        }
        Unit
    }

    LaunchedEffect(uuid) {
        viewModel.load(uuid)
    }

    // 同步标题到外层 TopAppBar
    LaunchedEffect(uiState.fullBlueprint?.meta?.displayName) {
        onTitleChange(uiState.fullBlueprint?.meta?.displayName ?: "蓝图详情")
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    when {
        uiState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        uiState.fullBlueprint == null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.error ?: "加载失败",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        else -> {
            val bp = uiState.fullBlueprint!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 预览按钮 — 含资源检查 + 进度对话框，详见 PreviewButton
                item { PreviewButton(bp = bp, navController = navController, viewModel = viewModel, uiState = uiState) }

                // 基本信息 Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.detail_meta_title), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailRow(stringResource(R.string.detail_meta_name), bp.meta.displayName)
                            DetailRow(stringResource(R.string.detail_meta_author), bp.meta.author.ifEmpty { stringResource(R.string.detail_meta_unknown) })
                            DetailRow(stringResource(R.string.detail_meta_mc_version), bp.raw?.minecraftDataVersion?.let { MinecraftVersions[it] } ?: stringResource(R.string.detail_meta_unknown))
                            DetailRow(stringResource(R.string.detail_meta_format_version), bp.raw?.version?.toString() ?: stringResource(R.string.detail_meta_unknown))
                            DetailRow(stringResource(R.string.detail_meta_region_count), bp.meta.regionCount.toString())
                            DetailRow(stringResource(R.string.detail_meta_block_count), formatNumber(bp.meta.blockCount))
                            FormatRow(
                                label = stringResource(R.string.detail_meta_format),
                                value = formatDisplayName(bp.meta.format),
                                actionContentDescription = stringResource(R.string.detail_convert_action),
                                enabled = !convertRunning,
                                onActionClick = openConvertDialog,
                            )
                        }
                    }
                }

                if (convertRunning) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                }

                // 已生成 Card — 当 raw 被释放后展示，提示用户预览可用
                if (bp.raw == null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.detail_generated_message),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.detail_regenerate_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }

                // 资源包状态
                item { NamespaceCard(bp = bp, onNavigate = { ns -> navController.navigate(NavRoutes.renderWithMod(ns)) }) }

                // 区域列表
                bp.raw?.regions?.takeIf { it.isNotEmpty() }?.let { regions ->
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.detail_region_list), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                    items(regions, key = { it.name }, contentType = { "region" }) { region ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text(region.name.ifEmpty { stringResource(R.string.detail_region_unnamed) }) },
                                supportingContent = {
                                    Text("${region.width} × ${region.height} × ${region.depth}")
                                },
                                trailingContent = {
                                    Text(formatNumber(region.width * region.height * region.depth))
                                },
                                leadingContent = {
                                    Icon(Icons.Default.ViewInAr, contentDescription = null)
                                },
                            )
                        }
                    }
                }

                // 材料统计 Card 标题
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource(R.string.detail_material_top10), style = MaterialTheme.typography.titleMedium)
                            if (bp.materials.isEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "无材料数据",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (bp.materials.isNotEmpty()) {
                    items(bp.materials) { (name, count) ->
                        MaterialRow(name = name, count = count)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    ConvertDialog(
        visible = showConvertDialog,
        currentFormat = currentFormat,
        selected = convertSelected,
        onSelectedChange = { convertSelected = it },
        onDismiss = { showConvertDialog = false },
        onConfirm = runConvert,
        running = convertRunning,
        confirmEnabled = !convertRunning,
    )
}

/** 预览按钮 — 含资源检查 + 大蓝图确认 + 进度对话框。手机/Pad 共用。 */
@Composable
private fun PreviewButton(
    bp: FullBlueprint,
    navController: NavController,
    viewModel: DetailViewModel,
    uiState: DetailUiState,
) {
    val ctx = LocalContext.current
    val generator = remember { GlbResourceManager.generator }
    val assetsDir = remember { java.io.File(ctx.filesDir, "blockprintcat/render_assets") }

    val requiredNs = remember(bp.raw) {
        val ns = mutableSetOf<String>()
        bp.raw?.regions?.forEach { reg ->
            reg.palette.entries.forEach { blk -> ns.add(blk.name.substringBefore(':')) }
        }
        ns.toList()
    }
    val missing = remember(requiredNs) {
        requiredNs.filter { ns -> val d = java.io.File(assetsDir, ns); !d.isDirectory || (d.listFiles()?.isEmpty() == true) }
    }
    val missingMinecraft = "minecraft" in missing
    val missingMods = missing - "minecraft"

    var showConfirm by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showWarnDialog by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var genProgress by remember { mutableStateOf(0f) }
    var genElapsed by remember { mutableStateOf(0L) }
    var genStage by remember { mutableStateOf("") }
    fun stageName(frac: Float): String = when {
        frac < 0.07f -> ctx.getString(R.string.preview_stage_region)
        frac < 0.22f -> ctx.getString(R.string.preview_stage_texture)
        frac < 0.32f -> ctx.getString(R.string.preview_stage_atlas)
        frac < 0.67f -> ctx.getString(R.string.preview_stage_pass1)
        frac < 0.95f -> ctx.getString(R.string.preview_stage_pass2)
        else -> ctx.getString(R.string.preview_stage_finalize)
    }

    // 缓存状态：订阅 GlbResourceManager.cachedKeys（持久化、跨进程），UI 自动响应
    val cachedKeys by GlbResourceManager.cachedKeys.collectAsState()
    val hasCache = bp.meta.uuid in cachedKeys

    val startGenerate = {
        GlbResourceManager.clearGlb(bp.meta.uuid)
        generator?.clearCache(bp.meta.uuid)
        if (bp.meta.blockCount > 70000) showConfirm = true
        else if (missingMinecraft) showBlockDialog = true
        else if (missingMods.isNotEmpty()) showWarnDialog = true
        else showDialog = true
    }

    val viewExisting = {
        navController.navigate(NavRoutes.previewRoute(bp.meta.uuid))
    }

    if (hasCache) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(
                onClick = viewExisting,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.detail_view_cached))
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalIconButton(
                onClick = startGenerate,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.detail_regenerate), modifier = Modifier.size(20.dp))
            }
        }
    } else {
        FilledTonalButton(
            onClick = startGenerate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.detail_generate))
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.detail_perf_title)) },
            text = { Text(stringResource(R.string.detail_perf_msg, bp.meta.blockCount)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    if (missingMinecraft) showBlockDialog = true
                    else if (missingMods.isNotEmpty()) showWarnDialog = true
                    else showDialog = true
                }) { Text(stringResource(R.string.action_continue)) }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(stringResource(R.string.detail_no_vanilla_title)) },
            text = { Text(stringResource(R.string.detail_no_vanilla_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showBlockDialog = false
                    navController.navigate(NavRoutes.RENDER)
                }) { Text(stringResource(R.string.action_goto_download)) }
            },
            dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (showWarnDialog) {
        AlertDialog(
            onDismissRequest = { showWarnDialog = false },
            title = { Text(stringResource(R.string.detail_no_mod_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.detail_no_mod_msg_lead))
                    Spacer(Modifier.height(8.dp))
                    missingMods.forEach { ns -> Text(stringResource(R.string.detail_no_mod_bullet, ns), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.detail_no_mod_msg_trail), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { showWarnDialog = false; showDialog = true }) { Text(stringResource(R.string.detail_continue_anyway)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { showWarnDialog = false; navController.navigate(NavRoutes.RENDER) }) { Text(stringResource(R.string.action_goto_download)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { showWarnDialog = false }) { Text(stringResource(R.string.action_cancel)) }
                }
            },
        )
    }

    if (showDialog) {
        BackHandler { }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.detail_generating_title)) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { genProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${(genProgress * 100).toInt()}% — ${genElapsed / 1000}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (genStage.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            genStage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {},
        )
        LaunchedEffect(Unit) {
            val t0 = System.currentTimeMillis()
            try {
                // If raw was released after a previous generation, reload it before regenerating.
                val lit = bp.raw ?: run {
                    viewModel.load(bp.meta.uuid)
                    // Await load completion via the StateFlow — no polling.
                    // .map().filterNotNull().first() suspends until the StateFlow
                    // emits a non-null raw value, then returns it. 5 s safety timeout
                    // so a missing/disk-failure doesn't hang the LaunchedEffect forever.
                    kotlinx.coroutines.withTimeoutOrNull(5_000) {
                        viewModel.uiState
                            .map { it.fullBlueprint?.raw }
                            .filterNotNull()
                            .first()
                    } ?: throw IllegalStateException("加载超时，litematic 文件可能已损坏或被删除")
                }
                val region = lit.regions.getOrNull(0)
                val modelMinY = region?.let { it.position.y - it.height / 2 }?.toFloat() ?: 0f
                val modelCX = region?.position?.x?.toFloat() ?: 0f
                val modelCZ = region?.position?.z?.toFloat() ?: 0f
                val cacheFile = withContext(Dispatchers.IO) {
                    generator?.getOrGenerateFile(
                        lit,
                        GlbGenerator.Key(blueprintUuid = bp.meta.uuid),
                    ) { p ->
                        genProgress = p
                        genElapsed = System.currentTimeMillis() - t0
                        genStage = stageName(p)
                    }
                } ?: throw IllegalStateException("渲染引擎未初始化")
                GlbResourceManager.putGlb(bp.meta.uuid, cacheFile, modelMinY, modelCX, modelCZ)
                // Drop the Litematic from ViewModel state — frees memory before
                // Preview opens, avoiding post-generation lag.
                viewModel.releaseLitematic()
                // Workaround for blockprint-core Pass 2 not releasing its
                // OffHeapBuf ByteArray segments promptly: force GC BEFORE
                // navigating to Preview so Filament init + texture upload
                // don't compete with collection of tens of MB of stale
                // generation buffers.
                System.gc()
                showDialog = false
                navController.navigate(NavRoutes.previewRoute(bp.meta.uuid))
            } catch (_: Exception) {
                showDialog = false
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

/**
 * 信息行 — label + value + 右侧 FilledTonalIconButton。用于 basic info Card 的"格式"行。
 * 按钮点击触发转换 dialog（state 在调用方持有）。
 */
@Composable
private fun FormatRow(
    label: String,
    value: String,
    actionContentDescription: String,
    enabled: Boolean = true,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 12.dp, end = 8.dp),
            )
            FilledTonalIconButton(
                onClick = onActionClick,
                enabled = enabled,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = actionContentDescription,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (!enabled) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

/**
 * 格式转换对话框 — 4 个目标格式的单选。Show/hide + 选中项在调用方持有。
 * Confirm 之后目前是 no-op，真正的转换逻辑后续接 blockprint-core 的 writer。
 */
@Composable
private fun ConvertDialog(
    visible: Boolean,
    currentFormat: io.github.moxisuki.blockprint.core.SchematicFormat,
    selected: Int,
    onSelectedChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    running: Boolean = false,
    confirmEnabled: Boolean = true,
) {
    if (!visible) return
    val currentTargetIndex = when (currentFormat) {
        io.github.moxisuki.blockprint.core.SchematicFormat.Litematica -> 0
        io.github.moxisuki.blockprint.core.SchematicFormat.Sponge -> 1
        io.github.moxisuki.blockprint.core.SchematicFormat.Structure,
        io.github.moxisuki.blockprint.core.SchematicFormat.PartialNbt -> 3
        else -> -1
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_convert_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.detail_convert_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (currentTargetIndex != 0) {
                    ConvertTargetRow(
                        label = stringResource(R.string.detail_convert_target_litematic),
                        selected = selected == 0,
                        onClick = { onSelectedChange(0) },
                    )
                }
                if (currentTargetIndex != 1) {
                    ConvertTargetRow(
                        label = stringResource(R.string.detail_convert_target_schem),
                        selected = selected == 1,
                        onClick = { onSelectedChange(1) },
                    )
                }
                if (currentTargetIndex != 2) {
                    ConvertTargetRow(
                        label = stringResource(R.string.detail_convert_target_schematic),
                        selected = selected == 2,
                        onClick = { onSelectedChange(2) },
                    )
                }
                if (currentTargetIndex != 3) {
                    ConvertTargetRow(
                        label = stringResource(R.string.detail_convert_target_nbt),
                        selected = selected == 3,
                        onClick = { onSelectedChange(3) },
                    )
                }
                if (running) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.detail_convert_running),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(stringResource(R.string.action_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ConvertTargetRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
            androidx.compose.material3.HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            content()
        }
    }
}

@Composable
private fun RegionRow(region: io.github.moxisuki.blockprint.core.LitematicRegion) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ViewInAr,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                region.name.ifEmpty { stringResource(R.string.detail_region_unnamed) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${region.width} × ${region.height} × ${region.depth}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            formatNumber(region.width * region.height * region.depth),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 根据材质名生成稳定颜色 */
private fun materialColor(name: String): Color {
    val hue = (name.hashCode() and 0x7FFFFFFF) % 360
    return Color.hsl(hue.toFloat(), 0.45f, 0.55f)
}

/** SchematicFormat → 本地化显示文本。 */
@Composable
private fun formatDisplayName(format: io.github.moxisuki.blockprint.core.SchematicFormat): String =
    when (format) {
        io.github.moxisuki.blockprint.core.SchematicFormat.Litematica -> stringResource(R.string.detail_convert_target_litematic)
        io.github.moxisuki.blockprint.core.SchematicFormat.Sponge -> stringResource(R.string.detail_convert_target_schem)
        io.github.moxisuki.blockprint.core.SchematicFormat.Structure,
        io.github.moxisuki.blockprint.core.SchematicFormat.PartialNbt -> stringResource(R.string.detail_format_nbt)
        io.github.moxisuki.blockprint.core.SchematicFormat.BuildingHelper -> stringResource(R.string.detail_format_building_helper)
        io.github.moxisuki.blockprint.core.SchematicFormat.Unknown -> stringResource(R.string.detail_meta_unknown)
    }

@Composable
private fun MaterialRow(name: String, count: Int) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DetailScreenEntryPoint::class.java
        )
    }
    val iconIndexResolver = entryPoint.iconIndexResolver()
    LaunchedEffect(Unit) {
        iconIndexResolver.ensureLoaded()
    }
    val langName = remember(name) { LangManager.displayName(context, name) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MaterialIcon(name = name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                langName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "× ${formatNumber(count)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MaterialIcon(name: String) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DetailScreenEntryPoint::class.java
        )
    }
    val iconIndexResolver = entryPoint.iconIndexResolver()
    val indexReady by iconIndexResolver.ready.collectAsState()
    val variants = remember(name, indexReady) {
        listOfNotNull(
            iconIndexResolver.getIconUrl(name),
            iconIndexResolver.getIconUrl(name, "_block"),
            iconIndexResolver.getIconUrl(name, "_item"),
        )
    }
    var attempt by remember { mutableIntStateOf(0) }
    val currentUrl = variants.getOrNull(attempt)

    if (currentUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(currentUrl)
                .crossfade(true)
                .build(),
            contentDescription = name,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)),
            error = {
                if (attempt < variants.lastIndex) {
                    LaunchedEffect(currentUrl) { attempt++ }
                } else {
                    UnknownIcon()
                }
            },
        )
    } else {
        val displayName = name.removePrefix("minecraft:")
        if (name.startsWith("minecraft:")) {
            val bg = materialColor(name)
            val label = displayName.replace("_", " ").take(2).uppercase()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        } else {
            UnknownIcon()
        }
    }
}

@Composable
private fun UnknownIcon() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
fun BlueprintDetailContent(
    uuid: String,
    navController: NavController? = null,
    snackbarHostState: SnackbarHostState,
    viewModel: DetailViewModel = hiltViewModel(),
    bridgeViewModel: BridgeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    // 转换 dialog 的 state（与手机端 BlueprintDetailScreen 独立但实现一致）
    var showConvertDialog by remember { mutableStateOf(false) }
    var convertSelected by remember { mutableIntStateOf(0) }
    val currentFormat = uiState.fullBlueprint?.meta?.format
        ?: io.github.moxisuki.blockprint.core.SchematicFormat.Unknown
    val openConvertDialog = {
        val currentTargetIndex = when (currentFormat) {
            io.github.moxisuki.blockprint.core.SchematicFormat.Litematica -> 0
            io.github.moxisuki.blockprint.core.SchematicFormat.Sponge -> 1
            io.github.moxisuki.blockprint.core.SchematicFormat.Structure,
            io.github.moxisuki.blockprint.core.SchematicFormat.PartialNbt -> 3
            else -> -1
        }
        convertSelected = (0..3).firstOrNull { it != currentTargetIndex } ?: 0
        showConvertDialog = true
    }
    val convertRunning by bridgeViewModel.convertInFlight.collectAsState()
    val runConvert = {
        val (target, ext) = when (convertSelected) {
            0 -> io.github.moxisuki.blockprint.core.SchematicFormat.Litematica to "litematic"
            1 -> io.github.moxisuki.blockprint.core.SchematicFormat.Sponge to "schem"
            2 -> io.github.moxisuki.blockprint.core.SchematicFormat.Sponge to "schematic"
            3 -> io.github.moxisuki.blockprint.core.SchematicFormat.Structure to "nbt"
            else -> error("Unexpected convert index: $convertSelected")
        }
        showConvertDialog = false
        val targetUuid = uiState.fullBlueprint?.meta?.uuid
        if (targetUuid != null) {
            bridgeViewModel.convertBlueprint(targetUuid, target, ext)
        }
        Unit
    }

    LaunchedEffect(uuid) { viewModel.load(uuid) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.fullBlueprint == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(uiState.error ?: "加载失败", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        }
        else -> {
            val bp = uiState.fullBlueprint!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (navController != null) {
                    val nc = navController
                    item { PreviewButton(bp = bp, navController = nc, viewModel = viewModel, uiState = uiState) }
                }
                item {
                    SectionCard(title = stringResource(R.string.detail_meta_title)) {
                        DetailRow(stringResource(R.string.detail_meta_name), bp.meta.displayName)
                        DetailRow(stringResource(R.string.detail_meta_author), bp.meta.author.ifEmpty { stringResource(R.string.detail_meta_unknown) })
                        DetailRow(stringResource(R.string.detail_meta_mc_version), bp.raw?.minecraftDataVersion?.let { MinecraftVersions[it] } ?: stringResource(R.string.detail_meta_unknown))
                        DetailRow(stringResource(R.string.detail_meta_format_version), bp.raw?.version?.toString() ?: stringResource(R.string.detail_meta_unknown))
                        DetailRow(stringResource(R.string.detail_meta_region_count), bp.meta.regionCount.toString())
                        DetailRow(stringResource(R.string.detail_meta_block_count), formatNumber(bp.meta.blockCount))
                        FormatRow(
                            label = stringResource(R.string.detail_meta_format),
                            value = formatDisplayName(bp.meta.format),
                            actionContentDescription = stringResource(R.string.detail_convert_action),
                            enabled = !convertRunning,
                            onActionClick = openConvertDialog,
                        )
                    }
                }
                if (convertRunning) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                }
                // 已生成 Card — 当 raw 被释放后展示
                if (bp.raw == null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.detail_generated_message),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.detail_regenerate_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
                // 资源包状态（与手机端复用 NamespaceCard）
                item { NamespaceCard(bp = bp, onNavigate = { ns -> navController?.navigate(NavRoutes.renderWithMod(ns)) }) }
                bp.raw?.regions?.takeIf { it.isNotEmpty() }?.let { regions ->
                    item {
                        SectionCard(title = stringResource(R.string.detail_region_list)) {
                            regions.forEachIndexed { index, region ->
                                if (index > 0) androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                )
                                RegionRow(region)
                            }
                        }
                    }
                }
                item {
                    SectionCard(title = stringResource(R.string.detail_material_top10)) {
                        if (bp.materials.isEmpty()) {
                            Text(
                                stringResource(R.string.detail_material_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
                if (bp.materials.isNotEmpty()) {
                    items(bp.materials, key = { (it as Pair).first }, contentType = { "material" }) { (name, count) -> MaterialRow(name = name, count = count) }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    ConvertDialog(
        visible = showConvertDialog,
        currentFormat = currentFormat,
        selected = convertSelected,
        onSelectedChange = { convertSelected = it },
        onDismiss = { showConvertDialog = false },
        onConfirm = runConvert,
        running = convertRunning,
        confirmEnabled = !convertRunning,
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DetailScreenEntryPoint {
    fun iconIndexResolver(): IconIndexResolver
}
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RenderPreviewEntryPoint {
    fun vanillaAssetDownloader(): io.github.moxisuki.blockprint.cat.data.vanilla.VanillaAssetDownloader
    fun modAssetManager(): io.github.moxisuki.blockprint.cat.data.vanilla.ModAssetManager
}
