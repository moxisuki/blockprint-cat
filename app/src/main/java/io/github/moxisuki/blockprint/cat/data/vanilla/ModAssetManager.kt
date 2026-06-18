package io.github.moxisuki.blockprint.cat.data.vanilla

import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ModAssetManager"

@Singleton
class ModAssetManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val modrinthClient: ModrinthClient,
    private val statusDao: ModAssetStatusDao,
) {
    sealed class ModState {
        data object NotInstalled : ModState()
        data class Downloading(val fileName: String, val progress: Float) : ModState()
        data class Installed(val entity: ModAssetStatusEntity) : ModState()
        data class Error(val message: String) : ModState()
    }

    data class ModInfo(
        val slug: String, val name: String, val mcVersion: String,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _states = MutableStateFlow<Map<String, ModState>>(emptyMap())
    val states: StateFlow<Map<String, ModState>> = _states.asStateFlow()

    fun isInstalled(slug: String): Boolean {
        val normalized = slug.replace('-', '_')
        val state = _states.value[slug] ?: _states.value[slug.replace('_', '-')]
        if (state is ModState.Installed) return true
        // Also check by namespace — modifiers use - in slug but _ in asset namespace
        return _states.value.any { (_, s) ->
            s is ModState.Installed && s.entity.namespaces.split(",").any { ns ->
                ns == slug || ns == normalized || ns.replace('-', '_') == normalized
            }
        }
    }

    private val assetsDir: File
        get() = File(context.filesDir, "blockprintcat/render_assets")

    init {
        // 同步读取 Room，保证 isInstalled() 首次调用就返回正确值
        kotlinx.coroutines.runBlocking {
            val entities = statusDao.getAll()
            val map = mutableMapOf<String, ModState>()
            for (e in entities) {
                map[e.projectSlug] = ModState.Installed(e)
            }
            _states.value = map
        }
    }

    fun stateFor(slug: String): ModState {
        _states.value[slug]?.let { return it }
        _states.value[slug.replace('_', '-')]?.let { return it }
        return _states.value.entries.firstOrNull { (_, s) ->
            s is ModState.Installed && s.entity.namespaces.split(",").any { ns ->
                ns == slug || ns.replace('-', '_') == slug.replace('-', '_')
            }
        }?.value ?: ModState.NotInstalled
    }

    suspend fun searchMods(query: String): List<ModrinthClient.SearchResult> = withContext(Dispatchers.IO) {
        modrinthClient.searchMods(query.ifBlank { "create" })
    }

    suspend fun fetchVersions(slug: String): List<ModrinthClient.ModVersion> = withContext(Dispatchers.IO) {
        val projectId = modrinthClient.searchProject(slug) ?: throw IllegalStateException("未找到项目: $slug")
        modrinthClient.getAllVersions(projectId).take(20)
    }

    fun install(info: ModInfo) {
        if (_states.value[info.slug] is ModState.Downloading) return
        scope.launch {
            try {
                _states.value = _states.value + (info.slug to ModState.Downloading("搜索中…", 0f))
                val projectId = modrinthClient.searchProject(info.slug)
                    ?: throw IllegalStateException("未找到项目: ${info.slug}")
                val versions = modrinthClient.getVersions(projectId, info.mcVersion)
                if (versions.isEmpty()) throw IllegalStateException("未找到 ${info.mcVersion} 兼容版本")
                val ver = versions.first()
                _states.value = _states.value + (info.slug to ModState.Downloading(ver.fileName, 0f))
                val result = modrinthClient.downloadAndExtractAssets(
                    fileUrl = ver.fileUrl, fileName = ver.fileName,
                    destDir = assetsDir, assetNamespace = info.slug,
                    onProgress = { p ->
                        _states.value = _states.value + (info.slug to ModState.Downloading(ver.fileName, p))
                    }
                )
                val extractedNs = result.namespaces
                var totalSize = 0L
                for (ns in extractedNs) {
                    val nsDir = File(assetsDir, ns)
                    if (nsDir.isDirectory) totalSize += nsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                }
                val entity = ModAssetStatusEntity(
                    projectSlug = info.slug, projectName = info.name,
                    versionName = ver.name, mcVersion = info.mcVersion,
                    fileCount = result.fileCount, totalSize = totalSize,
                    installedAt = System.currentTimeMillis(),
                    namespaces = extractedNs.joinToString(","),
                )
                statusDao.upsert(entity)
                _states.value = _states.value + (info.slug to ModState.Installed(entity))
                if (result.fileCount == 0) {
                    Log.w(TAG, "Installed ${info.slug} has no resource assets — 不含资源包")
                } else {
                    Log.i(TAG, "Installed ${info.slug}: ${result.fileCount} files, $totalSize bytes, namespaces: ${extractedNs.joinToString(", ")}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Install failed: ${info.slug}", e)
                _states.value = _states.value + (info.slug to ModState.Error(e.message ?: "下载失败"))
            }
        }
    }

    fun delete(slug: String) {
        scope.launch {
            File(assetsDir, slug).deleteRecursively()
            statusDao.delete(slug)
            _states.value = _states.value - slug
            Log.i(TAG, "Deleted: $slug")
        }
    }

}
