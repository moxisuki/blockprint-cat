package io.github.moxisuki.blockprint.cat.app.feature.community.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.AppMotion
import io.github.moxisuki.blockprint.cat.app.feature.community.CommunitySourceUi
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val CommunitySourceTabsWidth = 152.dp
private val CommunitySourceTabWidth = 73.dp
private val CommunitySourceTabsHeight = 36.dp
private val CommunitySourceTabsInset = 3.dp

@Composable
internal fun CommunitySourceControls(
    selectedSource: CommunitySourceUi,
    sourceSlideProgress: Float,
    onSourceSelected: (CommunitySourceUi) -> Unit,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onSearchClear: () -> Unit,
    showTopicButton: Boolean,
    areTopicsVisible: Boolean,
    onTopicsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CommunitySourceCapsule(
                selectedSource = selectedSource,
                slideProgress = sourceSlideProgress,
                onSourceSelected = onSourceSelected,
            )
            Spacer(modifier = Modifier.weight(1f))
            ToolIconButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(R.string.community_search_hint),
                selected = isSearchExpanded || searchQuery.isNotBlank(),
                onClick = onSearchClick,
            )
            AnimatedToolButtonVisibility(visible = showTopicButton) {
                ToolIconButton(
                    icon = Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.community_section_topics),
                    selected = areTopicsVisible,
                    onClick = onTopicsClick,
                )
            }
        }
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = fadeIn(
                animationSpec = AppMotion.fadeEnterSpec(AppMotion.Duration.AppBarMillis),
            ) + expandVertically(animationSpec = tween(durationMillis = AppMotion.Duration.ChildEnterMillis)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = AppMotion.Duration.ChildExitMillis)) +
                fadeOut(animationSpec = AppMotion.fadeExitSpec(AppMotion.Duration.TabExitMillis)),
        ) {
            CommunitySearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSubmit = onSearchSubmit,
                onClear = onSearchClear,
            )
        }
    }
}

@Composable
internal fun CommunitySourceOverview(
    totalCount: String,
    loadedCount: String,
    status: String,
    hint: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 150)) +
            expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = 220),
            ),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = 180),
        ) + fadeOut(animationSpec = tween(durationMillis = 120)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = status,
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = hint,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CommunityStatCard(
                    label = stringResource(R.string.community_stat_total),
                    value = totalCount,
                    modifier = Modifier.weight(1f),
                )
                CommunityStatCard(
                    label = stringResource(R.string.community_stat_loaded),
                    value = loadedCount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CommunitySourceCapsule(
    selectedSource: CommunitySourceUi,
    slideProgress: Float,
    onSourceSelected: (CommunitySourceUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedSlideProgress = slideProgress.coerceIn(0f, 1f)
    val indicatorOffset = CommunitySourceTabWidth * normalizedSlideProgress

    Box(
        modifier = modifier
            .width(CommunitySourceTabsWidth)
            .height(CommunitySourceTabsHeight)
            .clip(RoundedCornerShape(CommunitySourceTabsHeight / 2))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(CommunitySourceTabsInset),
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(CommunitySourceTabWidth)
                .height(CommunitySourceTabsHeight - CommunitySourceTabsInset * 2)
                .clip(RoundedCornerShape((CommunitySourceTabsHeight - CommunitySourceTabsInset * 2) / 2))
                .background(MiuixTheme.colorScheme.primary),
        )
        Row {
            CommunitySourceUi.entries.forEach { source ->
                SourceSegment(
                    text = source.displayName,
                    selectionProgress = if (source == CommunitySourceUi.MCS) {
                        1f - normalizedSlideProgress
                    } else {
                        normalizedSlideProgress
                    },
                    onClick = { onSourceSelected(source) },
                    modifier = Modifier
                        .width(CommunitySourceTabWidth)
                        .height(CommunitySourceTabsHeight - CommunitySourceTabsInset * 2),
                )
            }
        }
    }
}

@Composable
private fun SourceSegment(
    text: String,
    selectionProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = lerp(
        start = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        stop = MiuixTheme.colorScheme.onPrimary,
        fraction = selectionProgress.coerceIn(0f, 1f),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CommunitySourceTabsHeight / 2))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun AnimatedToolButtonVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = AppMotion.fadeEnterSpec(AppMotion.Duration.BottomBarEnterMillis),
        ) +
            expandHorizontally(
                expandFrom = Alignment.End,
                animationSpec = tween(durationMillis = AppMotion.Duration.ChildEnterMillis),
            ),
        exit = shrinkHorizontally(
            shrinkTowards = Alignment.End,
            animationSpec = tween(durationMillis = AppMotion.Duration.ChildExitMillis),
        ) + fadeOut(
            animationSpec = AppMotion.fadeExitSpec(AppMotion.Duration.BottomBarExitMillis),
        ),
    ) {
        content()
    }
}

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (selected) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantSummary
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CommunitySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(start = 12.dp, end = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.community_search_hint),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(18.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MiuixTheme.textStyles.body2.copy(
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) {
                            Text(
                                text = stringResource(R.string.community_search_hint),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                style = MiuixTheme.textStyles.body2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            AnimatedVisibility(
                visible = query.isNotBlank(),
                enter = fadeIn(animationSpec = tween(durationMillis = 120)) +
                    expandHorizontally(expandFrom = Alignment.End),
                exit = shrinkHorizontally(shrinkTowards = Alignment.End) +
                    fadeOut(animationSpec = tween(durationMillis = 100)),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(MiuixTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.community_filter_clear_input),
                        tint = MiuixTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        insideMargin = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(3.dp))
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
