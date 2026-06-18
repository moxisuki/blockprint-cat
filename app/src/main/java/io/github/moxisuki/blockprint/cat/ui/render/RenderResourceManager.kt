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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

import io.github.moxisuki.blockprint.core.Litematic
import io.github.moxisuki.blockprint.cat.glb.FileSystemFileAccessor
import io.github.moxisuki.blockprint.cat.glb.GlbCache
import io.github.moxisuki.blockprint.cat.glb.GlbGenerator

/**
 * TODO: Render refactor — network/download/rendering code was removed from litematic-lib.
 * All methods now return mock values. Re-implement rendering in the app layer.
 */
object RenderResourceManager {

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

    /** 缓存：Detail 页传 Litematic 给 PreviewScreen */
    private var cachedLitematic: Litematic? = null
    private var cachedLitematicKey: String = ""

    fun putLitematic(key: String, lit: Litematic) {
        cachedLitematic = lit
        cachedLitematicKey = key
    }

    fun takeLitematic(key: String): Litematic? {
        if (cachedLitematicKey == key) {
            val lit = cachedLitematic
            cachedLitematic = null
            cachedLitematicKey = ""
            return lit
        }
        return null
    }

    /** GLB 缓存：Detail 页预生成 GLB + 模型包围盒，PreviewScreen 直接渲染 */
    private var cachedGlb: ByteArray? = null
    private var cachedGlbKey: String = ""
    private var cachedGlbMinY: Float = 0f
    private var cachedGlbCenterX: Float = 0f
    private var cachedGlbCenterZ: Float = 0f

    // 持久化的 GLB 缓存键集合（跨进程保留，Room 持久化）
    private var glbCacheDao: io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao? = null
    private val glbScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _cachedKeys = MutableStateFlow<Set<String>>(emptySet())
    /** 已生成 GLB 的蓝图 uuid 集合，持久化 + 跨进程，UI 可订阅 */
    val cachedKeys: StateFlow<Set<String>> = _cachedKeys.asStateFlow()

    fun putGlb(key: String, bytes: ByteArray, minY: Float = 0f, centerX: Float = 0f, centerZ: Float = 0f) {
        cachedGlb = bytes
        cachedGlbKey = key
        cachedGlbMinY = minY
        cachedGlbCenterX = centerX
        cachedGlbCenterZ = centerZ
        val dao = glbCacheDao ?: return
        glbScope.launch {
            dao.upsert(
                io.github.moxisuki.blockprint.cat.data.render.GlbCacheEntity(
                    uuid = key,
                    sizeBytes = bytes.size.toLong(),
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
        _cachedKeys.value = _cachedKeys.value + key
    }

    fun hasGlb(key: String): Boolean = key in _cachedKeys.value

    fun peekGlb(key: String): GlbCacheEntry? {
        if (cachedGlbKey == key && cachedGlb != null) {
            return GlbCacheEntry(cachedGlb!!, cachedGlbMinY, cachedGlbCenterX, cachedGlbCenterZ)
        }
        return null
    }

    fun clearGlb(key: String) {
        if (cachedGlbKey == key) {
            cachedGlb = null
            cachedGlbKey = ""
            cachedGlbMinY = 0f
            cachedGlbCenterX = 0f
            cachedGlbCenterZ = 0f
        }
        val dao = glbCacheDao ?: return
        glbScope.launch { dao.delete(key) }
        _cachedKeys.value = _cachedKeys.value - key
    }

    fun clearAllGlb() {
        cachedGlb = null
        cachedGlbKey = ""
        cachedGlbMinY = 0f
        cachedGlbCenterX = 0f
        cachedGlbCenterZ = 0f
        val dao = glbCacheDao ?: return
        glbScope.launch { dao.clearAll() }
        _cachedKeys.value = emptySet()
    }

    fun takeGlb(key: String): GlbCacheEntry? {
        if (cachedGlbKey == key) {
            val e = GlbCacheEntry(cachedGlb!!, cachedGlbMinY, cachedGlbCenterX, cachedGlbCenterZ)
            cachedGlb = null
            cachedGlbKey = ""
            return e
        }
        return null
    }

    data class GlbCacheEntry(val bytes: ByteArray, val minY: Float, val centerX: Float, val centerZ: Float)

    fun init(context: Context, dao: io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao) {
        Log.d(TAG, "初始化渲染资源管线...")
        applicationContext = context.applicationContext
        glbCacheDao = dao

        // 加载持久化的 GLB 缓存键（Room）
        runCatching {
            kotlinx.coroutines.runBlocking {
                val all = dao.getAll()
                _cachedKeys.value = all.map { it.uuid }.toSet()
                Log.d(TAG, "已恢复 GLB 缓存键: ${all.size} 个")
            }
        }

        val renderAssetsDir = File(context.filesDir, "blockprintcat/render_assets")
        // 使用已下载的渲染资源（不再内置 APK assets）
        val accessor = FileSystemFileAccessor(renderAssetsDir)
        Log.d(TAG, "使用渲染资源目录: $renderAssetsDir (已下载=${renderAssetsDir.isDirectory})")
        fileAccessor = AndroidFileAccessor(context, baseDir = renderAssetsDir)

        // Initialize GLB pipeline
        val cacheDir = File(context.filesDir, "glb_cache")
        _generator = GlbGenerator(listOf(renderAssetsDir.toPath()), GlbCache(cacheDir))

        // Verify asset access works
        val testPath = "minecraft/textures/block/stone.png"
        val testBytes = accessor.readBytes(testPath)
        Log.d(TAG, "资源测试: $testPath -> ${if (testBytes != null) "${testBytes.size} bytes" else "未找到"}")

        _state.value = ResourceState(
            resourcesDir = renderAssetsDir,
            builtinReady = true,
        )
        Log.d(TAG, "渲染资源管线初始化完成, generator=${_generator != null}, accessor=${accessor.javaClass.simpleName}")
    }

    private const val TAG = "RenderResourceMgr"

    // TODO: Re-implement vanilla asset download + install
    suspend fun installVanilla(
        versionId: String = "latest",
    ): String = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException("TODO: render pipeline not yet rebuilt")
    }

    // TODO: Re-implement uninstall
    suspend fun uninstall(versionId: String): Boolean = false

    // TODO: Re-implement mod asset merging
    fun installModAssets(namespace: String, version: String = "") {
        // no-op: rendering not yet rebuilt
    }

    // TODO: Re-implement vanilla reset
    suspend fun resetVanilla(versionId: String) {
        _state.value = _state.value.copy(vanillaInstalled = false, resolverReady = false)
    }

    fun listInstalled(): List<Map<String, String>> = emptyList()

    fun iconFor(blockName: String, targetPx: Int): Bitmap? = null

    fun isInstalled(): Boolean = false
    fun available(): Boolean = false
    fun resourcesDir(): File? = _state.value.resourcesDir
}
