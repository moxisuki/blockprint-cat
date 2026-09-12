package io.github.moxisuki.blockprint.cat.app.feature.detail

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.preview.PreviewCacheInfo
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialItem
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintItem

@Immutable
internal data class BlueprintDetailState(
    val blueprint: HomeBlueprintItem? = null,
    val materials: List<BlueprintDetailMaterialItem> = emptyList(),
    val namespaces: List<BlueprintNamespaceItem> = emptyList(),
    val previewCache: PreviewCacheInfo = PreviewCacheInfo(),
    val isLoading: Boolean = true,
)

@Immutable
internal data class BlueprintNamespaceItem(
    val namespace: String,
    val isInstalled: Boolean,
)

@Immutable
internal sealed interface BlueprintConversionState {
    data object Idle : BlueprintConversionState
    data class Running(val target: BlueprintFormat) : BlueprintConversionState
    data class Success(val target: BlueprintFormat, val fileName: String) : BlueprintConversionState
    data class Failure(val message: String) : BlueprintConversionState
}
