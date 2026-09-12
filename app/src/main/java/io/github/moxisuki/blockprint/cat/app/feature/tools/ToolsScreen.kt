package io.github.moxisuki.blockprint.cat.app.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ToolsBottomPadding = 104.dp

@Composable
internal fun ToolsScreen(
    entries: List<ToolEntryUi>,
    onToolClick: (ToolDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val featured = remember(entries) { entries.firstOrNull { it.destination == ToolDestination.ImageToBlueprint } }
    val compactEntries = remember(entries) { entries.filterNot { it.destination == ToolDestination.ImageToBlueprint } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appScrollEndHaptic()
            .appMaxContentWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 12.dp,
            end = 20.dp,
            bottom = ToolsBottomPadding,
        ),
    ) {
        item(key = "hero") {
            ToolsHeroCard()
        }
        featured?.let { entry ->
            item(key = entry.destination.name) {
                FeaturedToolCard(
                    entry = entry,
                    icon = entry.icon(),
                    onClick = { onToolClick(entry.destination) },
                )
            }
        }
        item(key = "compact-grid") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                compactEntries.forEach { entry ->
                    CompactToolCard(
                        entry = entry,
                        icon = entry.icon(),
                        onClick = { onToolClick(entry.destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolsHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolWorkbenchMark()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = stringResource(R.string.nav_title_tools),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.tools_subtitle),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        MiniMosaicStrip()
    }
}

@Composable
private fun FeaturedToolCard(
    entry: ToolEntryUi,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        cornerRadius = 22.dp,
        insideMargin = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolIconFrame(color = entry.accent, icon = icon, size = 58)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = stringResource(entry.titleRes),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(entry.subtitleRes),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

@Composable
private fun CompactToolCard(
    entry: ToolEntryUi,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        cornerRadius = 20.dp,
        insideMargin = PaddingValues(14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            ToolIconFrame(color = entry.accent, icon = icon, size = 46)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(entry.titleRes),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(entry.subtitleRes),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ToolWorkbenchMark() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.26f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(3) { col ->
                        val active = (row + col) % 2 == 0
                        Box(
                            modifier = Modifier
                                .size(if (active) 13.dp else 10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (active) {
                                        MiuixTheme.colorScheme.primary
                                    } else {
                                        MiuixTheme.colorScheme.secondary.copy(alpha = 0.36f)
                                    },
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMosaicStrip() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val colors = listOf(
            MiuixTheme.colorScheme.primary,
            Color(0xFFE09A28),
            Color(0xFF13A891),
            Color(0xFFCF4B57),
            MiuixTheme.colorScheme.secondary,
            MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        colors.forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (index % 2 == 0) 26.dp else 18.dp)
                    .align(Alignment.Bottom)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = if (index % 2 == 0) 0.22f else 0.14f)),
            )
        }
    }
}

@Composable
private fun ToolIconFrame(color: Color, icon: ImageVector, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.20f), RoundedCornerShape((size / 3).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size((size * 0.48f).dp),
        )
    }
}

@Composable
private fun ToolEntryUi.icon(): ImageVector = when (destination) {
    ToolDestination.ImageToBlueprint -> MiuixIcons.File
    ToolDestination.TextToBlueprint -> MiuixIcons.Edit
    ToolDestination.BlockPaint -> MiuixIcons.GridView
}

@Preview(showBackground = true)
@Composable
private fun ToolsScreenPreview() {
    PreviewAppTheme {
        ToolsScreen(
            entries = ToolEntries,
            onToolClick = {},
        )
    }
}
