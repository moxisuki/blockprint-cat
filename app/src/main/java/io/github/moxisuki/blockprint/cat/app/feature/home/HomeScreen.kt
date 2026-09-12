package io.github.moxisuki.blockprint.cat.app.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import io.github.moxisuki.blockprint.cat.app.feature.home.category.BlueprintCategoryFilter
import io.github.moxisuki.blockprint.cat.app.feature.home.category.CategoryFilterBar
import io.github.moxisuki.blockprint.cat.app.feature.home.category.CategoryManageSheet
import io.github.moxisuki.blockprint.cat.app.feature.home.category.HomeCategoryId
import io.github.moxisuki.blockprint.cat.app.feature.home.components.BlueprintCard
import io.github.moxisuki.blockprint.cat.app.feature.home.components.CategoryEmptyPanel
import io.github.moxisuki.blockprint.cat.app.feature.home.components.EmptyBlueprintPanel
import io.github.moxisuki.blockprint.cat.app.feature.home.components.HomeTopControls
import io.github.moxisuki.blockprint.cat.app.feature.home.components.ImportPreviewSheet
import io.github.moxisuki.blockprint.cat.app.feature.home.components.PcBridgePage
import io.github.moxisuki.blockprint.cat.app.feature.home.components.SafDirectoryRequiredPanel
import io.github.moxisuki.blockprint.cat.app.feature.home.components.SearchEmptyPanel
import kotlinx.coroutines.flow.distinctUntilChanged
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

private val FloatingNavigationListBottomPadding = 104.dp
private val HomeSources = listOf(HomeBlueprintSource.Local, HomeBlueprintSource.Pc)

