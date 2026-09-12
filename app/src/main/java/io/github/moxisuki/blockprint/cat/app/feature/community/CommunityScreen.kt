package io.github.moxisuki.blockprint.cat.app.feature.community

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.design.AppMotion
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import io.github.moxisuki.blockprint.cat.app.core.persistence.McsAuthCookies
import io.github.moxisuki.blockprint.cat.app.feature.community.components.CommunityBlueprintCard
import io.github.moxisuki.blockprint.cat.app.feature.community.components.CommunitySectionTitle
import io.github.moxisuki.blockprint.cat.app.feature.community.components.CommunitySourceControls
import io.github.moxisuki.blockprint.cat.app.feature.community.components.CommunitySourceOverview
import io.github.moxisuki.blockprint.cat.app.feature.community.components.CommunityTopicStrip
import io.github.moxisuki.blockprint.cat.app.feature.community.components.McsLoginRequiredPanel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val FloatingNavigationCommunityBottomPadding = 104.dp

@Composable
internal fun CommunityScreen(
    state: CommunityState,
    onAction: (CommunityAction) -> Unit,
    onLoginClick: () -> Unit,
    onBlueprintClick: (CommunityBlueprintUiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTopics by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = state.selectedSource.ordinal,
        pageCount = { CommunitySourceUi.entries.size },
    )
    val activeContent = remember(state.selectedSource, state.mcs, state.cms) {
        state.activeContent
    }
    val showTopicButton = activeContent.isLoggedIn &&
        (activeContent.topics.isNotEmpty() || activeContent.selectedTopics.isNotEmpty())
    val sourceSlideProgress by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, (CommunitySourceUi.entries.size - 1).toFloat())
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onAction(CommunityAction.SourceSelected(CommunitySourceUi.entries[page]))
            }
    }

    LaunchedEffect(state.selectedSource) {
        if (pagerState.currentPage != state.selectedSource.ordinal) {
            pagerState.animateScrollToPage(state.selectedSource.ordinal)
        }
    }

    val selectedOverviewVisible = state.selectedSource in state.visibleOverviewSources
    LaunchedEffect(state.selectedSource, selectedOverviewVisible) {
        if (!selectedOverviewVisible) return@LaunchedEffect
        delay(2400)
        onAction(CommunityAction.OverviewDismissed(state.selectedSource))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appMaxContentWidth(),
    ) {
        CommunitySourceControls(
            selectedSource = state.selectedSource,
            sourceSlideProgress = sourceSlideProgress,
            onSourceSelected = { source -> onAction(CommunityAction.SourceSelected(source)) },
            isSearchExpanded = activeContent.isSearchExpanded,
            searchQuery = activeContent.searchDraft,
            onSearchClick = { onAction(CommunityAction.SearchToggled) },
            onSearchQueryChange = { onAction(CommunityAction.SearchQueryChanged(it)) },
            onSearchSubmit = { onAction(CommunityAction.SearchSubmitted) },
            onSearchClear = { onAction(CommunityAction.SearchCleared) },
            showTopicButton = showTopicButton,
            areTopicsVisible = showTopics,
            onTopicsClick = { showTopics = !showTopics },
            modifier = Modifier.padding(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 10.dp,
            ),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            val source = CommunitySourceUi.entries[page]
            val content = when (source) {
                CommunitySourceUi.MCS -> state.mcs.toSourceContent()
                CommunitySourceUi.CMS -> state.cms.toSourceContent()
            }
            CommunitySourcePage(
                content = content,
                isCheckingLogin = state.mcs.isCheckingLogin,
                showOverview = source in state.visibleOverviewSources,
                showTopics = showTopics,
                onRefresh = { onAction(CommunityAction.RefreshRequested) },
                onLoadMore = { onAction(CommunityAction.LoadMoreRequested) },
                onTopicClick = { topic -> onAction(CommunityAction.TopicToggled(topic)) },
                onLoginClick = onLoginClick,
                onBlueprintClick = onBlueprintClick,
            )
        }
    }
}

