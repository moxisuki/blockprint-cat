package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Visual: full-width card with cover/icon on the left, name + count on the
 * right, optional clear-filter ✕ button. When the user is on a specific
 * category (not "全部" and not "管理"), the clear button is shown.
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
    onClearFilter: () -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return  // 防御: 没有任何 All 行时不要渲染

    val totalPages = rows.size + 1  // +1 是管理页
    val selectedIndex = rows.indexOfFirst { it == selectedRow }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceIn(0, rows.size - 1),
    ) { totalPages }

    // 外部 selectedRow 变化时同步 pager 位置
    LaunchedEffect(selectedRow) {
        val target = rows.indexOfFirst { it == selectedRow }.coerceAtLeast(0)
        if (target in 0 until rows.size && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 32.dp),
        pageSpacing = 8.dp,
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
            val isSelectedPage = page == selectedIndex
            HeaderCard(
                row = rows[page],
                selected = isSelectedPage,
                count = if (isSelectedPage) visibleCount else rows[page].count,
                showClear = !isSelectedPage && isRealCategory(rows[page]),
                onClick = { onSelect(rows[page]) },
                onClear = onClearFilter,
            )
        } else {
            ManageCard(onClick = onManageClick)
        }
    }
}

private fun isRealCategory(row: CategoryRow): Boolean = row is CategoryRow.Real

/**
 * Single category card. Visual: rounded surfaceVariant bg, cover/icon on
 * the left, name + count on the right, optional clear ✕ button. Animated
 * border + content transition on selection.
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
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cover/icon container
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
            Text(
                text = pluralStringResource(R.plurals.category_count, count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // ✕ 清空按钮: 只在 "其他" 真实分类页面显示, 不在全部/当前选中页/管理页显示
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.home_category_manage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
