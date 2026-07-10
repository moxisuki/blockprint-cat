package io.github.moxisuki.blockprint.cat.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow
import io.github.moxisuki.blockprint.cat.ui.format.FormatFilter
import io.github.moxisuki.blockprint.cat.ui.home.components.EmptyHomeState
import io.github.moxisuki.blockprint.cat.ui.home.components.CategoryListHeader
import io.github.moxisuki.blockprint.cat.ui.home.components.CategoryEmptyState
import io.github.moxisuki.blockprint.cat.ui.home.components.HomeBlueprintCard
import io.github.moxisuki.blockprint.cat.ui.home.components.HomeFilterPanel
import io.github.moxisuki.blockprint.cat.ui.home.components.NoMatchState
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import kotlinx.coroutines.delay

private const val PAGE_SIZE = 15

/**
 * Lazy list of local blueprints used by the Local tab body.
 *
 * Implements three compose-side features:
 *   - 120ms search debounce so the filter pipeline doesn't churn on every
 *     keystroke (see [LaunchedEffect] around `filterQuery`)
 *   - `derivedStateOf` for `visibleBlueprints` + `hasMore` so the LazyColumn
 *     only sees a new list when the visible-window actually changed
 *   - Auto-load next page via a sentinel `LaunchedEffect(Unit)` inside the
 *     `load_more` lazy item — the cheap idiom for infinite scroll that
 *     doesn't need a side-effecting `produceState`
 *
 * @param allBlueprints     all on-disk blueprints (unfiltered)
 * @param visibleCount      lazy window size, ticks up by [PAGE_SIZE] on scroll
 * @param filterVisible / filterQuery / filterFormat  toolbar state — owned by
 *                          HomeScreen so they survive tab switches
 * @param onVisibleCountChange  caller needs the bump to write back so the
 *                          outer HomeScreen survives configuration changes
 * @param snackbarHostState forwarded to error toasts
 * @param onDeleteTarget / onRenameTarget / onUpload  callbacks dispatch into
 *                          the outer HomeScreen's dialog state machine
 */
