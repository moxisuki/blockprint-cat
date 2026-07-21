package io.github.moxisuki.blockprint.cat.app.feature.detail

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialItem
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintItem

@Immutable
internal data class BlueprintDetailState(
    val blueprint: HomeBlueprintItem? = null,
    val materials: List<BlueprintDetailMaterialItem> = emptyList(),
    val isLoading: Boolean = true,
)
