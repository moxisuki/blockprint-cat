package io.github.moxisuki.blockprint.cat.ui.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow
import io.github.moxisuki.blockprint.cat.ui.home.HomeViewModel

/**
 * Thin connector between [HomeViewModel] and [CategoryRail].
 *
 * Subscribes to the ViewModel's `categories` and `selectedCategoryId` flows
 * using lifecycle-aware collection so collection pauses with the host's
 * lifecycle (avoids background recompositions).
 *
 * Pass-through callbacks let the Home screen own navigation / long-press
 * dialog dispatch without the rail needing to know about them.
 */
@Composable
fun CategoryHomeSection(
    vm: HomeViewModel,
    onCategoryClick: (CategoryRow) -> Unit,
    onCategoryLongClick: (CategoryRow) -> Unit,
    onAddClick: () -> Unit,
) {
    val rows by vm.categories.collectAsStateWithLifecycle()
    val selectedId by vm.selectedCategoryId.collectAsStateWithLifecycle()
    CategoryRail(
        rows = rows,
        selectedId = selectedId,
        onCategoryClick = onCategoryClick,
        onCategoryLongClick = onCategoryLongClick,
        onAddClick = onAddClick,
    )
}