@Composable
internal fun HomeScreen(
    state: HomeState,
    onPickSafDirectory: () -> Unit,
    onImportBlueprint: () -> Unit,
    onAction: (HomeAction) -> Unit,
    onBlueprintClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isSafDirectorySelected) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            SafDirectoryRequiredPanel(
                onPickSafDirectory = onPickSafDirectory,
            )
        }
        return
    }

    val selectedPage = state.selectedSource.pageIndex()
    val pagerState = rememberPagerState(
        initialPage = selectedPage,
        pageCount = { HomeSources.size },
    )
    val sourceSlideProgress by remember {
        derivedStateOf {
            (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                .coerceIn(0f, (HomeSources.size - 1).toFloat())
        }
    }
    val allCategoryLabel = stringResource(R.string.home_category_all)
    val uncategorizedCategoryLabel = stringResource(R.string.home_category_uncategorized)
    val categoryFilters = remember(
        state.localBlueprints,
        state.customCategories,
        allCategoryLabel,
    ) {
        state.localBlueprints.toCategoryFilters(
            customCategories = state.customCategories,
            allLabel = allCategoryLabel,
        )
    }

    LaunchedEffect(selectedPage) {
        if (pagerState.currentPage != selectedPage) {
            pagerState.animateScrollToPage(selectedPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onAction(HomeAction.SourceSelected(HomeSources[page]))
            }
    }
    val showLocalCategoryControls = state.selectedSource == HomeBlueprintSource.Local &&
        state.localBlueprints.isNotEmpty()
    val showLocalImportButton = state.selectedSource == HomeBlueprintSource.Local
    val showCategoryBar = showLocalCategoryControls && state.isCategoryBarVisible

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appMaxContentWidth(),
    ) {
        HomeTopControls(
            selectedSource = state.selectedSource,
            sourceSlideProgress = sourceSlideProgress,
            pcConnection = state.pcBridgeState.connection,
            onSourceSelected = { onAction(HomeAction.SourceSelected(it)) },
            isSearchExpanded = state.isSearchExpanded,
            searchQuery = state.searchQuery,
            onSearchClick = { onAction(HomeAction.SearchToggled) },
            onSearchQueryChange = { onAction(HomeAction.SearchQueryChanged(it)) },
            onSearchClear = { onAction(HomeAction.SearchCleared) },
            showCategoryButton = showLocalCategoryControls,
            isCategoryBarVisible = state.isCategoryBarVisible,
            onCategoryClick = { onAction(HomeAction.CategoryBarToggled) },
            isRefreshing = state.isRefreshing,
            onRefreshClick = { onAction(HomeAction.RefreshClicked) },
            showImportButton = showLocalImportButton,
            onImportClick = onImportBlueprint,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 12.dp,
                end = 16.dp,
                bottom = 10.dp,
            ),
        )
        AnimatedVisibility(
            visible = showCategoryBar,
            enter = fadeIn(animationSpec = tween(durationMillis = 140)) +
                expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(durationMillis = 220),
                ),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(durationMillis = 180),
            ) + fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            CategoryFilterBar(
                categories = categoryFilters,
                selectedCategoryId = state.selectedCategoryId,
                onCategorySelected = { onAction(HomeAction.CategorySelected(it)) },
                manageLabel = stringResource(R.string.home_category_manage),
                onManageClick = {
                    onAction(HomeAction.CategoryManageVisibilityChanged(visible = true))
                },
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        AnimatedVisibility(
            visible = state.isSelectionMode && state.selectedSource == HomeBlueprintSource.Local,
            enter = fadeIn(animationSpec = tween(durationMillis = 140)) +
                expandVertically(animationSpec = tween(durationMillis = 180)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 160)) +
                fadeOut(animationSpec = tween(durationMillis = 100)),
        ) {
            BlueprintSelectionBar(
                selectedCount = state.selectedBlueprintIds.size,
                onMoveClick = {
                    onAction(HomeAction.MoveCategoryRequested(state.selectedBlueprintIds))
                },
                onClearClick = {
                    onAction(HomeAction.BlueprintSelectionCleared)
                },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        ) { page ->
            val source = HomeSources[page]
            when (source) {
                HomeBlueprintSource.Local -> {
                    BlueprintListPage(
                        source = source,
                        blueprints = state.localBlueprints,
                        onBlueprintClick = onBlueprintClick,
                        selectedCategoryId = if (state.isCategoryBarVisible) {
                            state.selectedCategoryId
                        } else {
                            HomeCategoryId.All
                        },
                        searchQuery = state.searchQuery,
                        selectedBlueprintIds = state.selectedBlueprintIds,
                        selectionMode = state.isSelectionMode,
                        showLocalActions = true,
                        onBlueprintLongClick = { blueprintId ->
                            onAction(HomeAction.BlueprintLongPressed(blueprintId))
                        },
                        onBlueprintSelectionToggle = {
                            onAction(HomeAction.BlueprintSelectionToggled(it))
                        },
                        onMoveCategoryClick = { blueprintId ->
                            onAction(HomeAction.MoveCategoryRequested(setOf(blueprintId)))
                        },
                        onRenameClick = { blueprintId ->
                            onAction(HomeAction.RenameRequested(blueprintId))
                        },
                        onDeleteClick = { blueprintId ->
                            onAction(HomeAction.DeleteRequested(blueprintId))
                        },
                        onImportClick = onImportBlueprint,
                        enablePullToRevealCategory = showLocalCategoryControls &&
                            !state.isCategoryBarVisible,
                        onRevealCategoryBar = {
                            onAction(HomeAction.CategoryBarVisibilityChanged(visible = true))
                        },
                    )
                }
                HomeBlueprintSource.Pc -> {
                    PcBridgePage(
                        bridgeState = state.pcBridgeState,
                        host = state.pcHostInput,
                        port = state.pcPortInput,
                        token = state.pcTokenInput,
                        searchQuery = state.searchQuery,
                        onHostChange = { onAction(HomeAction.PcHostChanged(it)) },
                        onPortChange = { onAction(HomeAction.PcPortChanged(it)) },
                        onTokenChange = { onAction(HomeAction.PcTokenChanged(it)) },
                        onDeviceClick = {
                            onAction(HomeAction.PcDeviceSelected(it.host, it.port))
                        },
                        onConnectClick = { onAction(HomeAction.PcConnectClicked) },
                        onDisconnectClick = { onAction(HomeAction.PcDisconnectClicked) },
                        onRefreshClick = { onAction(HomeAction.PcRefreshClicked) },
                        onDownloadClick = {
                            onAction(HomeAction.PcDownloadClicked(it.id))
                        },
                        onCancelTaskClick = {
                            onAction(HomeAction.PcTaskCancelClicked(it))
                        },
                        onDismissError = { onAction(HomeAction.PcErrorDismissed) },
                    )
                }
            }
        }
    }

    CategoryManageSheet(
        show = state.isCategoryManageVisible && showCategoryBar,
        categories = categoryFilters.filter { it.id != HomeCategoryId.All },
        onCreateCategory = { onAction(HomeAction.CategoryCreated(it)) },
        onRenameCategory = { oldName, newName ->
            onAction(HomeAction.CategoryRenamed(oldName, newName))
        },
        onDeleteCategory = { onAction(HomeAction.CategoryDeleted(it)) },
        onDismissRequest = {
            onAction(HomeAction.CategoryManageVisibilityChanged(visible = false))
        },
    )

    CategoryMoveSheet(
        show = state.categoryMoveBlueprintIds.isNotEmpty(),
        categories = categoryFilters.filter { it.id != HomeCategoryId.All },
        uncategorizedLabel = uncategorizedCategoryLabel,
        onCategorySelected = { onAction(HomeAction.MoveCategorySelected(it)) },
        onDismissRequest = { onAction(HomeAction.MoveCategoryDismissed) },
    )

    val renameBlueprint = state.renameBlueprintId?.let { id ->
        state.localBlueprints.firstOrNull { it.id == id }
    }
    if (renameBlueprint != null) {
        RenameBlueprintDialog(
            blueprint = renameBlueprint,
            onDismissRequest = { onAction(HomeAction.RenameDismissed) },
            onConfirm = { newName ->
                onAction(HomeAction.RenameConfirmed(renameBlueprint.id, newName))
            },
        )
    }

    val deleteBlueprint = state.deleteBlueprintId?.let { id ->
        state.localBlueprints.firstOrNull { it.id == id }
    }
    if (deleteBlueprint != null) {
        DeleteBlueprintDialog(
            blueprint = deleteBlueprint,
            onDismissRequest = { onAction(HomeAction.DeleteDismissed) },
            onConfirm = {
                onAction(HomeAction.DeleteConfirmed(deleteBlueprint.id))
            },
        )
    }

    ImportPreviewSheet(
        show = state.importPreview != null,
        preview = state.importPreview,
        loading = state.isImportPreviewLoading,
        importing = state.isImporting,
        onConfirm = { onAction(HomeAction.ImportConfirmed) },
        onDismissRequest = { onAction(HomeAction.ImportDismissed) },
    )
}

