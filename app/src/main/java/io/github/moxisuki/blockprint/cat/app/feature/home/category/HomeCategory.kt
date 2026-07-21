package io.github.moxisuki.blockprint.cat.app.feature.home.category

import androidx.compose.runtime.Immutable

object HomeCategoryId {
    const val All = "all"
    const val Uncategorized = "uncategorized"
}

@Immutable
data class BlueprintCategoryFilter(
    val id: String,
    val label: String,
    val count: Int,
)
