package io.github.moxisuki.blockprint.cat.glb

import io.github.moxisuki.blockprint.core.glb.FileAccessor
import java.io.File

/**
 * Reads assets from the downloaded render_assets/ directory.
 * Core paths like "assets/minecraft/models/block/stone.json" →
 * filesystem "render_assets/minecraft/models/block/stone.json".
 */
private const val TAG_FSA = "FileSystemAccessor"

class FileSystemFileAccessor(private val rootDir: File) : FileAccessor {

    override fun readBytes(relPath: String): ByteArray? {
        var file = File(rootDir, relPath)
        if (!file.isFile) {
            val stripped = relPath.removePrefix("assets/")
            file = File(rootDir, stripped)
            if (file.isFile) android.util.Log.d(TAG_FSA, "stripped: $relPath → $stripped (${file.length()}B)")
        }
        return if (file.isFile) file.readBytes() else {
            // Log first few misses per prefix to diagnose
            if (relPath.hashCode() % 100 == 0) android.util.Log.w(TAG_FSA, "miss: $relPath (root=$rootDir)")
            null
        }
    }

    override fun exists(relPath: String): Boolean {
        return File(rootDir, relPath).isFile || File(rootDir, relPath.removePrefix("assets/")).isFile
    }
}
