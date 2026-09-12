package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ActiveInstall
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId

@Immutable
data class ResourcePacksState(
    val installed: List<ResourcePackEntry> = emptyList(),
    val activeInstalls: Map<ResourcePackId, ActiveInstall> = emptyMap(),
    val modSearch: ModSearchState = ModSearchState.Closed,
    val isDeleteAllConfirmVisible: Boolean = false,
    val pendingDelete: PendingResourcePackDelete? = null,
    val feedback: ResourcePacksFeedback? = null,
)

sealed interface ModSearchState {
    data object Closed : ModSearchState
    data object Searching : ModSearchState
    data class Results(val hits: List<ModSearchHit>, val query: String) : ModSearchState
    data class LoadingVersions(val hit: ModSearchHit, val previousResults: Results) : ModSearchState
    data class Versions(
        val hit: ModSearchHit,
        val versions: List<ModVersionInfo>,
        val previousResults: Results,
    ) : ModSearchState
    data class VersionError(
        val hit: ModSearchHit,
        val message: String,
        val previousResults: Results,
    ) : ModSearchState
}

sealed interface ResourcePacksFeedback {
    data class Info(val message: String) : ResourcePacksFeedback
    data class Error(val message: String) : ResourcePacksFeedback
}

@Immutable
data class PendingResourcePackDelete(
    val id: ResourcePackId,
    val displayName: String,
)