@Composable
private fun BlueprintListPage(
    source: HomeBlueprintSource,
    blueprints: List<HomeBlueprintItem>,
    onBlueprintClick: (String) -> Unit,
    selectedCategoryId: String,
    searchQuery: String,
    selectedBlueprintIds: Set<String>,
    selectionMode: Boolean,
    showLocalActions: Boolean,
    onBlueprintLongClick: (String) -> Unit,
    onBlueprintSelectionToggle: (String) -> Unit,
    onMoveCategoryClick: (String) -> Unit,
    onRenameClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onImportClick: () -> Unit,
    enablePullToRevealCategory: Boolean,
    onRevealCategoryBar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyGridState()
    val currentOnRevealCategoryBar by rememberUpdatedState(onRevealCategoryBar)
    val pullDistancePx = remember { mutableFloatStateOf(0f) }
    val revealThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val pullToRevealCategoryConnection = remember(
        enablePullToRevealCategory,
        listState,
        revealThresholdPx,
    ) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enablePullToRevealCategory || source != NestedScrollSource.UserInput) {
                    pullDistancePx.floatValue = 0f
                    return Offset.Zero
                }
                val isAtTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (available.y > 0f && isAtTop) {
                    pullDistancePx.floatValue += available.y
                    if (pullDistancePx.floatValue >= revealThresholdPx) {
                        pullDistancePx.floatValue = 0f
                        currentOnRevealCategoryBar()
                    }
                } else if (available.y < 0f || !isAtTop) {
                    pullDistancePx.floatValue = 0f
                }
                return Offset.Zero
            }
        }
    }
    val categorizedBlueprints = remember(blueprints, selectedCategoryId) {
        blueprints.filterByCategory(selectedCategoryId)
    }
    val visibleBlueprints = remember(categorizedBlueprints, searchQuery) {
        categorizedBlueprints.filterBySearch(searchQuery)
    }
    val isCategoryFiltered = selectedCategoryId != HomeCategoryId.All
    val isSearchActive = searchQuery.isNotBlank()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 300.dp),
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(pullToRevealCategoryConnection)
            .appScrollEndHaptic(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 0.dp,
            end = 16.dp,
            bottom = FloatingNavigationListBottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (visibleBlueprints.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                if (isSearchActive && categorizedBlueprints.isNotEmpty()) {
                    SearchEmptyPanel()
                } else if (isCategoryFiltered) {
                    CategoryEmptyPanel()
                } else {
                    EmptyBlueprintPanel(
                        source = source,
                        onImportClick = onImportClick,
                    )
                }
            }
        } else {
            items(
                items = visibleBlueprints,
                key = { it.id },
            ) { blueprint ->
                BlueprintCard(
                    blueprint = blueprint,
                    selected = blueprint.id in selectedBlueprintIds,
                    selectionMode = selectionMode,
                    showLocalActions = showLocalActions,
                    onClick = {
                        if (selectionMode) {
                            onBlueprintSelectionToggle(blueprint.id)
                        } else {
                            onBlueprintClick(blueprint.id)
                        }
                    },
                    onDetailClick = { onBlueprintClick(blueprint.id) },
                    onMoveCategoryClick = { onMoveCategoryClick(blueprint.id) },
                    onRenameClick = { onRenameClick(blueprint.id) },
                    onDeleteClick = { onDeleteClick(blueprint.id) },
                    onLongClick = { onBlueprintLongClick(blueprint.id) },
                )
            }
        }
    }
}

