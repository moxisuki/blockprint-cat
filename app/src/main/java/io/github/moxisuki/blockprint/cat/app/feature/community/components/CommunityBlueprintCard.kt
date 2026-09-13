package io.github.moxisuki.blockprint.cat.app.feature.community.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.feature.community.CommunityBlueprintUiItem
import io.github.moxisuki.blockprint.cat.app.feature.community.CommunitySourceUi
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

private val CommunityCardChipHeight = 28.dp
private val CommunityCardChipRadius = 7.dp

@Composable
internal fun CommunityBlueprintCard(
    item: CommunityBlueprintUiItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(12.dp),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            CommunityPreviewTile(
                accent = communityAccentColor(item.accentIndex),
                source = item.source,
                coverUrl = item.coverUrl,
                modifier = Modifier
                    .width(104.dp)
                    .aspectRatio(1f),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.title,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = item.author,
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.meaningfulDescription()?.let { description ->
                    Text(
                        text = description,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FormatBadge(
                            format = item.format,
                            rawLabel = item.formatLabel,
                        )
                        item.primaryMetric()?.let { InfoPill(text = it, highlighted = true) }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item.secondaryMetric()?.let { InfoPill(text = it) }
                        item.categoryName?.let { category ->
                            InfoPill(text = category)
                        }
                        item.gameVersion?.let { version ->
                            InfoPill(text = version)
                        }
                        item.stress?.let { stress ->
                            InfoPill(text = stringResource(R.string.cdl_stress_value, stress))
                        }
                        item.tags.take(2).forEach { tag ->
                            InfoPill(text = tag.removePrefix("minecraft:"))
                        }
                    }
                }
                if (item.updateTime.isNotBlank()) {
                    Text(
                        text = item.updateTime,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.78f),
                        style = MiuixTheme.textStyles.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityPreviewTile(
    accent: Color,
    source: CommunitySourceUi,
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        CommunityPreviewFallback(accent = accent)
        if (coverUrl != null) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(120)
                    .build(),
                contentDescription = stringResource(R.string.community_preview_mcs),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.82f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            text = when {
                source == CommunitySourceUi.MCS -> stringResource(R.string.community_preview_mcs)
                coverUrl != null -> stringResource(R.string.community_preview_cms_cover)
                else -> stringResource(R.string.community_preview_cms)
            },
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun CommunityPreviewFallback(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(4) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { column ->
                    val highlighted = (row + column) % 3 == 0
                    Box(
                        modifier = Modifier
                            .size(if (highlighted) 18.dp else 14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (highlighted) {
                                    accent
                                } else {
                                    MiuixTheme.colorScheme.secondaryContainer
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatBadge(
    format: BlueprintFormat,
    rawLabel: String?,
) {
    Box(
        modifier = Modifier
            .height(CommunityCardChipHeight)
            .clip(RoundedCornerShape(CommunityCardChipRadius))
            .background(format.accentColor().copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = format.accentColor().copy(alpha = 0.28f),
                shape = RoundedCornerShape(CommunityCardChipRadius),
            )
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rawLabel?.takeIf { format == BlueprintFormat.Unknown }
                ?.replace('_', ' ')
                ?.replaceFirstChar { it.uppercase() }
                ?: format.localizedLabel(),
            color = format.accentColor(),
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun InfoPill(
    text: String,
    highlighted: Boolean = false,
) {
    Box(
        modifier = Modifier
            .height(CommunityCardChipHeight)
            .widthIn(max = 132.dp)
            .clip(RoundedCornerShape(CommunityCardChipRadius))
            .background(
                if (highlighted) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MiuixTheme.colorScheme.secondaryContainer
                },
            )
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (highlighted) {
                MiuixTheme.colorScheme.primary
            } else {
                MiuixTheme.colorScheme.onSecondaryContainer
            },
            style = MiuixTheme.textStyles.body2,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
private fun communityAccentColor(index: Int): Color {
    val colors = listOf(
        MiuixTheme.colorScheme.primary,
        MiuixTheme.colorScheme.onSurfaceContainer,
        MiuixTheme.colorScheme.onSurfaceVariantSummary,
        MiuixTheme.colorScheme.secondary,
    )
    return colors[index % colors.size]
}

@Composable
private fun CommunityBlueprintUiItem.primaryMetric(): String? =
    when (source) {
        CommunitySourceUi.MCS -> downloads?.let {
            "${stringResource(R.string.community_metric_downloads)} ${it.compactCount()}"
        }
        CommunitySourceUi.CMS -> downloads?.let {
            "${stringResource(R.string.community_metric_downloads)} ${it.compactCount()}"
        }
    }

private fun CommunityBlueprintUiItem.secondaryMetric(): String? =
    when (source) {
        CommunitySourceUi.MCS -> dimensions ?: categoryName
        CommunitySourceUi.CMS -> sizeText
    }

private fun Int.compactCount(): String =
    if (this >= 1000) {
        val major = this / 1000
        val minor = (this % 1000) / 100
        if (minor == 0) "${major}K" else "$major.${minor}K"
    } else {
        toString()
    }

private fun CommunityBlueprintUiItem.meaningfulDescription(): String? {
    val normalizedDescription = description.trim().removePrefix("minecraft:")
    if (normalizedDescription.isBlank()) return null
    val normalizedTags = tags.map { it.trim().removePrefix("minecraft:") }
    return normalizedDescription.takeIf { it !in normalizedTags }
}