@Composable
internal fun LocalBlueprintList(
    modifier: Modifier = Modifier,
    allBlueprints: List<BlueprintMeta>,
    totalBlueprintCount: Int,
    scanning: Boolean,
    visibleCount: Int,
    onVisibleCountChange: (Int) -> Unit,
    safFolderName: String?,
    onRequestSafFolder: () -> Unit,
    navController: NavController,
    onBlueprintSelected: ((BlueprintMeta) -> Unit)?,
    onDeleteTarget: (BlueprintMeta) -> Unit,
    onRenameTarget: (BlueprintMeta) -> Unit,
    onUpload: (BlueprintMeta) -> Unit,
    bridgeConnected: Boolean,
    canTransfer: Boolean,
    snackbarHostState: SnackbarHostState,
    filterVisible: Boolean,
    filterQuery: String,
    onFilterQueryChange: (String) -> Unit,
    filterFormat: FormatFilter,
    onFilterFormatChange: (FormatFilter) -> Unit,
    categories: List<CategoryRow> = emptyList(),
    selectedCategoryId: String? = null,
    onCategorySelect: (CategoryRow) -> Unit = {},
    onCategoryEdit: (CategoryRow) -> Unit = {},
    onManageCategoryClick: () -> Unit = {},
    onClearCategoryFilter: () -> Unit = {},
    onLongPress: (String) -> Unit = {},
    isSelected: (String) -> Boolean = { false },
) {
    // Debounce search query (avoid re-filtering on every keystroke)
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(filterQuery) {
        delay(120)
        debouncedQuery = filterQuery
    }

    val filtered = remember(allBlueprints, debouncedQuery, filterFormat) {
        val q = debouncedQuery.trim().lowercase()
        allBlueprints.filter { bp ->
            val matchesQuery = q.isEmpty() ||
                bp.displayName.lowercase().contains(q) ||
                bp.fileName.lowercase().contains(q)
            val matchesFormat = when (filterFormat) {
                FormatFilter.All -> true
                FormatFilter.Litematica -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Litematica
                FormatFilter.Schematic -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Sponge
                FormatFilter.Nbt -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Structure ||
                                     bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.PartialNbt ||
                                     bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Unknown
                FormatFilter.Json -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.BuildingHelper
            }
            matchesQuery && matchesFormat
        }
    }
    val visibleBlueprints by remember(filtered, visibleCount) {
        derivedStateOf { filtered.take(visibleCount) }
    }
    val hasMore by remember(filtered, visibleCount) {
        derivedStateOf { visibleCount < filtered.size }
    }

    if (scanning) {
        Column(
            modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.home_scanning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else if (safFolderName == null) {
        // SAF 未选: 引导选 SAF 文件夹
        EmptyHomeState(
            onScanFolder = onRequestSafFolder,
            safFolderName = null,
            modifier = modifier.fillMaxSize(),
        )
    } else if (totalBlueprintCount == 0) {
        // SAF 已选, 但文件夹里没有任何蓝图
        EmptyHomeState(
            onScanFolder = onRequestSafFolder,
            safFolderName = safFolderName,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        // SAF 已选 + 有文件: 完整显示 (分类条 + 筛选 + 内容)
        // 即使当前分类为空, 分类条仍要可见 (用户能切换到其他分类)
        Column(modifier = modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = filterVisible,
                enter = expandVertically(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                        fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)),
                exit = shrinkVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                       fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)),
            ) {
                HomeFilterPanel(
                    query = filterQuery,
                    onQueryChange = onFilterQueryChange,
                    selectedFormat = filterFormat,
                    onFormatChange = onFilterFormatChange,
                )
            }
            // 分类 header (sticky, 横向可滑动, 末页是管理)
            val selectedRow = categories.firstOrNull { row ->
                when (row) {
                    is CategoryRow.All -> selectedCategoryId == null
                    is CategoryRow.Real -> selectedCategoryId == row.entity.id
                }
            } ?: categories.firstOrNull() ?: CategoryRow.All(visibleBlueprints.size)
            CategoryListHeader(
                rows = categories,
                selectedRow = selectedRow,
                visibleCount = visibleBlueprints.size,
                onSelect = onCategorySelect,
                onEdit = onCategoryEdit,
                onClearFilter = onClearCategoryFilter,
                onManageClick = onManageCategoryClick,
            )
            if (visibleBlueprints.isEmpty()) {
                // 区分两种空态: 当前分类本身没文件 vs 搜索/格式过滤排除全部
                if (allBlueprints.isEmpty()) {
                    CategoryEmptyState(
                        onClearFilter = onClearCategoryFilter,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    NoMatchState(
                        onClearFilters = {
                            onFilterQueryChange("")
                            onFilterFormatChange(FormatFilter.All)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleBlueprints, key = { it.uuid }, contentType = { "bp" }) { bp ->
                        HomeBlueprintCard(
                            blueprint = bp,
                            onDetail = {
                                if (onBlueprintSelected != null) {
                                    onBlueprintSelected(bp)
                                } else {
                                    navController.navigate(NavRoutes.detailRoute(bp.uuid))
                                }
                            },
                            onDelete = { onDeleteTarget(bp) },
                            onRename = { onRenameTarget(bp) },
                            onUpload = { onUpload(bp) },
                            connected = bridgeConnected,
                            canTransfer = canTransfer,
                            selected = isSelected(bp.uuid),
                            onLongClick = { onLongPress(bp.uuid) },
                        )
                    }
                    if (hasMore) {
                        item(key = "load_more") {
                            LaunchedEffect(Unit) { onVisibleCountChange(visibleCount + PAGE_SIZE) }
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    item(key = "bottom_spacer") { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }
}

