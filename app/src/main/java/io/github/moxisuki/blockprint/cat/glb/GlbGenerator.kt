package io.github.moxisuki.blockprint.cat.glb

import io.github.moxisuki.blockprint.core.Litematic
import io.github.moxisuki.blockprint.core.glb.GlbExportOptions
import io.github.moxisuki.blockprint.core.glb.ImageBackend
import io.github.moxisuki.blockprint.core.glb.LitematicToGlb
import java.io.File
import java.nio.file.Path

class GlbGenerator(
    private val assetsDirs: List<Path>,
    private val cache: GlbCache,
    private val imageBackend: ImageBackend? = null,
) {

    /**
     * 一份 GLB 缓存的唯一标识。regionIndex 与 floorHeight 是历史参数,
     * 当前永远 (0, LAYER_FLOOR_HEIGHT),保留为数据类字段是为了日后扩展不破坏 API。
     */
    data class Key(
        val blueprintUuid: String,
        val regionIndex: Int = 0,
        val floorHeight: Int = LAYER_FLOOR_HEIGHT,
    )

    companion object {
        const val LAYER_FLOOR_HEIGHT = 1
        /** Files smaller than this are treated as missing/corrupt. */
        const val MIN_VALID_GLB_BYTES = 200L
        private const val TAG = "GlbGenerator"
        private fun log(msg: String) = println("[$TAG] $msg")
    }

    /** Check if a cached GLB file exists and looks valid (non-empty, ≥ MIN_VALID_GLB_BYTES). */
    fun peekCacheFile(key: Key): File? {
        val file = cache.getFile(key)
        return file.takeIf { it.isFile && it.length() >= MIN_VALID_GLB_BYTES }
    }

    fun getOrGenerateFile(
        litematic: Litematic,
        key: Key,
        onProgress: ((Float) -> Unit)? = null,
    ): File {
        val file = cache.getFile(key)
        if (file.isFile) {
            log("缓存文件命中: ${file.absolutePath}, ${file.length()} bytes")
        } else {
            log("缓存文件未命中, 生成中: ${key.blueprintUuid} r${key.regionIndex} fh${key.floorHeight}")
            val t0 = System.currentTimeMillis()
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.parentFile?.mkdirs()
            val opts = if (key.floorHeight > 0) GlbExportOptions(floorHeight = key.floorHeight) else GlbExportOptions()
            tmp.outputStream().use { out ->
                LitematicToGlb.convert(litematic, assetsDirs, out, key.regionIndex, opts, onProgress)
            }
            tmp.renameTo(file)
            val elapsed = System.currentTimeMillis() - t0
            log("GLB 生成完成: ${file.length()} bytes, 耗时 ${elapsed}ms")
        }
        return file
    }

    fun hasCache(key: Key): Boolean = cache.getFile(key).isFile

    fun clearCache(key: String) = cache.clear(Key(key))

    fun clearAllCache() = cache.clear()
}
