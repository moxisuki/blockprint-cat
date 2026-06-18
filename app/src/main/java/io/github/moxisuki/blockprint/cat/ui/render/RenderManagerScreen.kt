package io.github.moxisuki.blockprint.cat.ui.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.vanilla.ModAssetManager
import io.github.moxisuki.blockprint.cat.data.vanilla.ModrinthClient
import io.github.moxisuki.blockprint.cat.data.vanilla.VanillaAssetDownloader
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

private fun searchQueryFromSlug(slug: String): String =
    slug
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")        // camelCase → camel Case
        .replace(Regex("[_\\-.]"), " ")                     // separators → space
        .replace(Regex("\\s+"), " ")                        // collapse multiple spaces
        .trim()

@Composable
fun RenderManagerScreen(snackbarHostState: SnackbarHostState, initialModSlug: String = "") {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, RenderEntryPoint::class.java)
    }
    val downloader = entryPoint.vanillaAssetDownloader()
    val downloadState by downloader.downloadState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(stringResource(R.string.render_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.render_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        // 原版资源包
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ViewInAr, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.render_vanilla), style = MaterialTheme.typography.titleMedium)
                        when (val s = downloadState) {
                            is VanillaAssetDownloader.DownloadState.Idle -> Text(
                                "未安装 — 下载 Minecraft 原版纹理、模型和语言文件",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            is VanillaAssetDownloader.DownloadState.FetchingManifest -> Text(
                                "获取版本信息…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            is VanillaAssetDownloader.DownloadState.DownloadingJar -> Text(
                                "下载 client.jar ${(s.progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            is VanillaAssetDownloader.DownloadState.Extracting -> Text(
                                "解压资源 (${s.extracted} 文件)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            is VanillaAssetDownloader.DownloadState.Installed -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.render_vanilla_installed, s.version, s.fileCount, formatSize(s.totalSize)),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            is VanillaAssetDownloader.DownloadState.Error -> Text(
                                "错误: ${s.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Progress bar during JAR download
                if (downloadState is VanillaAssetDownloader.DownloadState.DownloadingJar) {
                    val ds = downloadState as VanillaAssetDownloader.DownloadState.DownloadingJar
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { ds.progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(ds.fileName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                // Current file during extraction
                if (downloadState is VanillaAssetDownloader.DownloadState.Extracting) {
                    val ds = downloadState as VanillaAssetDownloader.DownloadState.Extracting
                    Spacer(Modifier.height(4.dp))
                    Text(ds.current, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(Modifier.height(12.dp))

                when (downloadState) {
                    is VanillaAssetDownloader.DownloadState.DownloadingJar, is VanillaAssetDownloader.DownloadState.Extracting ->
                        TextButton(onClick = { downloader.cancel() }) { Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.error) }
                    is VanillaAssetDownloader.DownloadState.Idle ->
                        TextButton(onClick = { downloader.startDownload() }) { Text(stringResource(R.string.render_check_update)) }
                    is VanillaAssetDownloader.DownloadState.Error ->
                        TextButton(onClick = { downloader.startDownload() }) { Text(stringResource(R.string.render_redownload)) }
                    is VanillaAssetDownloader.DownloadState.Installed ->
                        Row {
                            TextButton(onClick = { downloader.startDownload() }) { Text(stringResource(R.string.render_redownload)) }
                            TextButton(onClick = { downloader.deleteAssets() }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                        }
                    else -> {}
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Mod 资源卡片
        ModAssetCard(entryPoint = entryPoint, initialSlug = initialModSlug)

        Spacer(Modifier.height(16.dp))
        Text(
            "原版资源从 client.jar 提取（models/blockstates/textures/lang），Mod 资源从 Modrinth 下载对应版本的 JAR 并提取 assets。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ModAssetCard(entryPoint: RenderEntryPoint, initialSlug: String = "") {
    val manager = remember { entryPoint.modAssetManager() }
    val states by manager.states.collectAsState()
    var showAdd by remember { mutableStateOf(initialSlug.isNotEmpty()) }
    var initialSlugUsed by remember { mutableStateOf(false) }
    var retrySlug by remember { mutableStateOf("") }

    // 已安装的 Mod 列表
    // Downloading first, then newest installations
    val sortedStates = states.entries.sortedByDescending { (_, s) ->
        when (s) {
            is ModAssetManager.ModState.Downloading -> Long.MAX_VALUE
            is ModAssetManager.ModState.Installed -> s.entity.installedAt
            else -> 0L
        }
    }
    sortedStates.forEach { (slug, state) ->
        if (state is ModAssetManager.ModState.Installed || state is ModAssetManager.ModState.Downloading || state is ModAssetManager.ModState.Error) {
            val info = if (state is ModAssetManager.ModState.Installed) state.entity else null
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ViewInAr, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("$slug Mod 资源", style = MaterialTheme.typography.titleMedium)
                            when (val s = state) {
                                is ModAssetManager.ModState.Installed -> Text(
                                    if (s.entity.fileCount == 0) "已安装 · 不含资源包"
                                else "已安装 · v${s.entity.versionName} · ${s.entity.fileCount} 文件 · ${formatSize(s.entity.totalSize)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                is ModAssetManager.ModState.Downloading -> Text(
                                    stringResource(R.string.render_downloading), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                is ModAssetManager.ModState.Error -> Text(
                                    s.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                else -> {}
                            }
                        }
                        when (state) {
                            is ModAssetManager.ModState.Installed -> Row {
                                TextButton(onClick = { manager.install(ModAssetManager.ModInfo(slug, slug, info!!.mcVersion)) }) { Text(stringResource(R.string.render_redownload)) }
                                TextButton(onClick = { manager.delete(slug) }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                            }
                            is ModAssetManager.ModState.Error -> TextButton(onClick = { manager.delete(slug); retrySlug = slug; showAdd = true }) { Text(stringResource(R.string.render_redownload)) }
                            is ModAssetManager.ModState.Downloading -> TextButton(onClick = {}) { Text(stringResource(R.string.render_downloading)) }
                            else -> {}
                        }
                    }
                    if (state is ModAssetManager.ModState.Downloading) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    // 添加 Mod 按钮
    TextButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
        Text("+ 添加 Mod 资源")
    }

    if (showAdd) {
        val scope = rememberCoroutineScope()
        var query by remember {
            mutableStateOf(
                when {
                    retrySlug.isNotEmpty() -> searchQueryFromSlug(retrySlug).also { retrySlug = "" }
                    !initialSlugUsed && initialSlug.isNotEmpty() -> searchQueryFromSlug(initialSlug).also { initialSlugUsed = true }
                    else -> ""
                }
            )
        }
        var searchResults by remember { mutableStateOf<List<ModrinthClient.SearchResult>?>(null) }
        var versions by remember { mutableStateOf<List<ModrinthClient.ModVersion>?>(null) }
        var selectedSlug by remember { mutableStateOf("") }
        var fetching by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAdd = false; searchResults = null; versions = null },
            title = { Text(if (versions != null) "选择版本" else if (searchResults != null) "搜索结果" else "搜索 Mod") },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; searchResults = null; versions = null },
                        label = { Text(stringResource(R.string.render_mod_search_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !fetching,
                    )
                    Spacer(Modifier.height(4.dp))

                    if (fetching) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_search) + "…", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (versions != null) {
                        Spacer(Modifier.height(8.dp))
                        val list = versions!!
                        if (list.isEmpty()) Text(stringResource(R.string.render_no_versions), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        else {
                            Text("${list.size} 个版本", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            LazyColumn(modifier = Modifier.height(200.dp)) {
                                items(list, key = { it.id }) { v ->
                                    TextButton(onClick = {
                                        manager.install(ModAssetManager.ModInfo(selectedSlug, selectedSlug, v.gameVersions.firstOrNull() ?: ""))
                                        showAdd = false; versions = null; searchResults = null
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                            Text(v.name, style = MaterialTheme.typography.bodySmall)
                                            Text("MC ${v.gameVersions.take(3).joinToString(", ")} · ${"%.1f".format(v.fileSize / 1048576.0)} MB",
                                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    } else if (searchResults != null) {
                        Spacer(Modifier.height(8.dp))
                        val list = searchResults!!
                        if (list.isEmpty()) Text(stringResource(R.string.render_no_results), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        else {
                            Text("${list.size} 个结果", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            LazyColumn(modifier = Modifier.height(240.dp)) {
                                items(list, key = { it.projectId }) { r ->
                                    TextButton(onClick = {
                                        selectedSlug = r.slug
                                        fetching = true
                                        scope.launch {
                                            try { versions = manager.fetchVersions(r.slug) }
                                            catch (e: Exception) { versions = emptyList() }
                                            fetching = false
                                        }
                                    }, modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(r.title, style = MaterialTheme.typography.bodySmall)
                                                Spacer(Modifier.width(6.dp))
                                                Text(r.projectType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                            if (r.description.isNotEmpty()) Text(r.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(stringResource(R.string.render_mod_search_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (query.isNotBlank() && !fetching) {
                        if (versions != null) { versions = null; searchResults = null }
                        else if (searchResults != null) { searchResults = null }
                        else {
                            fetching = true
                            scope.launch {
                                try { searchResults = manager.searchMods(query.trim()) }
                                catch (e: Exception) { searchResults = emptyList() }
                                fetching = false
                            }
                        }
                    }
                }) {
                    Text(when {
                        versions != null -> "返回"
                        searchResults != null -> "返回"
                        else -> "搜索 Mod"
                    })
                }
            },
            dismissButton = { TextButton(onClick = { showAdd = false; searchResults = null; versions = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RenderEntryPoint {
    fun vanillaAssetDownloader(): VanillaAssetDownloader
    fun modAssetManager(): ModAssetManager
}
