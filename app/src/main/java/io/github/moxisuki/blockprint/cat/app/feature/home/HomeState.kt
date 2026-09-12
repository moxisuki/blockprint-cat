package io.github.moxisuki.blockprint.cat.app.feature.home

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintImportPreview
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.LocalBlueprint
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcBridgeDefaultPort
import io.github.moxisuki.blockprint.cat.app.core.pcbridge.PcBridgeState
import io.github.moxisuki.blockprint.cat.app.feature.home.category.HomeCategoryId
import java.text.NumberFormat
import java.util.Locale

@Immutable
data class HomeState(
    val localBlueprintTreeUri: String? = null,
    val localBlueprintTreeDocumentId: String? = null,
    val selectedSource: HomeBlueprintSource = HomeBlueprintSource.Local,
    val localBlueprints: List<HomeBlueprintItem> = emptyList(),
    val pcBridgeState: PcBridgeState = PcBridgeState(),
    val pcHostInput: String = "",
    val pcPortInput: String = PcBridgeDefaultPort.toString(),
    val pcTokenInput: String = "",
    val customCategories: List<String> = emptyList(),
    val selectedCategoryId: String = HomeCategoryId.All,
    val isCategoryBarVisible: Boolean = false,
    val isCategoryManageVisible: Boolean = false,
    val isSearchExpanded: Boolean = false,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val selectedBlueprintIds: Set<String> = emptySet(),
    val categoryMoveBlueprintIds: Set<String> = emptySet(),
    val renameBlueprintId: String? = null,
    val deleteBlueprintId: String? = null,
    val importPreview: HomeImportPreview? = null,
    val isImportPreviewLoading: Boolean = false,
    val isImporting: Boolean = false,
) {
    val isSafDirectorySelected: Boolean
        get() = !localBlueprintTreeUri.isNullOrBlank()

    val isSelectionMode: Boolean
        get() = selectedBlueprintIds.isNotEmpty()
}

@Immutable
data class HomeImportPreview(
    val sourceUri: String,
    val fileName: String,
    val displayName: String,
    val format: HomeBlueprintFormat,
    val author: String,
    val blockCount: String,
    val regionCount: Int,
    val size: String,
    val errorMessage: String? = null,
) {
    val hasError: Boolean
        get() = errorMessage != null
}

enum class HomeBlueprintSource {
    Local,
    Pc,
}

enum class HomeBlueprintFormat {
    Litematica,
    Schematic,
    Nbt,
    BuildingHelper,
    Unknown,
}

@Immutable
data class HomeBlueprintItem(
    val id: String,
    val name: String,
    val fileName: String,
    val format: HomeBlueprintFormat,
    val category: String,
    val author: String,
    val blockCount: String,
    val regionCount: Int,
    val size: String,
    val updatedAt: String,
)

internal fun previewLocalBlueprints(): List<HomeBlueprintItem> = listOf(
    HomeBlueprintItem(
        id = "local-courtyard",
        name = "Cherry Courtyard",
        fileName = "cherry_courtyard.litematic",
        format = HomeBlueprintFormat.Litematica,
        category = "Survival",
        author = "moxisuki",
        blockCount = "48,320",
        regionCount = 3,
        size = "2.4 MB",
        updatedAt = "07-18 22:40",
    ),
    HomeBlueprintItem(
        id = "local-workshop",
        name = "Redstone Workshop",
        fileName = "redstone_workshop.schematic",
        format = HomeBlueprintFormat.Schematic,
        category = "Machine",
        author = "Orange Peel",
        blockCount = "12,608",
        regionCount = 1,
        size = "860 KB",
        updatedAt = "07-17 09:12",
    ),
    HomeBlueprintItem(
        id = "local-statue",
        name = "Cat Statue",
        fileName = "cat_statue.nbt",
        format = HomeBlueprintFormat.Nbt,
        category = "Pixel Art",
        author = "Unknown",
        blockCount = "6,144",
        regionCount = 1,
        size = "420 KB",
        updatedAt = "07-14 18:06",
    ),
)

internal fun LocalBlueprint.toHomeBlueprintItem(): HomeBlueprintItem = HomeBlueprintItem(
    id = id,
    name = displayName,
    fileName = fileName,
    format = format.toHomeBlueprintFormat(),
    category = category,
    author = author,
    blockCount = formatCount(blockCount),
    regionCount = regionCount,
    size = formatFileSize(sizeBytes),
    updatedAt = "",
)

internal fun BlueprintImportPreview.toHomeImportPreview(): HomeImportPreview = HomeImportPreview(
    sourceUri = sourceUri,
    fileName = fileName,
    displayName = displayName,
    format = format.toHomeBlueprintFormat(),
    author = author,
    blockCount = formatCount(blockCount),
    regionCount = regionCount,
    size = formatFileSize(sizeBytes),
)

internal fun BlueprintFormat.toHomeBlueprintFormat(): HomeBlueprintFormat = when (this) {
    BlueprintFormat.Litematica -> HomeBlueprintFormat.Litematica
    BlueprintFormat.Schematic -> HomeBlueprintFormat.Schematic
    BlueprintFormat.Nbt -> HomeBlueprintFormat.Nbt
    BlueprintFormat.BuildingHelper -> HomeBlueprintFormat.BuildingHelper
    BlueprintFormat.Unknown -> HomeBlueprintFormat.Unknown
}

internal fun formatCount(count: Int): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).format(count)

internal fun formatFileSize(bytes: Long): String {
    if (bytes < 0L) return "-"
    val kb = 1024.0
    val mb = kb * 1024.0
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < mb -> String.format(Locale.getDefault(), "%.1f KB", bytes / kb)
        else -> String.format(Locale.getDefault(), "%.1f MB", bytes / mb)
    }
}