private fun HomeBlueprintSource.pageIndex(): Int = when (this) {
    HomeBlueprintSource.Local -> 0
    HomeBlueprintSource.Pc -> 1
}

private fun List<HomeBlueprintItem>.filterByCategory(categoryId: String): List<HomeBlueprintItem> =
    when (categoryId) {
        HomeCategoryId.All -> this
        HomeCategoryId.Uncategorized -> filter { it.category.isBlank() }
        else -> filter { it.category == categoryId }
    }

private fun List<HomeBlueprintItem>.filterBySearch(query: String): List<HomeBlueprintItem> {
    val normalizedQuery = query.trim().lowercase(Locale.getDefault())
    if (normalizedQuery.isEmpty()) return this

    return filter { blueprint ->
        listOf(
            blueprint.name,
            blueprint.fileName,
            blueprint.author,
            blueprint.category,
            blueprint.format.name,
        ).any { field ->
            field.lowercase(Locale.getDefault()).contains(normalizedQuery)
        }
    }
}

private fun List<HomeBlueprintItem>.toCategoryFilters(
    customCategories: List<String>,
    allLabel: String,
): List<BlueprintCategoryFilter> {
    val countByCategory = filter { it.category.isNotBlank() }
        .groupingBy { it.category }
        .eachCount()
    val categoryCounts = (countByCategory.keys + customCategories)
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
        .map { category ->
            BlueprintCategoryFilter(category, category, countByCategory[category] ?: 0)
        }
    return buildList {
        add(
            BlueprintCategoryFilter(
                id = HomeCategoryId.All,
                label = allLabel,
                count = this@toCategoryFilters.size,
            ),
        )
        addAll(categoryCounts)
    }
}

