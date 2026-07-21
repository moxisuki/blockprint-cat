package io.github.moxisuki.blockprint.cat.app.feature.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintFormat
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintItem
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

private val BlueprintChipHeight = 24.dp

@Composable
internal fun BlueprintCard(
    blueprint: HomeBlueprintItem,
    onClick: () -> Unit = {},
    onDetailClick: () -> Unit = {},
    onMoveCategoryClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    selected: Boolean = false,
    selectionMode: Boolean = false,
    showLocalActions: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val cardColor by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        },
        label = "blueprintCardSelectionColor",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.primary.copy(alpha = 0.66f)
        } else if (selectionMode) {
            MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.10f)
        } else {
            Color.Transparent
        },
        label = "blueprintCardSelectionBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else if (selectionMode) 1.dp else 0.dp,
        label = "blueprintCardSelectionBorderWidth",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            ),
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(color = cardColor),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onClick,
        onLongPress = onLongClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (selectionMode) 12.dp else 14.dp,
                    top = 12.dp,
                    end = 10.dp,
                    bottom = 12.dp,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                if (selectionMode) {
                    SelectionIndicator(
                        selected = selected,
                        modifier = Modifier.padding(top = 2.dp, end = 10.dp),
                    )
                }
                BlueprintTitleBlock(
                    blueprint = blueprint,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (!selectionMode) {
                    BlueprintActionsButton(
                        showLocalActions = showLocalActions,
                        onDetailClick = onDetailClick,
                        onMoveCategoryClick = onMoveCategoryClick,
                        onRenameClick = onRenameClick,
                        onDeleteClick = onDeleteClick,
                    )
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
            BlueprintMetaSummary(blueprint = blueprint)
            Spacer(modifier = Modifier.height(6.dp))
            BlueprintMetricLine(blueprint = blueprint)
        }
    }
}

@Composable
private fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                if (selected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
            )
            .border(
                width = 1.5.dp,
                color = if (selected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.38f)
                },
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun BlueprintTitleBlock(
    blueprint: HomeBlueprintItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(top = 1.dp),
    ) {
        Text(
            text = blueprint.name,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = blueprint.fileName,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BlueprintMetaSummary(blueprint: HomeBlueprintItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FormatBadge(format = blueprint.format)
        CategoryBadge(
            text = blueprint.category.ifBlank {
                stringResource(R.string.home_category_uncategorized)
            },
        )
        SizeBadge(
            text = blueprint.size,
        )
    }
}

@Composable
private fun BlueprintMetricLine(blueprint: HomeBlueprintItem) {
    val author = stringResource(R.string.bp_card_author, blueprint.author)
    val blocks = stringResource(R.string.bp_card_blocks, blueprint.blockCount)
    val regions = stringResource(R.string.bp_card_region, blueprint.regionCount)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetricText(
            text = author,
            modifier = Modifier.weight(1.15f),
        )
        MetricDivider()
        MetricText(
            text = blocks,
            modifier = Modifier.weight(1f),
        )
        MetricDivider()
        MetricText(
            text = regions,
            modifier = Modifier.weight(0.85f),
        )
    }
}

@Composable
private fun BlueprintActionsButton(
    showLocalActions: Boolean,
    onDetailClick: () -> Unit,
    onMoveCategoryClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val entry = DropdownEntry(
        items = buildList {
            add(
            DropdownItem(
                text = stringResource(R.string.action_detail),
                icon = { modifier ->
                    DropdownMenuIcon(
                        icon = Icons.Filled.Info,
                        modifier = modifier,
                    )
                },
                onClick = onDetailClick,
            ),
            )
            if (showLocalActions) {
                add(
                    DropdownItem(
                        text = stringResource(R.string.action_move_category),
                        icon = { modifier ->
                            DropdownMenuIcon(
                                icon = Icons.Filled.Folder,
                                modifier = modifier,
                            )
                        },
                        onClick = onMoveCategoryClick,
                    ),
                )
                add(
                    DropdownItem(
                        text = stringResource(R.string.action_rename),
                        icon = { modifier ->
                            DropdownMenuIcon(
                                icon = Icons.Filled.Edit,
                                modifier = modifier,
                            )
                        },
                        onClick = onRenameClick,
                    ),
                )
                add(
                    DropdownItem(
                        text = stringResource(R.string.action_delete),
                        icon = { modifier ->
                            DropdownMenuIcon(
                                icon = Icons.Filled.Delete,
                                modifier = modifier,
                            )
                        },
                        onClick = onDeleteClick,
                    ),
                )
            }
        },
    )

    OverlayIconDropdownMenu(
        entry = entry,
        backgroundColor = MiuixTheme.colorScheme.surfaceContainer,
        cornerRadius = 17.dp,
        minWidth = 34.dp,
        minHeight = 34.dp,
    ) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(R.string.home_blueprint_more_actions),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun DropdownMenuIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = modifier.size(18.dp),
    )
}

@Composable
private fun MetricText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun MetricDivider() {
    Text(
        text = "·",
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.55f),
        style = MiuixTheme.textStyles.body2,
        modifier = Modifier.padding(horizontal = 6.dp),
    )
}

@Composable
private fun CategoryBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .height(BlueprintChipHeight)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FormatBadge(format: HomeBlueprintFormat) {
    Box(
        modifier = Modifier
            .height(BlueprintChipHeight)
            .clip(RoundedCornerShape(4.dp))
            .background(format.accentColor().copy(alpha = 0.12f))
            .border(1.dp, format.accentColor().copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = format.localizedLabel(),
            color = format.accentColor(),
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SizeBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(BlueprintChipHeight)
            .clip(RoundedCornerShape(5.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
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
private fun BlueprintCardPreview() {
    PreviewAppTheme {
        BlueprintCard(
            blueprint = HomeBlueprintItem(
                id = "preview",
                name = "Cherry Courtyard With A Very Long Blueprint Title",
                fileName = "cherry_courtyard.litematic",
                format = HomeBlueprintFormat.Litematica,
                category = "Survival",
                author = "moxisuki",
                blockCount = "48,320",
                regionCount = 3,
                size = "2.4 MB",
                updatedAt = "07-18 22:40",
            )
        )
    }
}
