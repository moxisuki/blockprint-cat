package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ActiveInstall
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import kotlinx.coroutines.flow.Flow

interface ResourcePackRepository {
    val installedPacks: Flow<List<ResourcePackEntry>>
    val activeInstalls: Flow<Map<ResourcePackId, ActiveInstall>>

    suspend fun searchMods(query: String, limit: Int = 10): List<ModSearchHit>
    suspend fun fetchModVersions(slug: String, mcVersion: String?): List<ModVersionInfo>

    suspend fun installVanilla(): ResourcePackId
    suspend fun installMod(hit: ModSearchHit, version: ModVersionInfo): ResourcePackId
    suspend fun reinstall(id: ResourcePackId)
    suspend fun delete(id: ResourcePackId)
    suspend fun deleteAll()
    fun cancel(id: ResourcePackId)
}