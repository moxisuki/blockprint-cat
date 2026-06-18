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

    companion object {
        const val LAYER_FLOOR_HEIGHT = 1
        private const val TAG = "GlbGenerator"
        private fun log(msg: String) = println("[$TAG] $msg")
    }

    fun generate(
        litematic: Litematic,
        cacheKey: String,
        regionIndex: Int = 0,
        floorHeight: Int = 0,
        onProgress: ((Float) -> Unit)? = null,
    ): ByteArray {
        cache.get(cacheKey, regionIndex, floorHeight)?.let {
            log("缓存命中: $cacheKey r$regionIndex fh$floorHeight, ${it.size} bytes")
            return it
        }
        log("缓存未命中, 开始生成: $cacheKey r$regionIndex fh$floorHeight")
        val t0 = System.currentTimeMillis()
        val options = if (floorHeight > 0) GlbExportOptions(floorHeight = floorHeight)
                      else GlbExportOptions()
        val bytes = LitematicToGlb.convertToBytes(
            litematic, assetsDirs, regionIndex,
            imageBackend = imageBackend,
            onProgress = onProgress,
            options = options,
        )
        val elapsed = System.currentTimeMillis() - t0
        log("GLB 生成完成: ${bytes.size} bytes, 耗时 ${elapsed}ms")
        cache.put(cacheKey, regionIndex, bytes, floorHeight)
        return bytes
    }

    fun getOrGenerateFile(litematic: Litematic, cacheKey: String, regionIndex: Int = 0): File {
        val file = cache.getFile(cacheKey, regionIndex)
        if (file.isFile) {
            log( "缓存文件命中: ${file.absolutePath}, ${file.length()} bytes")
        } else {
            log( "缓存文件未命中, 生成中: $cacheKey")
            val t0 = System.currentTimeMillis()
            val bytes = LitematicToGlb.convertToBytes(litematic, assetsDirs, regionIndex, imageBackend = imageBackend)
            val elapsed = System.currentTimeMillis() - t0
            log( "GLB 生成完成: ${bytes.size} bytes, 耗时 ${elapsed}ms")
            cache.put(cacheKey, regionIndex, bytes)
        }
        return file
    }

    fun hasCache(cacheKey: String, regionIndex: Int = 0, floorHeight: Int = 0): Boolean =
        cache.get(cacheKey, regionIndex, floorHeight) != null

    fun clearCache(key: String) = cache.clear(key)

    fun clearAllCache() = cache.clear()
}
