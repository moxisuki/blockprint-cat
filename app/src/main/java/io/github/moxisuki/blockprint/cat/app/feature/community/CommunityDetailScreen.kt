package io.github.moxisuki.blockprint.cat.app.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialItem
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialsSection
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailNamespacesSection
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommunityDetailRoute(
    source: String,
    blueprintId: String,
    title: String,
    author: String,
    format: BlueprintFormat,
    description: String,
    heat: Int?,
    downloads: Int?,
    dimensions: String?,
    sizeText: String?,
    stress: String?,
    updateTime: String,
    coverUrl: String?,
    tags: List<String>,
    downloadable: Boolean,
    webUrl: String?,
    gameVersion: String? = null,
    versionNumber: Int = 1,
    categoryName: String? = null,
    formatLabel: String? = null,
    onResourceNamespaceClick: (String) -> Unit = {},
    viewModel: CommunityDetailViewModel = hiltViewModel(),
) {
    val seed = CommunityDetailSeed(
        source = source,
        blueprintId = blueprintId,
        title = title,
        author = author,
        format = format,
        description = description,
        heat = heat,
        downloads = downloads,
        dimensions = dimensions,
        sizeText = sizeText,
        stress = stress,
        updateTime = updateTime,
        coverUrl = coverUrl,
        tags = tags,
        downloadable = downloadable,
        webUrl = webUrl,
        gameVersion = gameVersion,
        versionNumber = versionNumber,
        categoryName = categoryName,
        formatLabel = formatLabel,
    )
    LaunchedEffect(seed) {
        viewModel.setSeed(seed)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    CommunityDetailScreen(
        state = state,
        onDownloadClick = viewModel::download,
        onOpenWebClick = {
            state.seed.webUrl?.let(uriHandler::openUri)
        },
        onResourceNamespaceClick = onResourceNamespaceClick,
    )
}

@Composable
private fun CommunityDetailScreen(
    state: CommunityDetailState,
    onDownloadClick: () -> Unit,
    onOpenWebClick: () -> Unit,
    onResourceNamespaceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seed = state.seed
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appMaxContentWidth(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "cover") {
            CommunityDetailCover(
                coverUrl = seed.coverUrl,
                accent = communityDetailAccent(seed.blueprintId),
            )
        }
        item(key = "header") {
            CommunityDetailHeader(state = state)
        }
        val namespaces = state.namespaces
        if (namespaces.isNotEmpty()) {
            item(key = "namespaces") {
                BlueprintDetailNamespacesSection(
                    namespaces = namespaces,
                    onNamespaceClick = onResourceNamespaceClick,
                )
            }
        }
        if (state.hasStats()) {
            item(key = "stats") {
                CommunityDetailStats(state = state)
            }
        }
        if (seed.downloadable || seed.webUrl != null) {
            item(key = "actions") {
                CommunityDetailActions(
                    showDownload = seed.downloadable,
                    showOpenWeb = seed.webUrl != null,
                    downloadState = state.downloadState,
                    downloadProgress = state.downloadProgress,
                    downloadMessage = state.downloadMessage,
                    onDownloadClick = onDownloadClick,
                    onOpenWebClick = onOpenWebClick,
                )
            }
        }
        item(key = "description") {
            CommunityDetailTextSection(
                title = stringResource(R.string.cdl_desc_label),
                body = state.description.ifBlank { stringResource(R.string.cdl_no_desc) },
            )
        }
        state.detailErrorMessage?.let { message ->
            item(key = "detail-error") {
                CommunityDetailTextSection(
                    title = stringResource(R.string.detail_load_failed),
                    body = message,
                )
            }
        }
        item(key = "materials") {
            if (state.isLoadingDetail) {
                CommunityDetailLoadingMaterials()
            } else {
                BlueprintDetailMaterialsSection(materials = state.materials)
            }
        }
    }
}

@Composable
private fun CommunityDetailCover(
    coverUrl: String?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(8.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        if (coverUrl != null) {
            val context = LocalContext.current
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { CommunityDetailCoverFallback(accent = accent) },
                error = { CommunityDetailCoverFallback(accent = accent) },
            )
        } else {
            CommunityDetailCoverFallback(accent = accent)
        }
    }
}

