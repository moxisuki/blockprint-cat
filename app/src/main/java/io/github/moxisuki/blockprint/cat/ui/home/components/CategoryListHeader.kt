package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow
import io.github.moxisuki.blockprint.cat.ui.category.CategoryCoverView

private const val HEADER_ANIM_MS = 280

/**
 * Sticky header at the top of the Local blueprint list. Surfaces the currently
 * active category (or "All") so the user has a clear visual anchor for what
 * they're looking at, plus a quick "clear filter" affordance when a specific
 * category is selected.
 *
 * The cover + name + count crossfade via [AnimatedContent] when the user
 * switches categories, giving a subtle but visible transition instead of a
 * hard swap.
 */
@Composable
internal fun CategoryListHeader(
    selectedCategoryRow: CategoryRow,
    visibleCount: Int,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAll = selectedCategoryRow is CategoryRow.All
    val onClear = remember(onClearFilter) { onClearFilter }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cover container with subtle background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = selectedCategoryRow,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(HEADER_ANIM_MS)) togetherWith
                            fadeOut(animationSpec = tween(HEADER_ANIM_MS / 2)))
                },
                label = "categoryHeaderCover",
            ) { row ->
                when (row) {
                    is CategoryRow.All -> Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    is CategoryRow.Real -> CategoryCoverView(
                        colorIdx = row.colorIdx,
                        patternIdx = row.patternIdx,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = selectedCategoryRow,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(HEADER_ANIM_MS)) togetherWith
                            fadeOut(animationSpec = tween(HEADER_ANIM_MS / 2)))
                },
                label = "categoryHeaderName",
            ) { row ->
                val name = when (row) {
                    is CategoryRow.All -> stringResource(R.string.home_category_all)
                    is CategoryRow.Real -> row.entity.name
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.home_list_header_subtitle, visibleCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!isAll) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.home_list_header_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
