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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeViewModel
import io.github.moxisuki.blockprint.cat.ui.format.FormatCatalog
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import io.github.moxisuki.blockprint.core.MinecraftVersions

/**
 * Pad-layout variant of the blueprint detail screen, used as the right
 * pane of the two-pane master-detail composition in `AppNavGraph.PadLayout`.
 *
 * Differences from the phone `BlueprintDetailScreen`:
 *   - No top-level title sync (the outer AppTopBar shows the title)
 *   - `navController` is optional (PadLayout supplies one, but a future
 *     preview-only embedding may not)
 *   - Region list rendered as one [SectionCard] with `forEachIndexed` rows
 *     (vs the phone's `items(regions, key = { it.name })` LazyColumn path)
 *   - Material list keeps the `key = { (it as Pair).first }` LazyColumn
 *     path so the per-row recomposition stays skippable
 *
 * Convert-dialog state is owned here (independent from the phone variant)
 * because the user can open it from the embedded FormatRow.
 */
@Composable
fun BlueprintDetailContent(
    uuid: String,
    navController: NavController? = null,
    snackbarHostState: SnackbarHostState,
    viewModel: DetailViewModel = hiltViewModel(),
    bridgeViewModel: BridgeViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    // Hoist the IconIndexResolver once per screen — every MaterialRow shares it.
    val iconIndexResolver = rememberIconIndexResolver()

    // 转换 dialog 的 state（与手机端 BlueprintDetailScreen 独立但实现一致）
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
                                if (index > 0) HorizontalDivider(
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
                    items(bp.materials, key = { (it as Pair).first }, contentType = { "material" }) { (name, count) -> MaterialRow(name = name, count = count, iconIndexResolver = iconIndexResolver) }
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
