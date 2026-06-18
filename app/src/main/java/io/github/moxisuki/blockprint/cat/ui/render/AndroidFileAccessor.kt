package io.github.moxisuki.blockprint.cat.ui.render

import android.content.Context
import java.io.File

/**
 * TODO: Render refactor — rendering/network code was removed from litematic-lib.
 * This class previously implemented FileAccessor for the lib's asset pipeline.
 * Re-implement when rendering is rebuilt in the app layer.
 */
class AndroidFileAccessor(
    private val context: Context,
    private val baseDir: File = context.filesDir,
) {
    private fun resolve(path: String): File {
        val f = File(baseDir, path)
        f.parentFile?.mkdirs()
        return f
    }

    fun writeBytes(path: String, bytes: ByteArray) {
        resolve(path).writeBytes(bytes)
    }

    fun readBytes(path: String): ByteArray? =
        resolve(path).takeIf { it.isFile }?.readBytes()

    fun exists(path: String): Boolean = resolve(path).exists()

    fun size(path: String): Long? =
        resolve(path).takeIf { it.isFile }?.length()

    fun delete(path: String): Boolean = resolve(path).deleteRecursively()

    fun mkdirs(path: String): Boolean = resolve(path).mkdirs()
}
