package io.github.moxisuki.blockprint.cat.data.community

import android.content.Context
import java.io.File

interface CommunityRepository {
    val source: CommunitySource
    suspend fun isAvailable(): Boolean
    suspend fun listCount(filter: String): Int
    suspend fun list(begin: Int, filter: String, heatSort: Boolean): List<UnifiedSchematic>
    suspend fun loadDetail(id: String): UnifiedDetail
    suspend fun loadPreview(id: String, detail: UnifiedDetail?): ByteArray?
    suspend fun downloadToFile(
        context: Context,
        schematic: UnifiedSchematic,
        detail: UnifiedDetail?,
        targetDir: File,
        onProgress: suspend (Long, Long) -> Unit,
    ): File
}