@Composable
private fun CommunitySourcePage(
    content: CommunitySourceContent,
    isCheckingLogin: Boolean,
    showOverview: Boolean,
    showTopics: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onTopicClick: (String) -> Unit,
    onLoginClick: () -> Unit,
    onBlueprintClick: (CommunityBlueprintUiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyGridState()
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val currentIsRefreshing by rememberUpdatedState(content.isRefreshing)
    val pullDistancePx = remember { mutableFloatStateOf(0f) }
    val refreshThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }
    val hasTopics = content.topics.isNotEmpty() || content.selectedTopics.isNotEmpty()
    val pullRefreshConnection = remember(listState, refreshThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || currentIsRefreshing) {
                    pullDistancePx.floatValue = 0f
                    return Offset.Zero
                }
                val isAtTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (available.y > 0f && isAtTop) {
                    pullDistancePx.floatValue += available.y
                    if (pullDistancePx.floatValue >= refreshThresholdPx) {
                        pullDistancePx.floatValue = 0f
                        currentOnRefresh()
                    }
                } else if (available.y < 0f || !isAtTop) {
                    pullDistancePx.floatValue = 0f
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState, content.items.size, content.hasMore, content.isLoading) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (content.hasMore &&
                    content.items.isNotEmpty() &&
                    !content.isLoading &&
                    lastVisibleIndex >= content.items.lastIndex - 2
                ) {
                    currentOnLoadMore()
                }
            }
    }

    if (content.source == CommunitySourceUi.MCS && !content.isLoggedIn) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(pullRefreshConnection)
                .appScrollEndHaptic(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 0.dp,
                end = 16.dp,
                bottom = FloatingNavigationCommunityBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "mcs-login", span = { GridItemSpan(maxLineSpan) }) {
                McsLoginRequiredPanel(
                    isCheckingLogin = isCheckingLogin,
                    errorMessage = content.errorMessage,
                    onLoginClick = onLoginClick,
                    onRetryClick = onRefresh,
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CommunitySourceOverview(
                totalCount = stringResource(R.string.community_total_x, content.total),
                loadedCount = stringResource(R.string.community_loaded_x, content.items.size),
                status = content.status,
                hint = content.hint,
                visible = showOverview,
            )
            CommunityRefreshIndicator(
                visible = content.isRefreshing,
            )
            content.errorMessage?.let { message ->
                CommunityMessageCard(
                    title = "加载失败",
                    message = message,
                )
            }
            if (hasTopics) {
                AnimatedVisibility(
                    visible = showTopics,
                    enter = fadeIn(
                        animationSpec = AppMotion.fadeEnterSpec(AppMotion.Duration.AppBarMillis),
                    ) +
                        expandVertically(
                            expandFrom = Alignment.Top,
                            animationSpec = tween(durationMillis = AppMotion.Duration.ChildEnterMillis),
                        ),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(durationMillis = AppMotion.Duration.ChildExitMillis),
                    ) + fadeOut(
                        animationSpec = AppMotion.fadeExitSpec(AppMotion.Duration.TabExitMillis),
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CommunitySectionTitle(text = stringResource(R.string.community_section_topics))
                        CommunityTopicStrip(
                            topics = content.topics,
                            selectedTopics = content.selectedTopics,
                            onTopicClick = onTopicClick,
                        )
                    }
                }
                CommunitySectionTitle(text = stringResource(R.string.community_section_featured))
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(pullRefreshConnection)
                .appScrollEndHaptic(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = FloatingNavigationCommunityBottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (content.items.isEmpty() && content.isLoading) {
                item(key = "${content.source}-loading", span = { GridItemSpan(maxLineSpan) }) {
                    CommunityLoadingPanel(source = content.source)
                }
            } else if (content.items.isEmpty()) {
                item(key = "${content.source}-empty", span = { GridItemSpan(maxLineSpan) }) {
                    CommunityMessageCard(
                        title = stringResource(R.string.community_empty),
                        message = if (content.filter.isBlank()) {
                            "下拉刷新，或稍后再试。"
                        } else {
                            "没有匹配 “${content.filter}” 的蓝图。"
                        },
                    )
                }
            } else {
                items(
                    items = content.items,
                    key = { item -> item.id },
                    contentType = { item -> "community-blueprint-${item.source}" },
                ) { item ->
                    CommunityBlueprintCard(
                        item = item,
                        onClick = { onBlueprintClick(item) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = AppMotion.fadeEnterSpec(AppMotion.Duration.ContentEnterMillis),
                            fadeOutSpec = AppMotion.fadeExitSpec(AppMotion.Duration.ContentExitMillis),
                        ),
                    )
                }
                item(key = "${content.source}-load-more", span = { GridItemSpan(maxLineSpan) }) {
                    CommunityLoadMoreIndicator(
                        visible = content.isLoading && content.items.isNotEmpty(),
                    )
                }
            }
            item(key = "${content.source}-bottom-space", span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CommunityRefreshIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = AppMotion.fadeEnterSpec(AppMotion.Duration.AppBarMillis),
        ) +
            expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(durationMillis = AppMotion.Duration.ChildEnterMillis),
            ),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = AppMotion.Duration.ChildExitMillis),
        ) + fadeOut(
            animationSpec = AppMotion.fadeExitSpec(AppMotion.Duration.TabExitMillis),
        ),
    ) {
        CommunityProgressRow(
            text = stringResource(R.string.community_refreshing),
            modifier = modifier,
        )
    }
}