@Composable
private fun CommunityDetailCoverFallback(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        repeat(5) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(7) { column ->
                    val active = (row * 2 + column) % 4 == 0
                    Box(
                        modifier = Modifier
                            .size(if (active) 24.dp else 18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (active) accent else MiuixTheme.colorScheme.secondaryContainer,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityDetailHeader(
    state: CommunityDetailState,
    modifier: Modifier = Modifier,
) {
    val seed = state.seed
    val categoryName = state.payload.categoryName ?: seed.categoryName
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = seed.title,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = seed.author,
            color = MiuixTheme.colorScheme.primary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        categoryName?.let { category ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val sourceFormat = state.payload.sourceFormat
                ?: seed.formatLabel
                ?: seed.format.localizedLabel()
            CommunityDetailChip(
                text = stringResource(
                    R.string.community_detail_source_format,
                    sourceFormat.displayCommunityFormat(),
                ),
                selected = true,
                contentColor = seed.format.accentColor(),
                containerColor = seed.format.accentColor().copy(alpha = 0.12f),
                borderColor = seed.format.accentColor().copy(alpha = 0.2f),
            )
            state.payload.viewerSourceFormat
                ?.takeIf { it.isNotBlank() && !it.equals(sourceFormat, ignoreCase = true) }
                ?.let { viewerFormat ->
                    CommunityDetailChip(
                        text = stringResource(
                            R.string.community_detail_viewer_format,
                            viewerFormat.displayCommunityFormat(),
                        ),
                    )
                }
            if (seed.versionNumber > 0) {
                CommunityDetailChip(
                    text = stringResource(R.string.community_detail_version, seed.versionNumber),
                )
            }
            (state.payload.gameVersion ?: seed.gameVersion)
                ?.takeIf { it.isNotBlank() }
                ?.let { gameVersion ->
                    CommunityDetailChip(
                        text = gameVersion,
                    )
                }
            categoryName?.let { category ->
                CommunityDetailChip(text = category)
            }
            state.primaryMetricText()?.let { CommunityDetailChip(text = it) }
            state.secondaryMetricText()?.let { CommunityDetailChip(text = it) }
        }
        if (seed.updateTime.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = seed.updateTime,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        state.payload.attribution?.let { attribution ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = attribution.localizedCommunityAttribution(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CommunityDetailStats(
    state: CommunityDetailState,
    modifier: Modifier = Modifier,
) {
    val payload = state.payload
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        CommunityDetailSectionTitle(text = stringResource(R.string.community_detail_stats))
        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            (payload.gameVersion ?: state.seed.gameVersion)?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_game_version), it)
            }
            payload.dimensions?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_dimensions), it)
            }
            payload.blockCount?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_blocks), it.toString())
            }
            payload.visibleBlockCount?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_visible_blocks), it.toString())
            }
            payload.materialKindCount?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_material_kinds), it.toString())
            }
            payload.paletteSize?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_palette), it.toString())
            }
            payload.tileEntityCount?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_tile_entities), it.toString())
            }
            payload.entityCount?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_entities), it.toString())
            }
            payload.viewCount?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_views), it.compactCommunityCount())
            }
            payload.downloadCount?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_downloads), it.compactCommunityCount())
            }
            payload.likeCount?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_likes), it.compactCommunityCount())
            }
            payload.favouriteCount?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_favourites), it.compactCommunityCount())
            }
            payload.fileSizeBytes?.let {
                CommunityDetailStatItem(stringResource(R.string.community_detail_file_size), it.communityFileSize())
            }
        }
    }
}

