package io.github.moxisuki.blockprint.cat.ui.render

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

import io.github.moxisuki.blockprint.core.Litematic
import io.github.moxisuki.blockprint.cat.glb.FileSystemFileAccessor
import io.github.moxisuki.blockprint.cat.glb.GlbCache
import io.github.moxisuki.blockprint.cat.glb.GlbGenerator

object GlbResourceManager {

    /**
     * Per-session metadata for a generated GLB. Held in memory only;
     * for UI subscription across cold start, see [cachedKeys].
     */
    data class CachedGlb(
        val blueprintUuid: String,
        val cacheFile: File,
        val minY: Float,
        val centerX: Float,
        val centerZ: Float,
    )

    data class ResourceState(
        val vanillaInstalled: Boolean = false,
        val vanillaVersion: String = "",
        val vanillaDate: String = "",
        val resolverReady: Boolean = false,
        val resourcesDir: File? = null,
        val i18nVersion: Int = 0,
        val resolverVersion: Int = 0,
        val builtinReady: Boolean = false,
    )

    private val _state = MutableStateFlow(ResourceState())
    val state: StateFlow<ResourceState> = _state.asStateFlow()

    private var fileAccessor: AndroidFileAccessor? = null
    val accessor: AndroidFileAccessor? get() = fileAccessor

    private var _generator: GlbGenerator? = null
    val generator: GlbGenerator?
        get() = _generator

    val activeMods: Set<String> get() = emptySet()
    val modVersions: Map<String, String> get() = emptyMap()

    fun assetsForRead(): Any? = null

    fun modVersion(namespace: String): String? = null

    fun getDisplayName(blockName: String, langCode: String = "zh_cn"): String =
        blockName.removePrefix("minecraft:")

    private var applicationContext: Context? = null

    /** Litematic handed from DetailScreen to PreviewScreen so we don't re-parse NBT. */
    private var pendingLitematic: Litematic? = null
    private var pendingLitematicKey: String = ""

    fun transferLitematic(key: String, lit: Litematic) {
        pendingLitematic = lit
        pendingLitematicKey = key
    }

    fun receiveLitematic(key: String): Litematic? {
        if (pendingLitematicKey == key) {
            val lit = pendingLitematic
            pendingLitematic = null
            pendingLitematicKey = ""
            return lit
        }
        return null
    }

    /**
     * Tier 1: UUID set mirrored from Room. Used by DetailScreen's
     * "View Cached" button to know which blueprints have a cached GLB.
     */
    private val _cachedKeys = MutableStateFlow<Set<String>>(emptySet())
    val cachedKeys: StateFlow<Set<String>> = _cachedKeys.asStateFlow()

    /**
     * Tier 2: per-session metadata (current process only). Used by
     * PreviewScreen Segment 1 to skip disk I/O when we already loaded
     * this blueprint in the same session.
     */
    private val sessionCache = mutableMapOf<String, CachedGlb>()

    fun peek(uuid: String): CachedGlb? = sessionCache[uuid]

    fun hasGlb(uuid: String): Boolean = uuid in _cachedKeys.value

    fun putGlb(uuid: String, cacheFile: File, minY: Float, centerX: Float, centerZ: Float) {
        sessionCache[uuid] = CachedGlb(uuid, cacheFile, minY, centerX, centerZ)
        _cachedKeys.update { it + uuid }
        val dao = glbCacheDao ?: return
        glbScope.launch {
            dao.upsert(
                io.github.moxisuki.blockprint.cat.data.render.GlbCacheEntity(
                    uuid = uuid,
                    sizeBytes = cacheFile.length(),
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun clearGlb(uuid: String) {
        sessionCache.remove(uuid)
        _cachedKeys.update { it - uuid }
        val dao = glbCacheDao ?: return
        glbScope.launch { dao.delete(uuid) }
    }

    fun clearAllGlb() {
        sessionCache.clear()
        _cachedKeys.value = emptySet()
        val dao = glbCacheDao ?: return
        glbScope.launch { dao.clearAll() }
    }

    private var glbCacheDao: io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao? = null
    private val glbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context, dao: io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao) {
        Log.d(TAG, "初始化 GLB 资源管线...")
        applicationContext = context.applicationContext
        glbCacheDao = dao

        runBlocking {
            val all = dao.getAll()
            _cachedKeys.value = all.map { it.uuid }.toSet()
            Log.d(TAG, "已恢复 GLB 缓存键: ${all.size} 个")
        }

        val renderAssetsDir = File(context.filesDir, "blockprintcat/render_assets")
        val accessor = FileSystemFileAccessor(renderAssetsDir)
        Log.d(TAG, "使用渲染资源目录: $renderAssetsDir (已下载=${renderAssetsDir.isDirectory})")
        fileAccessor = AndroidFileAccessor(context, baseDir = renderAssetsDir)

        val cacheDir = File(context.filesDir, "glb_cache")
        _generator = GlbGenerator(listOf(renderAssetsDir.toPath()), GlbCache(cacheDir))

        val testPath = "minecraft/textures/block/stone.png"
        val testBytes = accessor.readBytes(testPath)
        Log.d(TAG, "资源测试: $testPath -> ${if (testBytes != null) "${testBytes.size} bytes" else "未找到"}")

        _state.value = ResourceState(
            resourcesDir = renderAssetsDir,
            builtinReady = true,
        )
        Log.d(TAG, "GLB 资源管线初始化完成, generator=${_generator != null}, accessor=${accessor.javaClass.simpleName}")
    }

    private const val TAG = "GlbResourceMgr"

    suspend fun installVanilla(versionId: String = "latest"): String = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException("TODO: render pipeline not yet rebuilt")
    }

    suspend fun uninstallVanilla(): Boolean = false

    fun installModAssets(namespace: String, version: String = "") {}

    suspend fun resetVanilla(versionId: String) {
        _state.value = _state.value.copy(vanillaInstalled = false, resolverReady = false)
    }

    fun listInstalled(): List<Map<String, String>> = emptyList()

    fun iconFor(blockName: String, targetPx: Int): Bitmap? = null

    fun isInstalled(): Boolean = false
    fun available(): Boolean = false
    fun resourcesDir(): File? = _state.value.resourcesDir
}
