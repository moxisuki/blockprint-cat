package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao
import io.github.moxisuki.blockprint.cat.data.vanilla.ModAssetStatusDao
import io.github.moxisuki.blockprint.cat.data.vanilla.VanillaAssetStatusDao
import kotlinx.coroutines.launch

@Composable
fun CacheManagementDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, CacheEntryPoint::class.java)
    }
    val blueprintManager = entryPoint.blueprintManager()
    val glbCacheDao = entryPoint.glbCacheDao()
    val vanillaAssetStatusDao = entryPoint.vanillaAssetStatusDao()
    val modAssetStatusDao = entryPoint.modAssetStatusDao()
    val scope = rememberCoroutineScope()
    var roomSize by remember { mutableStateOf(-1L) }
    var roomCount by remember { mutableStateOf(0) }
    var glbSize by remember { mutableStateOf(-1L) }
    var glbCount by remember { mutableStateOf(0) }
    var clearingRoom by remember { mutableStateOf(false) }
    var clearingGlb by remember { mutableStateOf(false) }
    var confirmRoom by remember { mutableStateOf(false) }
    var confirmGlb by remember { mutableStateOf(false) }
    var renderAssetsSize by remember { mutableStateOf(-1L) }
    var clearingRender by remember { mutableStateOf(false) }
    var confirmRender by remember { mutableStateOf(false) }

    val glbCache by glbCacheDao.observeAll().collectAsState(initial = emptyList())
    LaunchedEffect(glbCache) {
        glbCount = glbCache.size
        glbSize = glbCache.sumOf { it.sizeBytes }
    }

    LaunchedEffect(Unit) {
        val dbPath = context.getDatabasePath("litematic.db")
        roomSize = if (dbPath.isFile) dbPath.length() else 0L
        roomCount = blueprintManager.blueprintCount.value
        val renderDir = java.io.File(context.filesDir, "blockprintcat/render_assets")
        renderAssetsSize = if (renderDir.isDirectory) renderDir.walkTopDown().sumOf { if (it.isFile) it.length() else 0L } else 0L
    }

    fun doClearRoom() {
        clearingRoom = true
        scope.launch {
            runCatching { blueprintManager.clearAllBlueprints() }
            val dbPath = context.getDatabasePath("litematic.db")
            roomSize = if (dbPath.isFile) dbPath.length() else 0L
            roomCount = 0
            clearingRoom = false; confirmRoom = false
        }
    }

    fun doClearGlb() {
        clearingGlb = true
        scope.launch {
            runCatching {
                val dir = java.io.File(context.filesDir, "glb_cache")
                dir.listFiles()?.forEach { it.delete() }
                io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager.generator?.clearAllCache()
                io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager.clearAllGlb()
                glbSize = 0L; glbCount = 0
            }
            clearingGlb = false; confirmGlb = false
        }
    }

    fun doClearRender() {
        clearingRender = true
        scope.launch {
            runCatching {
                val dir = java.io.File(context.filesDir, "blockprintcat/render_assets")
                if (dir.isDirectory) dir.deleteRecursively()
                vanillaAssetStatusDao.clear()
                modAssetStatusDao.clearAll()
                renderAssetsSize = 0L
            }
            clearingRender = false; confirmRender = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_cache_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                cacheCard(
                    title = stringResource(R.string.cache_room_title),
                    stats = if (roomSize >= 0) stringResource(R.string.cache_room_stats, roomCount, formatSize(roomSize)) else stringResource(R.string.cache_calculating),
                    showConfirm = confirmRoom,
                    confirmMsg = stringResource(R.string.cache_room_confirm_msg),
                    enabled = roomCount > 0 && !clearingRoom,
                    clearing = clearingRoom,
                    onClear = { confirmRoom = true },
                    onConfirm = { doClearRoom() },
                    onCancel = { confirmRoom = false },
                )
                Spacer(Modifier.height(8.dp))

                cacheCard(
                    title = stringResource(R.string.cache_glb_title),
                    stats = if (glbSize >= 0) stringResource(R.string.cache_glb_stats, glbCount, formatSize(glbSize)) else stringResource(R.string.cache_calculating),
                    showConfirm = confirmGlb,
                    confirmMsg = stringResource(R.string.cache_glb_confirm_msg),
                    enabled = glbSize > 0 && !clearingGlb,
                    clearing = clearingGlb,
                    onClear = { confirmGlb = true },
                    onConfirm = { doClearGlb() },
                    onCancel = { confirmGlb = false },
                )
                Spacer(Modifier.height(8.dp))

                cacheCard(
                    title = stringResource(R.string.cache_render_title),
                    stats = if (renderAssetsSize >= 0) stringResource(R.string.cache_render_stats, formatSize(renderAssetsSize)) else stringResource(R.string.cache_calculating),
                    showConfirm = confirmRender,
                    confirmMsg = stringResource(R.string.cache_render_confirm_msg),
                    enabled = renderAssetsSize > 0 && !clearingRender,
                    clearing = clearingRender,
                    onClear = { confirmRender = true },
                    onConfirm = { doClearRender() },
                    onCancel = { confirmRender = false },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.cache_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun cacheCard(
    title: String,
    stats: String,
    showConfirm: Boolean,
    confirmMsg: String,
    enabled: Boolean,
    clearing: Boolean,
    onClear: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (!showConfirm) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column {
                        Text(title, style = MaterialTheme.typography.bodyMedium)
                        Text(stats, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onClear, enabled = enabled) { Text(stringResource(R.string.action_clear)) }
                }
            } else {
                Text(confirmMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onConfirm, enabled = !clearing) {
                        Text(
                            if (clearing) stringResource(R.string.cache_clearing) else stringResource(R.string.action_confirm_clear),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CacheEntryPoint {
    fun blueprintManager(): BlueprintManager
    fun glbCacheDao(): GlbCacheDao
    fun vanillaAssetStatusDao(): VanillaAssetStatusDao
    fun modAssetStatusDao(): ModAssetStatusDao
}