@Composable
private fun BlueprintSelectionBar(
    selectedCount: Int,
    onMoveClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.primary.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(start = 14.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MiuixTheme.colorScheme.primary),
        )
        Text(
            text = stringResource(R.string.home_selection_count, selectedCount),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SelectionActionPill(
            onClick = onMoveClick,
        ) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = stringResource(R.string.action_move_category),
                color = MiuixTheme.colorScheme.onPrimary,
                style = MiuixTheme.textStyles.button,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(MiuixTheme.colorScheme.surface)
                .clickable(onClick = onClearClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(android.R.string.cancel),
                tint = MiuixTheme.colorScheme.onSecondaryVariant,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun SelectionActionPill(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MiuixTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun CategoryMoveSheet(
    show: Boolean,
    categories: List<BlueprintCategoryFilter>,
    uncategorizedLabel: String,
    onCategorySelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayBottomSheet(
        show = show,
        modifier = modifier,
        title = stringResource(R.string.action_move_category),
        onDismissRequest = onDismissRequest,
        insideMargin = androidx.compose.ui.unit.DpSize(16.dp, 18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCategorySelected(HomeCategoryId.Uncategorized) },
            ) {
                Text(
                    text = uncategorizedLabel,
                    color = MiuixTheme.colorScheme.onSecondaryVariant,
                    style = MiuixTheme.textStyles.button,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            categories.forEach { category ->
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onCategorySelected(category.id) },
                ) {
                    Text(
                        text = "${category.label} (${category.count})",
                        color = MiuixTheme.colorScheme.onSecondaryVariant,
                        style = MiuixTheme.textStyles.button,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameBlueprintDialog(
    blueprint: HomeBlueprintItem,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember(blueprint.id) { mutableStateOf(blueprint.fileName) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            androidx.compose.material3.Text(text = stringResource(R.string.home_blueprint_rename_title))
        },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = {
                    androidx.compose.material3.Text(text = stringResource(R.string.detail_meta_file_name))
                },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.trim().isNotBlank(),
                onClick = { onConfirm(value) },
            ) {
                androidx.compose.material3.Text(text = stringResource(R.string.action_rename))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                androidx.compose.material3.Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun DeleteBlueprintDialog(
    blueprint: HomeBlueprintItem,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            androidx.compose.material3.Text(text = stringResource(R.string.home_blueprint_delete_title))
        },
        text = {
            androidx.compose.material3.Text(
                text = stringResource(R.string.home_blueprint_delete_body, blueprint.fileName),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                androidx.compose.material3.Text(text = stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                androidx.compose.material3.Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PreviewAppTheme {
        HomeScreen(
            state = HomeState(),
            onPickSafDirectory = {},
            onImportBlueprint = {},
            onAction = {},
            onBlueprintClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenConfiguredPreview() {
    PreviewAppTheme {
        HomeScreen(
            state = HomeState(
                localBlueprintTreeUri = "content://preview/tree/blockprint",
                localBlueprints = previewLocalBlueprints(),
            ),
            onPickSafDirectory = {},
            onImportBlueprint = {},
            onAction = {},
            onBlueprintClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPcPreview() {
    PreviewAppTheme {
        HomeScreen(
            state = HomeState(
                localBlueprintTreeUri = "content://preview/tree/blockprint",
                localBlueprints = previewLocalBlueprints(),
                selectedSource = HomeBlueprintSource.Pc,
            ),
            onPickSafDirectory = {},
            onImportBlueprint = {},
            onAction = {},
            onBlueprintClick = {},
        )
    }
}
