package io.github.moxisuki.blockprint.cat.app.feature.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintSource
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val SourceTabsWidth = 152.dp
private val SourceTabWidth = 73.dp
private val SourceTabsHeight = 36.dp
private val SourceTabsInset = 3.dp

@Composable
internal fun HomeTopControls(
    selectedSource: HomeBlueprintSource,
    sourceSlideProgress: Float,
    onSourceSelected: (HomeBlueprintSource) -> Unit,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onSearchClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchClear: () -> Unit,
    showCategoryButton: Boolean,
    isCategoryBarVisible: Boolean,
    onCategoryClick: () -> Unit,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    showImportButton: Boolean,
    onImportClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val refreshRotation = if (isRefreshing) {
        val refreshTransition = rememberInfiniteTransition(label = "homeRefreshRotation")
        val rotation by refreshTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
            ),
            label = "homeRefreshRotationValue",
        )
        rotation
    } else {
        0f
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceTabs(
                selectedSource = selectedSource,
                slideProgress = sourceSlideProgress,
                onSourceSelected = onSourceSelected,
            )
            Spacer(modifier = Modifier.weight(1f))
            ToolIconButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(R.string.action_search),
                selected = isSearchExpanded || searchQuery.isNotBlank(),
                onClick = onSearchClick,
            )
            AnimatedToolButtonVisibility(visible = showCategoryButton) {
                ToolIconButton(
                    icon = Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.home_filter_label_category),
                    selected = isCategoryBarVisible,
                    onClick = onCategoryClick,
                )
            }
            ToolIconButton(
                icon = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.action_refresh),
                iconRotationDegrees = if (isRefreshing) refreshRotation else 0f,
                onClick = onRefreshClick,
            )
            AnimatedToolButtonVisibility(visible = showImportButton) {
                ToolIconButton(
                    icon = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.home_cd_import),
                    onClick = onImportClick,
                )
            }
        }

        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 160)) + expandVertically(),
            exit = shrinkVertically() + fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            BlueprintSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onClear = onSearchClear,
            )
        }
    }
}

@Composable
private fun AnimatedToolButtonVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 140)) +
            expandHorizontally(
                expandFrom = Alignment.End,
                animationSpec = tween(durationMillis = 180),
            ),
        exit = shrinkHorizontally(
            shrinkTowards = Alignment.End,
            animationSpec = tween(durationMillis = 160),
        ) + fadeOut(animationSpec = tween(durationMillis = 100)),
    ) {
        content()
    }
}

@Composable
private fun SourceTabs(
    selectedSource: HomeBlueprintSource,
    slideProgress: Float,
    onSourceSelected: (HomeBlueprintSource) -> Unit,
) {
    val tabs = listOf(
        stringResource(R.string.home_tab_local),
        stringResource(R.string.home_tab_pc),
    )
    val selectedTabIndex = when (selectedSource) {
        HomeBlueprintSource.Local -> 0
        HomeBlueprintSource.Pc -> 1
    }
    val normalizedSlideProgress = slideProgress.coerceIn(0f, 1f)
    val indicatorOffset = SourceTabWidth * normalizedSlideProgress

    Box(
        modifier = Modifier
            .width(SourceTabsWidth)
            .height(SourceTabsHeight)
            .clip(RoundedCornerShape(SourceTabsHeight / 2))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(SourceTabsInset),
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(SourceTabWidth)
                .height(SourceTabsHeight - SourceTabsInset * 2)
                .clip(RoundedCornerShape((SourceTabsHeight - SourceTabsInset * 2) / 2))
                .background(MiuixTheme.colorScheme.primary),
        )

        Row {
            tabs.forEachIndexed { index, label ->
                SourceTabButton(
                    text = label,
                    selectionProgress = if (index == 0) {
                        1f - normalizedSlideProgress
                    } else {
                        normalizedSlideProgress
                    },
                    onClick = {
                        onSourceSelected(
                            when (index) {
                                0 -> HomeBlueprintSource.Local
                                else -> HomeBlueprintSource.Pc
                            },
                        )
                    },
                    modifier = Modifier
                        .width(SourceTabWidth)
                        .height(SourceTabsHeight - SourceTabsInset * 2),
                )
            }
        }
    }
}

@Composable
private fun SourceTabButton(
    text: String,
    selectionProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = lerp(
        start = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        stop = MiuixTheme.colorScheme.onPrimary,
        fraction = selectionProgress.coerceIn(0f, 1f),
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(SourceTabsHeight / 2))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
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
private fun ToolIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean = false,
    iconRotationDegrees: Float = 0f,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.surfaceContainer
    }
    val iconColor = if (selected) {
        MiuixTheme.colorScheme.onPrimary
    } else {
        MiuixTheme.colorScheme.onSurface
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
            tint = iconColor,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    rotationZ = iconRotationDegrees
                },
        )
    }
}

@Composable
private fun BlueprintSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
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
                contentDescription = stringResource(R.string.action_search),
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
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.home_filter_search_hint),
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
                visible = query.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(durationMillis = 120)) +
                    expandHorizontally(expandFrom = Alignment.End),
                exit = shrinkHorizontally(shrinkTowards = Alignment.End) +
                    fadeOut(animationSpec = tween(durationMillis = 100)),
            ) {
                ToolIconButton(
                    icon = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.home_search_clear),
                    onClick = onClear,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}
