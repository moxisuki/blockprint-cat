package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow
import io.github.moxisuki.blockprint.cat.ui.category.CategoryCoverView

/**
 * Horizontal pager showing all categories as cards. The LAST page is a
 * dedicated "管理分类" card (gear + new) — swipe past all real categories
 * to access category management (create/edit/delete).
 *
 * Gesture isolation: this pager is inside the Local tab of the outer
 * HomeScreen pager (Local ↔ PC). Compose's nested HorizontalPager handles
 * the gesture distribution: while the user is swiping across categories,
 * the inner pager consumes the horizontal drag and the outer Local/PC
 * pager does NOT switch tabs. The outer pager only receives gestures that
 * happen outside the category area.
 *
 * @param selectedIndex index into [categories] (NOT including the manage
 *                     page). Pass `categories.indexOf(selectedRow)` from
 *                     the caller.
 * @param onSelect invoked with the page index when user lands on a real
 *                 category page.
 * @param onLongPress invoked with the page index for long-press on a real
 *                    category (used to open edit dialog).
 * @param onManageClick invoked when user taps the manage card (last page).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CategoryPager(
    categories: List<CategoryRow>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onLongPress: (Int) -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return  // 防御: 没有任何 All 行时不要渲染

    // +1 是管理页
    val totalPages = categories.size + 1
    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceIn(0, categories.size - 1),
    ) { totalPages }

    // 外部 selectedIndex 变化时同步 pager 位置
    LaunchedEffect(selectedIndex) {
        val target = selectedIndex.coerceIn(0, categories.size - 1)
        if (target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 64.dp),
        pageSpacing = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(104.dp),
        key = { page ->
            if (page < categories.size) {
                when (val row = categories[page]) {
                    is CategoryRow.All -> "cat-all"
                    is CategoryRow.Real -> "cat-${row.entity.id}"
                }
            } else "cat-manage"
        },
    ) { page ->
        if (page < categories.size) {
            CategoryPagerCard(
                row = categories[page],
                selected = page == selectedIndex,
                onClick = { onSelect(page) },
                onLongClick = { onLongPress(page) },
            )
        } else {
            ManagePagerCard(onClick = onManageClick)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryPagerCard(
    row: CategoryRow,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
             else MaterialTheme.colorScheme.surface
    val border = if (selected) Modifier.border(
        width = 2.dp,
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(16.dp),
    ) else Modifier

    Column(
        modifier = Modifier
            .width(120.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .then(border)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            CategoryCoverView(
                colorIdx = row.colorIdx,
                patternIdx = row.patternIdx,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = when (row) {
                is CategoryRow.All -> stringResource(R.string.home_category_all)
                is CategoryRow.Real -> row.entity.name
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralStringResource(R.plurals.category_count, row.count, row.count),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ManagePagerCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.home_category_manage),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
