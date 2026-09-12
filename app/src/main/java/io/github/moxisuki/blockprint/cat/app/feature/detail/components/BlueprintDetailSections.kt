package io.github.moxisuki.blockprint.cat.app.feature.detail.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.preview.PreviewCacheInfo
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.feature.detail.BlueprintNamespaceItem
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintFormat
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintItem
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class BlueprintDetailMaterialItem(
    val name: String,
    val count: Int,
    val iconUrls: List<String> = emptyList(),
    val displayName: String? = null,
)

@Composable
internal fun BlueprintDetailHeader(
    blueprint: HomeBlueprintItem,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(18.dp),
    ) {
        Text(
            text = blueprint.name,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = blueprint.fileName,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailChip(
                text = blueprint.format.localizedLabel(),
                contentColor = blueprint.format.accentColor(),
                containerColor = blueprint.format.accentColor().copy(alpha = 0.12f),
                borderColor = blueprint.format.accentColor().copy(alpha = 0.2f),
            )
            DetailChip(text = blueprint.category.ifBlank {
                stringResource(R.string.home_category_uncategorized)
            })
            DetailChip(text = blueprint.size)
        }
    }
}

@Composable
internal fun BlueprintDetailActions(
    onPreviewClick: () -> Unit,
    onRegeneratePreviewClick: () -> Unit,
    onConvertClick: () -> Unit,
    previewCache: PreviewCacheInfo,
    isConverting: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            onClick = onPreviewClick,
            colors = ButtonDefaults.buttonColorsPrimary(),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onPrimary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(
                    if (previewCache.isReady) R.string.detail_view_cached
                    else R.string.detail_generate,
                ),
                color = MiuixTheme.colorScheme.onPrimary,
                style = MiuixTheme.textStyles.button,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (previewCache.isReady) {
                DetailActionItem(
                    modifier = Modifier.weight(1f),
                    icon = MiuixIcons.Refresh,
                    text = stringResource(R.string.detail_regenerate),
                    contentColor = MiuixTheme.colorScheme.primary,
                    onClick = onRegeneratePreviewClick,
                )
                Spacer(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.18f)),
                )
            }
            DetailActionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.SwapHoriz,
                text = stringResource(
                    if (isConverting) R.string.detail_convert_running
                    else R.string.detail_convert_action,
                ),
                contentColor = MiuixTheme.colorScheme.onSurfaceContainer,
                enabled = !isConverting,
                onClick = onConvertClick,
            )
        }
    }
}

@Composable
private fun DetailActionItem(
    icon: ImageVector,
    text: String,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor.copy(alpha = if (enabled) 1f else 0.42f),
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text,
            color = contentColor.copy(alpha = if (enabled) 1f else 0.42f),
            style = MiuixTheme.textStyles.button,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun BlueprintDetailStats(
    blueprint: HomeBlueprintItem,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatItem(
                label = stringResource(R.string.detail_stat_block_count),
                value = blueprint.blockCount,
                modifier = Modifier.weight(1f),
            )
            StatItem(
                label = stringResource(R.string.detail_stat_region_count),
                value = blueprint.regionCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatItem(
                label = stringResource(R.string.detail_stat_size),
                value = blueprint.size,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun BlueprintDetailInfoSection(
    blueprint: HomeBlueprintItem,
    source: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.detail_meta_title),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        InfoRow(
            label = stringResource(R.string.detail_meta_author),
            value = blueprint.author.ifBlank { stringResource(R.string.detail_meta_unknown) },
        )
        InfoRow(
            label = stringResource(R.string.detail_meta_format),
            value = blueprint.format.localizedLabel(),
        )
        InfoRow(
            label = stringResource(R.string.detail_meta_category),
            value = blueprint.category.ifBlank { stringResource(R.string.home_category_uncategorized) },
        )
        InfoRow(
            label = stringResource(R.string.detail_meta_source),
            value = source,
        )
        InfoRow(
            label = stringResource(R.string.detail_meta_file_name),
            value = blueprint.fileName,
            scrollableValue = true,
        )
    }
}

@Composable
internal fun BlueprintDetailMaterialsSection(
    materials: List<BlueprintDetailMaterialItem>,
    modifier: Modifier = Modifier,
) {
    var showAll by androidx.compose.runtime.remember(materials.size) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    val visibleMaterials = if (showAll) materials else materials.take(MAX_COLLAPSED_MATERIALS)

    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (showAll) {
                    stringResource(R.string.detail_material_all, materials.size)
                } else {
                    stringResource(R.string.detail_material_top10)
                },
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (materials.size > MAX_COLLAPSED_MATERIALS) {
                TextButton(
                    text = stringResource(
                        if (showAll) R.string.detail_material_collapse
                        else R.string.detail_material_show_all,
                    ),
                    onClick = { showAll = !showAll },
                    minWidth = 0.dp,
                    minHeight = 32.dp,
                    insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (materials.isEmpty()) {
            Text(
                text = stringResource(R.string.detail_material_empty),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        } else {
            val maxCount = materials.maxOf { it.count }.coerceAtLeast(1)
            LazyColumn(
                modifier = Modifier.heightIn(max = if (showAll) 460.dp else 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(visibleMaterials, key = { it.name }) { material ->
                    MaterialRow(
                        material = material,
                        fraction = material.count.toFloat() / maxCount,
                    )
                }
            }
        }
    }
}

private const val MAX_COLLAPSED_MATERIALS = 10

@Composable
internal fun BlueprintDetailNamespacesSection(
    namespaces: List<BlueprintNamespaceItem>,
    onNamespaceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.detail_namespaces_title),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.detail_namespaces_subtitle),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            namespaces.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNamespaceClick(item.namespace) }
                        .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.54f))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (item.isInstalled) MiuixIcons.Ok else MiuixIcons.Download,
                        contentDescription = null,
                        tint = if (item.isInstalled) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                        modifier = Modifier.width(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.namespace,
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                            style = MiuixTheme.textStyles.body1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(
                                if (item.isInstalled) {
                                    R.string.detail_namespace_installed
                                } else {
                                    R.string.detail_namespace_missing
                                },
                            ),
                            color = if (item.isInstalled) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurfaceVariantSummary
                            },
                            style = MiuixTheme.textStyles.body2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = MiuixIcons.ChevronForward,
                        contentDescription = stringResource(R.string.detail_namespace_manage),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.width(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BlueprintDetailMissing(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.detail_load_failed),
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.detail_error_bp_missing),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun InfoRow(
    label: String,
    value: String,
    scrollableValue: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.width(110.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (scrollableValue) {
            Text(
                text = value,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.body2,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 900,
                        repeatDelayMillis = 1_200,
                    ),
                maxLines = 1,
            )
        } else {
            Text(
                text = value,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.body2,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MaterialRow(
    material: BlueprintDetailMaterialItem,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MaterialIcon(material = material)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.displayName
                        ?: material.name.removePrefix("minecraft:").replace("_", " "),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = material.name,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = material.count.toString(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.72f)),
            )
        }
    }
}

