package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackRepository
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.ResourcePackDao
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.toDomain
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.toEntity
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ActiveInstall
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.PackProgress
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Singleton
class DefaultResourcePackRepository @Inject constructor(
    private val dao: ResourcePackDao,
    @ApplicationContext private val context: Context,
    private val vanillaInstaller: VanillaInstaller?,
    private val modInstaller: ModInstaller?,
    private val modrinthSource: ModrinthSource?,
) : ResourcePackRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<ResourcePackId, Job>()
    private val activeInstallsState = MutableStateFlow<Map<ResourcePackId, ActiveInstall>>(emptyMap())

    override val installedPacks: Flow<List<ResourcePackEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override val activeInstalls: Flow<Map<ResourcePackId, ActiveInstall>> = activeInstallsState

    override suspend fun searchMods(query: String, limit: Int): List<ModSearchHit> =
        modrinthSource?.searchMods(query, limit) ?: emptyList()

    override suspend fun fetchModVersions(slug: String, mcVersion: String?): List<ModVersionInfo> {
        val hits = searchMods(slug, limit = 1)
        val hit = hits.firstOrNull() ?: return emptyList()
        return modrinthSource?.versionsFor(hit.projectId) ?: emptyList()
    }

    override suspend fun installVanilla(): ResourcePackId {
        val id = ResourcePackId.Vanilla
        cancel(id)
        requireNotNull(vanillaInstaller) { "VanillaInstaller not configured" }
        launchInstall(id, "Minecraft 原版") { vanillaInstaller.install() }
        return id
    }

    override suspend fun installMod(hit: ModSearchHit, version: ModVersionInfo): ResourcePackId {
        val id = ResourcePackId.mod(hit.slug)
        cancel(id)
        requireNotNull(modInstaller) { "ModInstaller not configured" }
        launchInstall(id, hit.title) { modInstaller.install(hit, version) }
        return id
    }

    override suspend fun reinstall(id: ResourcePackId) {
        when (id) {
            ResourcePackId.Vanilla -> installVanilla()
            else -> {
                val current = dao.get(id.value) ?: return
                val version = ModVersionInfo(
                    id = current.sourceVersionId ?: current.versionName,
                    name = current.versionName,
                    gameVersions = listOfNotNull(current.mcVersion),
                    fileName = current.versionName,
                    fileSize = current.totalSize,
                    fileUrl = current.source,
                )
                val hit = ModSearchHit(
                    slug = id.modSlug,
                    title = current.displayName,
                    description = "",
                    projectId = id.modSlug,
                )
                installMod(hit, version)
            }
        }
    }

    override suspend fun delete(id: ResourcePackId) {
        runCatching { File(context.filesDir, "blockprintcat/render_assets/${id.modSlug}").deleteRecursively() }
        if (id.isVanilla) {
            runCatching { File(context.filesDir, "blockprintcat/render_assets/minecraft").deleteRecursively() }
        }
        dao.delete(id.value)
    }

    override suspend fun deleteAll() {
        runCatching { File(context.filesDir, "blockprintcat/render_assets").deleteRecursively() }
        dao.clearAll()
    }

    override fun cancel(id: ResourcePackId) {
        activeJobs.remove(id)?.cancel()
        activeInstallsState.update { it - id }
    }

    private fun launchInstall(id: ResourcePackId, displayName: String, source: () -> Flow<PackProgress>) {
        val progressFlow = source()
        val carrier = ActiveInstall(
            packId = id,
            displayName = displayName,
            fileName = "",
            totalSize = -1L,
            progress = progressFlow,
        )
        val job = scope.launch {
            progressFlow.collect { progress ->
                when (progress) {
                    is PackProgress.Done -> {
                        dao.upsert(progress.entry.toEntity())
                        activeInstallsState.update { it - id }
                    }
                    is PackProgress.Failed -> {
                        activeInstallsState.update { it - id }
                    }
                    PackProgress.Cancelled -> {
                        activeInstallsState.update { it - id }
                    }
                    else -> Unit
                }
            }
        }
        activeJobs[id] = job
        activeInstallsState.update { it + (id to carrier) }
    }
}