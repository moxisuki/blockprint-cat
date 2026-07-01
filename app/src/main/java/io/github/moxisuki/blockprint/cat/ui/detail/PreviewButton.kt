package io.github.moxisuki.blockprint.cat.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.FullBlueprint
import io.github.moxisuki.blockprint.cat.glb.GlbGenerator
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.ui.render.GlbResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Preview-generate button used by both phone (`BlueprintDetailScreen`) and
 * pad (`BlueprintDetailContent`) variants. The body owns:
 *   - The `View in AR` button (or `View cached` + `Regenerate` row when
 *     [GlbResourceManager] already has a cached GLB for the uuid)
 *   - The 3 sequential confirm dialogs (size / no vanilla assets / missing mod assets)
 *   - The progress dialog with stage labels and elapsed seconds
 *   - The actual generation coroutine that calls `GlbGenerator` and navigates
 *     to the preview route when done
 *
 * Why this lives in its own file: the function is ~220 lines on its own,
 * holding an `AlertDialog` chain + 5 `var` dialog flags + a heavy coroutine
 * that touches [GlbResourceManager], [GlbGenerator], and the navController.
 * Pulling it out shrinks the detail screen to just LazyColumn plumbing.
 */
@Composable
internal fun PreviewButton(
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
    var genProgress by remember { mutableFloatStateOf(0f) }
    var genElapsed by remember { mutableLongStateOf(0L) }
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
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detail_view_cached))
            }
            Spacer(Modifier.width(8.dp))
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
            Spacer(Modifier.width(8.dp))
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
                    withTimeoutOrNull(5_000) {
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