@Composable
private fun MaterialIcon(
    material: BlueprintDetailMaterialItem,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.key(material.name, material.iconUrls) {
        val attemptState = androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableIntStateOf(0)
        }
        val url = material.iconUrls.getOrNull(attemptState.intValue)
        if (url != null) {
            val request = androidx.compose.runtime.remember(context, url) {
                ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build()
            }
            SubcomposeAsyncImage(
                model = request,
                contentDescription = material.name,
                modifier = modifier
                    .width(34.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(7.dp)),
                error = {
                    if (attemptState.intValue < material.iconUrls.lastIndex) {
                        attemptState.intValue += 1
                    } else {
                        MaterialIconFallback(material = material)
                    }
                },
            )
        } else {
            MaterialIconFallback(material = material, modifier = modifier)
        }
    }
}

@Composable
private fun MaterialIconFallback(
    material: BlueprintDetailMaterialItem,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(34.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = material.name
                .substringAfter(':', material.name)
                .replace("_", "")
                .take(2)
                .uppercase(),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailChip(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    borderColor: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeBlueprintFormat.accentColor(): Color = when (this) {
    HomeBlueprintFormat.Litematica -> MiuixTheme.colorScheme.primary
    HomeBlueprintFormat.Schematic -> MiuixTheme.colorScheme.onSurfaceContainer
    HomeBlueprintFormat.Nbt -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    HomeBlueprintFormat.BuildingHelper -> MiuixTheme.colorScheme.onSurfaceContainer
    HomeBlueprintFormat.Unknown -> MiuixTheme.colorScheme.onSurfaceVariantSummary
}

@Composable
private fun HomeBlueprintFormat.localizedLabel(): String = when (this) {
    HomeBlueprintFormat.Litematica -> stringResource(R.string.format_short_litematica)
    HomeBlueprintFormat.Schematic -> stringResource(R.string.format_short_worldedit)
    HomeBlueprintFormat.Nbt -> stringResource(R.string.format_short_nbt)
    HomeBlueprintFormat.BuildingHelper -> stringResource(R.string.format_short_building_helper)
    HomeBlueprintFormat.Unknown -> stringResource(R.string.format_short_unknown)
}

@Preview(showBackground = true)
@Composable
private fun BlueprintDetailHeaderPreview() {
    PreviewAppTheme {
        BlueprintDetailHeader(
            blueprint = HomeBlueprintItem(
                id = "preview",
                name = "Cherry Courtyard",
                fileName = "cherry_courtyard.litematic",
                format = HomeBlueprintFormat.Litematica,
                category = "Survival",
                author = "moxisuki",
                blockCount = "48,320",
                regionCount = 3,
                size = "2.4 MB",
                updatedAt = "07-18 22:40",
            ),
        )
    }
}