@Composable
private fun CommunityDetailStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(140.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CommunityDetailActions(
    showDownload: Boolean,
    showOpenWeb: Boolean,
    downloadState: CommunityDownloadState,
    downloadProgress: Float?,
    downloadMessage: String?,
    onDownloadClick: () -> Unit,
    onOpenWebClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showDownload) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onDownloadClick,
                    enabled = downloadState != CommunityDownloadState.Downloading,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (downloadState) {
                            CommunityDownloadState.Downloading -> downloadProgress?.let { progress ->
                                stringResource(R.string.cdl_downloading_pct, (progress * 100).toInt())
                            } ?: stringResource(R.string.cdl_downloading)
                            CommunityDownloadState.Downloaded -> stringResource(R.string.cdl_downloaded)
                            else -> stringResource(R.string.cdl_download)
                        },
                        color = MiuixTheme.colorScheme.onPrimary,
                        style = MiuixTheme.textStyles.button,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (showOpenWeb) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onOpenWebClick,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSecondaryVariant,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.cdl_open_web),
                        color = MiuixTheme.colorScheme.onSecondaryVariant,
                        style = MiuixTheme.textStyles.button,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (downloadState == CommunityDownloadState.Downloading) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = downloadProgress ?: 0f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        downloadMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when (downloadState) {
                    CommunityDownloadState.Downloaded -> stringResource(R.string.snackbar_downloaded, message)
                    CommunityDownloadState.Failed -> stringResource(R.string.snackbar_download_failed, message)
                    else -> message
                },
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CommunityDetailTextSection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        CommunityDetailSectionTitle(text = title)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CommunityDetailLoadingMaterials(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                size = 18.dp,
                strokeWidth = 2.dp,
            )
            Text(
                text = stringResource(R.string.community_refreshing),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CommunityDetailSectionTitle(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        style = MiuixTheme.textStyles.body1,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
private fun CommunityDetailChip(
    text: String,
    selected: Boolean = false,
    contentColor: Color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSecondaryContainer,
    containerColor: Color = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f) else MiuixTheme.colorScheme.secondaryContainer,
    borderColor: Color = Color.Transparent,
) {
    Text(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        text = text,
        color = contentColor,
        style = MiuixTheme.textStyles.body2,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CommunityDetailState.primaryMetricText(): String? =
    payload.downloadCount?.let {
        "${stringResource(R.string.community_metric_downloads)} ${it.compactCommunityCount()}"
    }
        ?: seed.heat?.let { "${stringResource(R.string.community_metric_heat)} ${it.compactCommunityCount()}" }
        ?: seed.downloads?.let { "${stringResource(R.string.community_metric_downloads)} ${it.compactCommunityCount()}" }

@Composable
private fun CommunityDetailState.secondaryMetricText(): String? =
    seed.dimensions
        ?: seed.sizeText
        ?: seed.stress?.let { stringResource(R.string.cdl_stress_value, it) }
        ?: seed.source.takeIf { it.isNotBlank() }

private fun CommunityDetailState.hasStats(): Boolean =
    payload.gameVersion != null ||
        seed.gameVersion != null ||
        payload.dimensions != null ||
        payload.blockCount != null ||
        payload.visibleBlockCount != null ||
        payload.materialKindCount != null ||
        payload.paletteSize != null ||
        payload.tileEntityCount != null ||
        payload.entityCount != null ||
        payload.viewCount != null ||
        payload.downloadCount != null ||
        payload.likeCount != null ||
        payload.favouriteCount != null ||
        payload.fileSizeBytes != null

private fun String.displayCommunityFormat(): String =
    replace('_', ' ')
        .replace('-', ' ')
        .trim()
        .replaceFirstChar { character -> character.uppercase() }

@Composable
private fun String.localizedCommunityAttribution(): String =
    when (lowercase()) {
        "original" -> stringResource(R.string.community_detail_attribution_original)
        "repost", "reposted" -> stringResource(R.string.community_detail_attribution_repost)
        else -> this
    }

private fun Long.communityFileSize(): String {
    if (this < 1024L) return "$this B"
    val units = arrayOf("KB", "MB", "GB")
    var value = this.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return if (value >= 100.0 || value % 1.0 == 0.0) {
        "${value.toInt()} ${units[unitIndex]}"
    } else {
        String.format(java.util.Locale.US, "%.1f %s", value, units[unitIndex])
    }
}

private fun Int.compactCommunityCount(): String =
    if (this >= 1000) {
        val major = this / 1000
        val minor = (this % 1000) / 100
        if (minor == 0) "${major}K" else "$major.${minor}K"
    } else {
        toString()
    }

@Composable
private fun BlueprintFormat.accentColor(): Color = when (this) {
    BlueprintFormat.Litematica -> MiuixTheme.colorScheme.primary
    BlueprintFormat.Schematic -> MiuixTheme.colorScheme.onSurfaceContainer
    BlueprintFormat.Nbt -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    BlueprintFormat.BuildingHelper -> MiuixTheme.colorScheme.onSurfaceContainer
    BlueprintFormat.Unknown -> MiuixTheme.colorScheme.onSurfaceVariantSummary
}

@Composable
private fun BlueprintFormat.localizedLabel(): String = when (this) {
    BlueprintFormat.Litematica -> stringResource(R.string.format_short_litematica)
    BlueprintFormat.Schematic -> stringResource(R.string.format_short_worldedit)
    BlueprintFormat.Nbt -> stringResource(R.string.format_short_nbt)
    BlueprintFormat.BuildingHelper -> stringResource(R.string.format_short_building_helper)
    BlueprintFormat.Unknown -> stringResource(R.string.format_short_unknown)
}

@Composable
private fun communityDetailAccent(id: String): Color {
    val colors = listOf(
        MiuixTheme.colorScheme.primary,
        MiuixTheme.colorScheme.secondary,
        MiuixTheme.colorScheme.onSurfaceContainer,
        MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
    return colors[id.hashCode().let { if (it < 0) -it else it } % colors.size]
}

@Preview(showBackground = true)
@Composable
private fun CommunityDetailScreenPreview() {
    PreviewAppTheme {
        CommunityDetailScreen(
            state = CommunityDetailState(
                seed = CommunityDetailSeed(
                    source = "MCS",
                    blueprintId = "preview",
                    title = "Compact Foundry Line",
                    author = "Aster",
                    format = BlueprintFormat.Litematica,
                    description = "Multi-smelter line with storage buffer and item routing.",
                    heat = 8421,
                    downloads = null,
                    dimensions = "96x28x64",
                    sizeText = null,
                    stress = null,
                    updateTime = "2026-07-18",
                    coverUrl = null,
                    tags = listOf("minecraft:furnace", "factory", "survival"),
                    downloadable = true,
                    webUrl = "https://www.mcschematic.top/home/preview",
                ),
                payload = CommunityDetailPayload(
                    materials = listOf(
                        BlueprintDetailMaterialItem("minecraft:furnace", 128, emptyList()),
                        BlueprintDetailMaterialItem("minecraft:stone_bricks", 64, emptyList()),
                    ),
                ),
            ),
            onDownloadClick = {},
            onOpenWebClick = {},
            onResourceNamespaceClick = {},
        )
    }
}
