package io.github.moxisuki.blockprint.cat.glb

import java.io.File

class GlbCache(private val cacheDir: File) {

    companion object {
        private const val MAX_SIZE_BYTES = 200L * 1024 * 1024  // 200 MB
    }

    init {
        cacheDir.mkdirs()
    }

    fun getFile(key: GlbGenerator.Key): File = cacheFile(key)

    fun clear(key: GlbGenerator.Key) {
        val prefix = key.blueprintUuid.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        cacheDir.listFiles()?.filter { it.name.startsWith("${prefix}_r") }?.forEach { it.delete() }
    }

    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun size(): Long = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L

    private fun cacheFile(key: GlbGenerator.Key): File {
        val safe = key.blueprintUuid.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val suffix = if (key.floorHeight > 0) "_fh${key.floorHeight}" else ""
        return File(cacheDir, "${safe}_r${key.regionIndex}${suffix}.glb")
    }
}