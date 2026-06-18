package io.github.moxisuki.blockprint.cat.glb

import java.io.File

class GlbCache(private val cacheDir: File) {

    companion object {
        private const val MAX_SIZE_BYTES = 200L * 1024 * 1024  // 200 MB
    }

    init {
        cacheDir.mkdirs()
    }

    fun get(key: String, regionIndex: Int = 0, floorHeight: Int = 0): ByteArray? {
        val file = cacheFile(key, regionIndex, floorHeight)
        return if (file.isFile) file.readBytes() else null
    }

    fun put(key: String, regionIndex: Int, bytes: ByteArray, floorHeight: Int = 0) {
        val file = cacheFile(key, regionIndex, floorHeight)
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeBytes(bytes)
        tmp.renameTo(file)
        evictIfNeeded()
    }

    fun getFile(key: String, regionIndex: Int = 0, floorHeight: Int = 0): File =
        cacheFile(key, regionIndex, floorHeight)

    private fun cacheFile(key: String, regionIndex: Int, floorHeight: Int): File {
        val safe = key.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val suffix = if (floorHeight > 0) "_fh${floorHeight}" else ""
        return File(cacheDir, "${safe}_r${regionIndex}${suffix}.glb")
    }

    fun size(): Long = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L

    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /** 清除指定 key 的所有 region 缓存文件 */
    fun clear(key: String) {
        val prefix = key.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        cacheDir.listFiles()?.filter { it.name.startsWith("${prefix}_r") }?.forEach { it.delete() }
    }

    private fun evictIfNeeded() {
        var totalSize = size()
        if (totalSize <= MAX_SIZE_BYTES) return
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        for (file in files) {
            if (totalSize <= MAX_SIZE_BYTES * 0.8) break
            totalSize -= file.length()
            file.delete()
        }
    }
}
