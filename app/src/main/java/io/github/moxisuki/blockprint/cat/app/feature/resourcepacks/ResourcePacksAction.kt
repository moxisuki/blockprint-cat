package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId

sealed interface ResourcePacksAction {
    data object Opened : ResourcePacksAction

    data object VanillaDownload : ResourcePacksAction
    data object VanillaRedownload : ResourcePacksAction
    data object VanillaDelete : ResourcePacksAction
    data object VanillaCancel : ResourcePacksAction

    data class ModRedownload(val id: ResourcePackId) : ResourcePacksAction
    data class ModDelete(val id: ResourcePackId) : ResourcePacksAction
    data class ModCancel(val id: ResourcePackId) : ResourcePacksAction

    data object PendingDeleteConfirm : ResourcePacksAction
    data object PendingDeleteDismiss : ResourcePacksAction

    data class ModSearchOpen(val initialQuery: String = "") : ResourcePacksAction
    data class ModSearchQueryChanged(val query: String) : ResourcePacksAction
    data object ModSearchSubmit : ResourcePacksAction
    data class ModSearchHitSelected(val hit: ModSearchHit) : ResourcePacksAction
    data class ModSearchVersionSelected(val version: ModVersionInfo) : ResourcePacksAction
    data object ModSearchBack : ResourcePacksAction
    data object ModSearchClose : ResourcePacksAction

    data object DeleteAllClicked : ResourcePacksAction
    data object DeleteAllConfirm : ResourcePacksAction
    data object DeleteAllDismissed : ResourcePacksAction
    data object FeedbackDismissed : ResourcePacksAction
}
