package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow
import io.github.moxisuki.blockprint.cat.ui.category.CategoryCoverView

private const val HEADER_ANIM_MS = 280
private const val PAGER_HEIGHT = 64

/**
 * Sticky header at the top of the Local blueprint list. Renders as a
 * HorizontalPager showing all categories as cards. The LAST page is a
 * "管理分类" card (gear + new) — swipe past all real categories to access
 * management (create/edit/delete).
 *
 * Visual: no card background, left-aligned, no peek (sibling pages are
 * not shown at rest). Selection is indicated by primary text color +
 * SemiBold on the category name.
 *
 * Interaction:
 *   - Swipe to a page → onSelect fires immediately when the swipe settles
 *     (currentPage + isScrollInProgress=false). The category switches
 *     right away, no extra tap required.
 *   - Tap a card → onEdit fires (opens the category edit dialog).
 *   - Tap the clear button → onClearFilter (reset to "全部").
 *   - Tap the manage page → onManageClick (open management dialog).
 *
 * Gesture isolation: this pager is inside the Local tab of the outer
 * HomeScreen pager (Local ↔ PC). Compose's nested HorizontalPager handles
 * gesture distribution: horizontal swipes here stay in the category pager
 * and do NOT switch Local/PC tabs.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategoryListHeader(
    rows: List<CategoryRow>,
    selectedRow: CategoryRow,
    visibleCount: Int,
    onSelect: (CategoryRow) -> Unit,
    onEdit: (CategoryRow) -> Unit,
    onClearFilter: () -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return

    val totalPages = rows.size + 1  // +1 管理页
    val initPage = rows.indexOfFirst { it == selectedRow }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initPage) { totalPages }

    // rememberUpdatedState: LaunchedEffect 闭包里始终读到最新值，避免 stale capture
    // https://developer.android.com/jetpack/compose/side-effects#rememberupdatedstate
    val latestRows by rememberUpdatedState(rows)
    val latestSelectedRow by rememberUpdatedState(selectedRow)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestOnEdit by rememberUpdatedState(onEdit)
    val latestOnClearFilter by rememberUpdatedState(onClearFilter)

    // 外部 selectedRow 变化 → 同步 pager 位置
    LaunchedEffect(selectedRow) {
        val target = latestRows.indexOfFirst { it == selectedRow }.coerceAtLeast(0)
        if (target in 0 until latestRows.size && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    // 滑动 settle → 切分类（currentPage 变化且 isScrollInProgress=false）
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage to pagerState.isScrollInProgress }
            .collect { (page, scrolling) ->
                if (!scrolling && page < latestRows.size) {
                    val row = latestRows[page]
                    if (row != latestSelectedRow) latestOnSelect(row)
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(0.dp),
        pageSpacing = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(PAGER_HEIGHT.dp),
        key = { page ->
            if (page < rows.size) {
                when (val row = rows[page]) {
                    is CategoryRow.All -> "cat-all"
                    is CategoryRow.Real -> "cat-${row.entity.id}"
                }
            } else "cat-manage"
        },
    ) { page ->
        if (page < rows.size) {
            val isActivePage = page == pagerState.currentPage
            HeaderCard(
                row = rows[page],
                selected = isActivePage,
                count = rows[page].count,
                showClear = isActivePage && isRealCategory(rows[page]),
                onClick = { latestOnEdit(rows[page]) },
                onClear = { latestOnClearFilter() },
            )
        } else {
            ManageCard(onClick = onManageClick)
        }
    }
}

private fun isRealCategory(row: CategoryRow): Boolean = row is CategoryRow.Real

/**
 * Single category card. No background, no border — blends into the app
 * background. Selection is indicated by primary text color + SemiBold.
 * Left/right padded 16.dp to align with the LazyColumn below.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeaderCard(
    row: CategoryRow,
    selected: Boolean,
    count: Int,
    showClear: Boolean,
    onClick: () -> Unit,
    onClear: () -> Unit,
) {
    val nameColor = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
    val countColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                     else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cover / icon
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = row,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(HEADER_ANIM_MS)) togetherWith
                            fadeOut(animationSpec = tween(HEADER_ANIM_MS / 2)))
                },
                label = "headerCover",
            ) { r ->
                when (r) {
                    is CategoryRow.All -> Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    is CategoryRow.Real -> CategoryCoverView(
                        colorIdx = r.colorIdx,
                        patternIdx = r.patternIdx,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = row,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(HEADER_ANIM_MS)) togetherWith
                            fadeOut(animationSpec = tween(HEADER_ANIM_MS / 2)))
                },
                label = "headerName",
            ) { r ->
                val name = when (r) {
                    is CategoryRow.All -> stringResource(R.string.home_category_all)
                    is CategoryRow.Real -> r.entity.name
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = nameColor,
                    maxLines = 1,
                )
            }
            Text(
                text = pluralStringResource(R.plurals.category_count, count, count),
                style = MaterialTheme.typography.labelSmall,
                color = countColor,
            )
        }
        // ✕ clear button — only when this category is active
        if (showClear) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.home_list_header_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Last pager page — tap opens management dialog (create/edit/delete). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManageCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.home_category_manage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
