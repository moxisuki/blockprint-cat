package io.github.moxisuki.blockprint.cat.ui.community

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.ui.graphics.FilterQuality
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.data.community.CmsParsers
import io.github.moxisuki.blockprint.cat.data.community.CommunitySource
import io.github.moxisuki.blockprint.cat.data.community.UnifiedDetail
import io.github.moxisuki.blockprint.cat.data.community.UnifiedMaterial
import io.github.moxisuki.blockprint.cat.data.community.UnifiedSchematic
import io.github.moxisuki.blockprint.cat.ui.util.rememberAppErrorResolver
import io.github.moxisuki.blockprint.cat.ui.util.rememberStringResolver
import kotlinx.coroutines.launch

@Composable
fun CommunityDetailScreen(
    source: CommunitySource,
    id: String,
    navController: NavController,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    viewModel: CommunityViewModel,
) {
    val state by viewModel.state.collectAsState()
    val detail by viewModel.detail.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resolveAppError = rememberAppErrorResolver()
    val resolveString = rememberStringResolver()

    val schematic: UnifiedSchematic? = remember(state, source, id, detail) {
        val fromList = when (source) {
            CommunitySource.MCS -> state.mcs.schematics.firstOrNull { it.id == id }
            CommunitySource.CMS -> state.cms.schematics.firstOrNull { it.id == id }
        }
        fromList ?: detail.headerSnapshot
    }

    LaunchedEffect(source, id) {
        viewModel.loadDetail(source, id)
        viewModel.loadPreview(source, id)
    }

    LaunchedEffect(Unit) {
        viewModel.download.collect { event ->
            when (event) {
                is DownloadEvent.Success ->
                    snackbarHostState.showSnackbar(resolveString(R.string.cdl_downloaded_snackbar, event.schematic.name))
                is DownloadEvent.Failed ->
                    snackbarHostState.showSnackbar(resolveAppError(event.error))
                is DownloadEvent.Progress -> Unit
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
            when {
                detail.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                detail.error != null && schematic == null -> Box(
                    Modifier.fillMaxSize(), Alignment.Center,
                ) {
                    Text(resolveAppError(detail.error!!), color = MaterialTheme.colorScheme.error)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SchematicHeader(
                            schematic = schematic,
                            detail = detail,
                            downloading = state.isDownloading,
                            downloadBytes = state.downloadBytes,
                            downloadTotal = state.downloadTotal,
                            downloadingName = state.downloadingName,
                            onDownload = {
                                schematic?.let {
                                    scope.launch { viewModel.downloadSchematic(context, it) }
                                }
                            },
                        )
                    }
                    item {
                        if (detail.markdown.isNotBlank()) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.cdl_desc_label), style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(detail.markdown,
                                         style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    if (detail.requirements.isNotEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        if (detail.requirements.isEmpty()) stringResource(R.string.cdl_requirements_zero)
                                        else stringResource(R.string.cdl_requirements, detail.requirements.size),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                        items(detail.requirements) { mat -> UnifiedMaterialRow(mat) }
                    }
                    if (detail.production.isNotEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.cdl_production_count, detail.production.size),
                                         style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        items(detail.production) { mat -> UnifiedMaterialRow(mat) }
                    }
                    if (detail.dependencies.isNotEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.cdl_dependencies_label), style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(detail.dependencies.joinToString("、"),
                                         style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    if (!detail.stress.isNullOrBlank()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.cdl_stress_value, detail.stress ?: ""),
                                         style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    if (detail.comments.isNotEmpty()) {
                        item {
                            Text(stringResource(R.string.cdl_comments, detail.comments.size),
                                 style = MaterialTheme.typography.titleMedium,
                                 modifier = Modifier.padding(top = 4.dp))
                        }
                        items(detail.comments, key = { it.uuid }, contentType = { "comment" }) { c ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Default.Person, null, Modifier.size(14.dp),
                                                 tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(c.nickName,
                                             style = MaterialTheme.typography.labelMedium,
                                             color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.weight(1f))
                                        Text(c.createTime.take(10),
                                             style = MaterialTheme.typography.labelSmall,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(c.content, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }

@Composable
private fun SchematicHeader(
    schematic: UnifiedSchematic?,
    detail: UnifiedDetail,
    onDownload: () -> Unit,
    downloading: Boolean,
    downloadBytes: Long = -1L,
    downloadTotal: Long = -1L,
    downloadingName: String = "",
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            val coverUrl = detail.coverUrl ?: schematic?.coverUrl
            val cmsFallback = "https://www.creativemechanicserver.com/static/backgrounds/17.webp"
            when {
                detail.source == CommunitySource.CMS -> {
                    val url = coverUrl?.let { absolutize(it) } ?: cmsFallback
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(url).crossfade(true).build(),
                        contentDescription = "封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth().height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                    )
                    Spacer(Modifier.height(12.dp))
                }
                detail.previewBitmap != null -> {
                    Image(
                        bitmap = detail.previewBitmap,
                        contentDescription = "预览图",
                        modifier = Modifier
                            .fillMaxWidth().height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                detail.previewLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth().height(120.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            Text(text = schematic?.name ?: "(加载中...)",
                 style = MaterialTheme.typography.titleLarge,
                 fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).clip(CircleShape)
                       .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(8.dp))
                Text(schematic?.author ?: "",
                     style = MaterialTheme.typography.bodyMedium,
                     modifier = Modifier.weight(1f))
                val (icon, value) = when (detail.source) {
                    CommunitySource.MCS ->
                        Icons.Default.LocalFireDepartment to (schematic?.heat?.toString() ?: "")
                    CommunitySource.CMS ->
                        Icons.Default.CloudDownload to (schematic?.downloads?.toString() ?: "")
                }
                Icon(icon, null, Modifier.size(16.dp),
                     tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(2.dp))
                Text(value, style = MaterialTheme.typography.labelSmall)
            }
            (schematic?.size?.let { "${it.first} × ${it.second} × ${it.third}" }
              ?: schematic?.sizeText)?.let { sizeText ->
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.cdl_size_label, sizeText),
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!schematic?.tags.isNullOrEmpty()) {
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    schematic!!.tags.forEach { tag ->
                        AssistChip(
                            onClick = { },
                            label = { Text(tag.removePrefix("minecraft:"),
                                           style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            ),
                        )
                    }
                }
            }
            if (downloading) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.cdl_downloading_name, downloadingName), style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    val done = downloadTotal > 0 && downloadBytes >= downloadTotal
                    val pct = if (done) "" else if (downloadTotal > 0) "${(downloadBytes * 100 / downloadTotal).toInt()}% " else ""
                    Text(if (done) stringResource(R.string.cdl_processing) else "$pct${downloadBytes / 1024} KB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                if (downloadTotal > 0) {
                    LinearProgressIndicator(progress = { downloadBytes.toFloat() / downloadTotal }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onDownload,
                enabled = schematic != null && !downloading && detail.downloadable,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(when {
                    downloading -> stringResource(R.string.cdl_downloading)
                    !detail.downloadable -> stringResource(R.string.cdl_no_link)
                    else -> stringResource(R.string.cdl_download_label)
                })
            }
        }
    }
}

private fun absolutize(url: String): String =
    if (url.startsWith("http")) url
    else "https://www.creativemechanicserver.com" + (if (url.startsWith("/")) url else "/$url")

@Composable
private fun UnifiedMaterialRow(material: UnifiedMaterial) {
    val count = material.countText.toIntOrNull() ?: 0
    val ctx = LocalContext.current
    if (material.iconUrl != null) {
        val url = absolutize(material.iconUrl)
        val title = material.displayName.takeIf { it.isNotBlank() } ?: material.blockId
        ListItem(
            headlineContent = {
                Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(material.blockId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            trailingContent = { Text("× ${material.countText}") },
            leadingContent = {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(ctx).data(url).crossfade(true).build(),
                    contentDescription = material.blockId,
                    modifier = Modifier.size(40.dp),
                )
            },
        )
    } else {
        io.github.moxisuki.blockprint.cat.ui.component.MaterialRow(name = material.blockId, count = count)
    }
}

@Composable
fun CommunityDetailContent(
    source: CommunitySource,
    id: String,
    viewModel: CommunityViewModel,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val state by viewModel.state.collectAsState()
    val detail by viewModel.detail.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resolveAppError = rememberAppErrorResolver()
    val resolveString = rememberStringResolver()

    val schematic: UnifiedSchematic? = remember(state, source, id, detail) {
        val fromList = when (source) {
            CommunitySource.MCS -> state.mcs.schematics.firstOrNull { it.id == id }
            CommunitySource.CMS -> state.cms.schematics.firstOrNull { it.id == id }
        }
        fromList ?: detail.headerSnapshot
    }

    LaunchedEffect(source, id) {
        viewModel.loadDetail(source, id)
        viewModel.loadPreview(source, id)
    }

    LaunchedEffect(Unit) {
        viewModel.download.collect { event ->
            when (event) {
                is DownloadEvent.Success ->
                    snackbarHostState.showSnackbar(resolveString(R.string.cdl_downloaded_snackbar, event.schematic.name))
                is DownloadEvent.Failed ->
                    snackbarHostState.showSnackbar(resolveAppError(event.error))
                is DownloadEvent.Progress -> Unit
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
            when {
                detail.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                detail.error != null && schematic == null -> Box(
                    Modifier.fillMaxSize(), Alignment.Center,
                ) {
                    Text(resolveAppError(detail.error!!), color = MaterialTheme.colorScheme.error)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SchematicHeader(
                            schematic = schematic,
                            detail = detail,
                            downloading = state.isDownloading,
                            downloadBytes = state.downloadBytes,
                            downloadTotal = state.downloadTotal,
                            downloadingName = state.downloadingName,
                            onDownload = {
                                schematic?.let {
                                    scope.launch { viewModel.downloadSchematic(context, it) }
                                }
                            },
                        )
                    }
                    item {
                        if (detail.markdown.isNotBlank()) {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.cdl_desc_label), style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(detail.markdown, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    if (detail.requirements.isNotEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        if (detail.requirements.isEmpty()) stringResource(R.string.cdl_requirements_zero)
                                        else stringResource(R.string.cdl_requirements, detail.requirements.size),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                            }
                        }
                        items(detail.requirements) { mat -> UnifiedMaterialRow(mat) }
                    }
                    if (detail.production.isNotEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.cdl_production_count, detail.production.size),
                                         style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        items(detail.production) { mat -> UnifiedMaterialRow(mat) }
                    }
                    if (detail.dependencies.isNotEmpty()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.cdl_dependencies_label), style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(detail.dependencies.joinToString("、"),
                                         style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    if (!detail.stress.isNullOrBlank()) {
                        item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.cdl_stress_value, detail.stress ?: ""),
                                         style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    if (detail.comments.isNotEmpty()) {
                        item {
                            Text(stringResource(R.string.cdl_comments, detail.comments.size),
                                 style = MaterialTheme.typography.titleMedium,
                                 modifier = Modifier.padding(top = 4.dp))
                        }
                        items(detail.comments, key = { it.uuid }, contentType = { "comment" }) { c ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(20.dp).clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Default.Person, null, Modifier.size(14.dp),
                                                 tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(c.nickName, style = MaterialTheme.typography.labelMedium,
                                             color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.weight(1f))
                                        Text(c.createTime.take(10), style = MaterialTheme.typography.labelSmall,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(c.content, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
