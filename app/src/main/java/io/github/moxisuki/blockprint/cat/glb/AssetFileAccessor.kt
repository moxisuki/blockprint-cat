package io.github.moxisuki.blockprint.cat.glb

import android.content.res.AssetManager
import io.github.moxisuki.blockprint.core.glb.FileAccessor

class AssetFileAccessor(
    private val assetManager: AssetManager,
) : FileAccessor {

    override fun readBytes(relPath: String): ByteArray? {
        return try {
            assetManager.open(relPath).use { it.readBytes() }
        } catch (_: java.io.IOException) {
            null
        }
    }

    override fun exists(relPath: String): Boolean {
        return try {
            assetManager.open(relPath).close()
            true
        } catch (_: java.io.IOException) {
            false
        }
    }
}