@Composable
private fun CommunityLoadMoreIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = AppMotion.fadeEnterSpec(AppMotion.Duration.AppBarMillis),
        ) + expandVertically(animationSpec = tween(durationMillis = AppMotion.Duration.ChildEnterMillis)),
        exit = shrinkVertically(animationSpec = tween(durationMillis = AppMotion.Duration.ChildExitMillis)) +
            fadeOut(animationSpec = AppMotion.fadeExitSpec(AppMotion.Duration.TabExitMillis)),
    ) {
        CommunityProgressRow(
            text = "继续载入",
            modifier = modifier,
        )
    }
}

@Composable
private fun CommunityProgressRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center,
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
                text = text,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CommunityLoadingPanel(
    source: CommunitySourceUi,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                size = 18.dp,
                strokeWidth = 2.dp,
            )
            Text(
                text = "正在载入 ${source.displayName} 蓝图",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

@Composable
private fun CommunityMessageCard(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = title,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = message,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal enum class CommunitySourceUi(val displayName: String) {
    MCS("MCS"),
    CMS("CMS"),
}

internal data class CommunitySourceContent(
    val source: CommunitySourceUi,
    val total: Int,
    val status: String,
    val hint: String,
    val topics: List<String>,
    val items: List<CommunityBlueprintUiItem>,
    val isLoggedIn: Boolean = true,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
    val filter: String = "",
    val searchDraft: String = "",
    val selectedTopics: List<String> = emptyList(),
    val isSearchExpanded: Boolean = false,
)

internal data class CommunityBlueprintUiItem(
    val id: String,
    val source: CommunitySourceUi,
    val title: String,
    val author: String,
    val heat: Int? = null,
    val downloads: Int? = null,
    val dimensions: String? = null,
    val sizeText: String? = null,
    val format: BlueprintFormat,
    val tags: List<String> = emptyList(),
    val description: String,
    val updateTime: String,
    val stress: String? = null,
    val coverUrl: String? = null,
    val downloadable: Boolean = true,
    val webUrl: String? = null,
    val accentIndex: Int,
)

internal val cmsPreviewItems = listOf(
    CommunityBlueprintUiItem(
        id = "cms-gate",
        source = CommunitySourceUi.CMS,
        title = "Mountain Gate",
        author = "Sora",
        downloads = 6302,
        sizeText = "2.6 MB",
        format = BlueprintFormat.Schematic,
        tags = listOf("Castle", "Entrance", "Medieval"),
        description = "Large mountain entrance with decorative side towers.",
        updateTime = "Jul 16, 2026",
        stress = "Low",
        coverUrl = "https://www.creativemechanicserver.com/",
        downloadable = false,
        webUrl = "https://www.creativemechanicserver.com/",
        accentIndex = 3,
    ),
    CommunityBlueprintUiItem(
        id = "cms-sorter",
        source = CommunitySourceUi.CMS,
        title = "Auto Sorter Core",
        author = "Creative Mechanic",
        downloads = 9104,
        sizeText = "820 KB",
        format = BlueprintFormat.Litematica,
        tags = listOf("Storage", "Redstone", "NBT"),
        description = "Compact sorter module with clearly separated input lanes.",
        updateTime = "Jul 09, 2026",
        stress = "Medium",
        coverUrl = "https://www.creativemechanicserver.com/",
        downloadable = false,
        webUrl = "https://www.creativemechanicserver.com/",
        accentIndex = 4,
    ),
)

@Preview(showBackground = true)
@Composable
private fun CommunityScreenPreview() {
    PreviewAppTheme {
        CommunityScreen(
            state = CommunityState(
                mcs = CommunityMcsState(
                    cookies = McsAuthCookies(userAuth = "preview", nickname = "moxisuki"),
                    items = listOf(
                        CommunityBlueprintUiItem(
                            id = "mcs-foundry",
                            source = CommunitySourceUi.MCS,
                            title = "Compact Foundry Line",
                            author = "Aster",
                            heat = 8421,
                            dimensions = "96 x 28 x 64",
                            format = BlueprintFormat.Litematica,
                            tags = listOf("minecraft:furnace", "factory", "survival"),
                            description = "Multi-smelter line with storage buffer and item routing.",
                            updateTime = "2026-07-18",
                            accentIndex = 0,
                        ),
                    ),
                    total = 1286,
                ),
            ),
            onAction = {},
            onLoginClick = {},
            onBlueprintClick = {},
        )
    }
}
