package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackRepository
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.LangReader
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.ResourcePackDao
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.toDomain
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.data.toEntity
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ActiveInstall
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.PackProgress
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackInstallRequest
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

private const val TAG = "ResourcePackRepository"
private const val TERMINAL_PROGRESS_RETENTION_MS = 12_000L

@Singleton
class DefaultResourcePackRepository @Inject constructor(
    private val dao: ResourcePackDao,
    @ApplicationContext private val context: Context,
    private val vanillaInstaller: VanillaInstaller?,
    private val modInstaller: ModInstaller?,
    private val modrinthSource: ModrinthSource?,
) : ResourcePackRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<ResourcePackId, Job>()
    private val activeInstallsState = MutableStateFlow<Map<ResourcePackId, ActiveInstall>>(emptyMap())

    override val installedPacks: Flow<List<ResourcePackEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override val activeInstalls: Flow<Map<ResourcePackId, ActiveInstall>> = activeInstallsState

    override suspend fun searchMods(query: String, limit: Int): List<ModSearchHit> =
        modrinthSource?.searchMods(query, limit) ?: emptyList()

    override suspend fun fetchModVersions(slug: String, mcVersion: String?): List<ModVersionInfo> {
        val hits = searchMods(slug, limit = 1)
        val hit = hits.firstOrNull() ?: return emptyList()
        return fetchModVersions(hit, mcVersion)
    }

    override suspend fun fetchModVersions(hit: ModSearchHit, mcVersion: String?): List<ModVersionInfo> {
        val versions = modrinthSource?.versionsFor(hit.projectId) ?: emptyList()
        return if (mcVersion.isNullOrBlank()) {
            versions
        } else {
            versions.filter { mcVersion in it.gameVersions }
        }
    }

    override suspend fun install(request: ResourcePackInstallRequest): ResourcePackId = withContext(Dispatchers.IO) {
        when (request) {
            ResourcePackInstallRequest.Vanilla -> {
                requireNotNull(vanillaInstaller) { "VanillaInstaller not configured" }
                cancelAndJoin(request.id)
                val stagingDir = createStagingAssetsDir(request.id)
                launchInstall(request, stagingDir) { vanillaInstaller.install(stagingDir) }
                request.id
            }
            is ResourcePackInstallRequest.Mod -> {
                requireNotNull(modInstaller) { "ModInstaller not configured" }
                cancelAndJoin(request.id)
                val stagingDir = createStagingAssetsDir(request.id)
                launchInstall(request, stagingDir) { modInstaller.install(request.hit, request.version, stagingDir) }
                request.id
            }
        }
    }

    override suspend fun installVanilla(): ResourcePackId =
        install(ResourcePackInstallRequest.Vanilla)

    override suspend fun installMod(hit: ModSearchHit, version: ModVersionInfo): ResourcePackId {
        return install(ResourcePackInstallRequest.Mod(hit, version))
    }

    override suspend fun reinstall(id: ResourcePackId) = withContext(Dispatchers.IO) {
        when (id) {
            ResourcePackId.Vanilla -> installVanilla()
            else -> {
                val current = dao.get(id.value) ?: return@withContext
                val versions = modrinthSource?.versionsFor(current.projectSlug).orEmpty()
                val version = versions.firstOrNull { it.id == current.sourceVersionId }
                    ?: versions.firstOrNull { it.name == current.versionName }
                    ?: versions.firstOrNull()
                    ?: throw IllegalStateException("No downloadable version for ${current.projectSlug}")
                val hit = ModSearchHit(
                    slug = current.projectSlug.ifBlank { id.modSlug },
                    title = current.displayName,
                    description = "",
                    projectId = current.projectSlug.ifBlank { id.modSlug },
                )
                installMod(hit, version)
            }
        }
        Unit
    }

    override suspend fun delete(id: ResourcePackId) = withContext(Dispatchers.IO) {
        cancelAndJoin(id)
        removeReplacedAssets(id)
        dao.delete(id.value)
        LangReader.clearCache()
    }

    override suspend fun deleteAll() = withContext(Dispatchers.IO) {
        val jobs = activeJobs.keys.toList().mapNotNull { id ->
            activeJobs.remove(id)?.also {
                it.cancel()
                activeInstallsState.update { installs -> installs - id }
            }
        }
        jobs.joinAll()
        runCatching { File(context.filesDir, "blockprintcat/render_assets").deleteRecursively() }
        dao.clearAll()
        LangReader.clearCache()
    }

    override fun cancel(id: ResourcePackId) {
        activeJobs.remove(id)?.cancel()
        activeInstallsState.update { it - id }
    }

    private suspend fun cancelAndJoin(id: ResourcePackId) {
        val job = activeJobs.remove(id)
        activeInstallsState.update { it - id }
        job?.cancel()
        job?.join()
    }

    /** Assets are stored by namespace, so cleanup must use the recorded ownership rather than the project slug. */
    private suspend fun removeReplacedAssets(id: ResourcePackId) {
        val current = dao.get(id.value) ?: return
        val otherNamespaces = dao.observeAll()
            .first()
            .filterNot { it.id == id.value }
            .flatMap { it.namespaces.split(',') }
            .filter { it.isNotBlank() }
            .toSet()
        current.namespaces
            .split(',')
            .filter { it.isNotBlank() && it !in otherNamespaces }
            .forEach { namespace ->
                runCatching {
                    File(context.filesDir, "blockprintcat/render_assets/$namespace").deleteRecursively()
                }
            }
    }

    private fun launchInstall(
        request: ResourcePackInstallRequest,
        stagingDir: File,
        source: () -> Flow<PackProgress>,
    ) {
        val id = request.id
        cancel(id)
        Log.i(TAG, "install start id=${id.value} name=${request.displayName} source=${request.source}")
        val progressState = MutableStateFlow<PackProgress>(PackProgress.Idle)
        val carrier = ActiveInstall(
            packId = id,
            displayName = request.displayName,
            fileName = request.fileName,
            totalSize = request.totalSize,
            source = request.source,
            progress = progressState.asStateFlow(),
        )
        val job = scope.launch {
            var lastProgressLogKey: String? = null
            try {
                source().collect { progress ->
                    progressState.value = progress
                    val progressLogKey = progress.logKey()
                    if (progressLogKey != lastProgressLogKey) {
                        Log.i(TAG, "install progress id=${id.value} ${progress.logMessage()}")
                        lastProgressLogKey = progressLogKey
                    }
                    when (progress) {
                        is PackProgress.Done -> {
                            commitStagedAssets(id, stagingDir, progress.entry.namespaces)
                            LangReader.clearCache()
                            dao.upsert(
                                progress.entry.toEntity(
                                    source = request.source.name.lowercase(),
                                    sourceVersionId = request.sourceVersionId,
                                ),
                            )
                            Log.i(TAG, "install done id=${id.value} files=${progress.entry.fileCount}")
                            activeInstallsState.update { it - id }
                        }
                        is PackProgress.Failed -> {
                            Log.e(TAG, "install failed id=${id.value} message=${progress.message}")
                            stagingDir.deleteRecursively()
                            delay(TERMINAL_PROGRESS_RETENTION_MS)
                            activeInstallsState.update { it - id }
                        }
                        PackProgress.Cancelled -> {
                            Log.i(TAG, "install cancelled id=${id.value}")
                            activeInstallsState.update { it - id }
                        }
                        else -> Unit
                    }
                }
            } catch (t: CancellationException) {
                Log.i(TAG, "install coroutine cancelled id=${id.value}")
                progressState.value = PackProgress.Cancelled
                stagingDir.deleteRecursively()
                activeInstallsState.update { it - id }
                throw t
            } catch (t: Throwable) {
                Log.e(TAG, "install coroutine failed id=${id.value}", t)
                progressState.value = PackProgress.Failed(t.message ?: "install failed")
                stagingDir.deleteRecursively()
                delay(TERMINAL_PROGRESS_RETENTION_MS)
                activeInstallsState.update { it - id }
            } finally {
                stagingDir.deleteRecursively()
                activeJobs.remove(id, coroutineContext[Job])
            }
        }
        activeJobs[id] = job
        activeInstallsState.update { it + (id to carrier) }
    }

    private fun createStagingAssetsDir(id: ResourcePackId): File {
        val safeId = id.value.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(
            context.filesDir,
            "blockprintcat/render_assets_staging/${safeId}_${System.currentTimeMillis()}",
        ).apply {
            deleteRecursively()
            mkdirs()
        }
    }

    private suspend fun commitStagedAssets(
        id: ResourcePackId,
        stagingDir: File,
        namespaces: Set<String>,
    ) {
        val renderRoot = File(context.filesDir, "blockprintcat/render_assets").apply { mkdirs() }
        val otherNamespaces = dao.observeAll()
            .first()
            .filterNot { it.id == id.value }
            .flatMap { it.namespaces.split(',') }
            .filter { it.isNotBlank() }
            .toSet()
        val nextNamespaces = namespaces.filter { it.isNotBlank() }.toSet()

        val current = dao.get(id.value)
        current?.namespaces
            ?.split(',')
            ?.filter { it.isNotBlank() && it !in otherNamespaces && it !in nextNamespaces }
            ?.forEach { namespace ->
                runCatching {
                    File(renderRoot, namespace).deleteRecursively()
                }
            }

        stagingDir.listFiles()
            ?.filter { it.isDirectory && it.name in nextNamespaces }
            ?.forEach { namespaceDir ->
                val destination = File(renderRoot, namespaceDir.name)
                if (namespaceDir.name !in otherNamespaces) {
                    destination.deleteRecursively()
                }
                destination.mkdirs()
                namespaceDir.copyRecursively(destination, overwrite = true)
            }
    }

    private fun PackProgress.logKey(): String = when (this) {
        PackProgress.Idle -> "idle"
        is PackProgress.Preparing -> "preparing:$label"
        is PackProgress.FetchingManifest -> "manifest:$label"
        is PackProgress.Downloading -> "downloading:$fileName:${((fraction.coerceIn(0f, 1f)) * 10).toInt()}"
        is PackProgress.Installing -> {
            val bucket = fraction?.let { (it.coerceIn(0f, 1f) * 10).toInt() }
                ?: (installedFiles / 250)
            "installing:$bucket"
        }
        is PackProgress.Extracting -> "extracting:$extracted"
        is PackProgress.Failed -> "failed:$message"
        PackProgress.Cancelled -> "cancelled"
        is PackProgress.Done -> "done:${entry.fileCount}"
    }

    private fun PackProgress.logMessage(): String = when (this) {
        PackProgress.Idle -> "idle"
        is PackProgress.Preparing -> "preparing label=$label"
        is PackProgress.FetchingManifest -> "fetchingManifest label=$label"
        is PackProgress.Downloading -> {
            val percent = (fraction.coerceIn(0f, 1f) * 100).toInt()
            "downloading file=$fileName percent=$percent bytes=$bytesRead/$totalBytes"
        }
        is PackProgress.Installing -> "installing label=$label fraction=$fraction files=$installedFiles/$totalFiles"
        is PackProgress.Extracting -> "extracting path=$currentPath files=$extracted"
        is PackProgress.Failed -> "failed message=$message"
        PackProgress.Cancelled -> "cancelled"
        is PackProgress.Done -> "done files=${entry.fileCount} size=${entry.totalSize}"
    }
}
