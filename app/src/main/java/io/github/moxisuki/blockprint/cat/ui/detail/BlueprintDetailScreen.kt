package io.github.moxisuki.blockprint.cat.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeViewModel
import io.github.moxisuki.blockprint.cat.ui.format.FormatCatalog
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import io.github.moxisuki.blockprint.core.MinecraftVersions

/**
 * Phone-layout entry point for the blueprint detail screen.
 *
 * After the Task #4 split this file owns only the LazyColumn plumbing:
 *   - state for the convert dialog
 *   - top-level title sync via [onTitleChange]
 *   - error → snackbar dispatch
 *   - the loading / error / content branches
 *
 * All visual sub-composables live in sibling files (same `ui.detail` package):
 *   - [PreviewButton]                — 3D preview-generate flow + dialogs
 *   - [DetailRow] / [FormatRow]     — meta info table
 *   - [SectionCard] / [RegionRow]    — section wrappers + region list
 *   - [ConvertDialog]                — format-conversion dialog
 *   - [MaterialRow] / [MaterialIcon] / [UnknownIcon] — material list
 *   - [formatDisplayName]            — format localization helper
 *
 * The pad variant for the master-detail right pane is
 * [BlueprintDetailContent] in its own file.
 *
 * Public signature unchanged so `AppNavGraph.CompactLayout` keeps calling
 * it without changes.
 */
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
    // Hoist the IconIndexResolver once per screen — every MaterialRow shares it.
    val iconIndexResolver = rememberIconIndexResolver()

    // 转换 dialog 的 state（与 Pad 端 BlueprintDetailContent 独立但实现一致）
    var showConvertDialog by remember { mutableStateOf(false) }
    var convertSelected by remember { mutableIntStateOf(0) }
    val currentFormat = uiState.fullBlueprint?.meta?.format
        ?: io.github.moxisuki.blockprint.core.SchematicFormat.Unknown
    val convertTargets = remember(currentFormat) {
        FormatCatalog.convertTargetsExcluding(currentFormat)
    }
    val hasConvertTarget = convertTargets.isNotEmpty()
    val openConvertDialog = remember(convertTargets) {
        // BuildingHelper is currently disabled as a convert source — guard
        // here so the user never sees an empty dialog or a crash.
        {
            if (convertTargets.isNotEmpty()) {
                convertSelected = 0
                showConvertDialog = true
            }
        }
    }
    val convertRunning by bridgeViewModel.convertInFlight.collectAsState()
    val runConvert = remember(convertTargets, convertSelected, uiState.fullBlueprint?.meta?.uuid) {
        {
            val targets = convertTargets
            val display = targets.getOrNull(convertSelected)
                ?: error("convertSelected $convertSelected out of range for ${targets.size} targets")
            val target = display.schematicFormat
            val ext = display.fileExtension
            showConvertDialog = false
            val targetUuid = uiState.fullBlueprint?.meta?.uuid
            if (targetUuid != null) {
                bridgeViewModel.convertBlueprint(targetUuid, target, ext)
            }
            Unit
        }
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
                    text = uiState.error ?: stringResource(R.string.detail_load_failed),
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
                            DetailRow(stringResource(R.string.detail_meta_file_name), bp.meta.fileName)
                            DetailRow(stringResource(R.string.detail_meta_author), bp.meta.author.ifEmpty { stringResource(R.string.detail_meta_unknown) })
                            DetailRow(stringResource(R.string.detail_meta_mc_version), bp.raw?.minecraftDataVersion?.let { MinecraftVersions[it] } ?: stringResource(R.string.detail_meta_unknown))
                            DetailRow(stringResource(R.string.detail_meta_format_version), bp.raw?.version?.toString() ?: stringResource(R.string.detail_meta_unknown))
                            DetailRow(stringResource(R.string.detail_meta_region_count), bp.meta.regionCount.toString())
                            DetailRow(stringResource(R.string.detail_meta_block_count), formatNumber(bp.meta.blockCount))
                            FormatRow(
                                label = stringResource(R.string.detail_meta_format),
                                value = formatDisplayName(bp.meta.format),
                                actionContentDescription = stringResource(R.string.detail_convert_action),
                                enabled = !convertRunning && hasConvertTarget,
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
                                    stringResource(R.string.detail_no_materials),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                if (bp.materials.isNotEmpty()) {
                    items(bp.materials) { (name, count) ->
                        MaterialRow(name = name, count = count, iconIndexResolver = iconIndexResolver)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    ConvertDialog(
        visible = showConvertDialog,
        currentFormat = currentFormat,
        targets = convertTargets,
        selected = convertSelected,
        onSelectedChange = { convertSelected = it },
        onDismiss = { showConvertDialog = false },
        onConfirm = runConvert,
        confirmEnabled = !convertRunning,
    )
}

/**
 * Hilt entry point that exposes the [io.github.moxisuki.blockprint.cat.data.vanilla.VanillaAssetDownloader]
 * singleton to preview-related composables. Registered against the
 * [SingletonComponent] so it lives for the app's lifetime.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface RenderPreviewEntryPoint {
    fun vanillaAssetDownloader(): io.github.moxisuki.blockprint.cat.data.vanilla.VanillaAssetDownloader
    fun modAssetManager(): io.github.moxisuki.blockprint.cat.data.vanilla.ModAssetManager
}
