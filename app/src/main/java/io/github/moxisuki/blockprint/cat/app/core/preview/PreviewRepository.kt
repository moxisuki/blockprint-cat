package io.github.moxisuki.blockprint.cat.app.core.preview

import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintRepository
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackAssetLocator
import io.github.moxisuki.blockprint.core.api.BlockPrintToGlb
import io.github.moxisuki.blockprint.core.glb.writer.GlbExportOptions
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext

data class PreviewModel(
    val file: File,
    val title: String,
    val fromCache: Boolean,
    val missingNamespaces: Set<String> = emptySet(),
)

enum class PreviewCacheStatus {
    Missing,
    Ready,
}

data class PreviewCacheInfo(
    val status: PreviewCacheStatus = PreviewCacheStatus.Missing,
    val sizeBytes: Long = 0L,
) {
    val isReady: Boolean
        get() = status == PreviewCacheStatus.Ready
}

enum class PreviewStage {
    Preparing,
    Reading,
    Generating,
    LoadingModel,
    Ready,
}

data class PreviewProgress(
    val stage: PreviewStage,
    val fraction: Float,
)

@Singleton
class PreviewRepository @Inject constructor(
    private val blueprintRepository: BlueprintRepository,
    private val resourcePackAssetLocator: ResourcePackAssetLocator,
    private val blueprintDao: io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) {
    private val cacheChanges = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun observeCacheStatus(blueprintId: String): Flow<PreviewCacheInfo> {
        val refreshes = merge(
            flowOf(Unit),
            cacheChanges.filter { it == blueprintId }.map { Unit },
        )
        return combine(
            blueprintDao.observeBlueprint(blueprintId),
            refreshes,
        ) { blueprint, _ -> cacheInfo(blueprint) }
            .distinctUntilChanged()
    }

    suspend fun prepare(
        blueprintId: String,
        forceRegenerate: Boolean = false,
        onProgress: (PreviewProgress) -> Unit,
    ): PreviewModel = withContext(Dispatchers.IO) {
        val progress = PreviewProgressEmitter(onProgress)
        progress.emit(PreviewStage.Preparing, 0f, force = true)
        val blueprint = blueprintDao.getBlueprint(blueprintId)
            ?: throw NoSuchElementException("Blueprint not found: $blueprintId")
        progress.emit(PreviewStage.Reading, 0.02f, force = true)
        val document = blueprintRepository.loadBlueprintDocument(blueprintId)
        val missingNamespaces = resourcePackAssetLocator.missingNamespacesFor(document)
        val output = cacheFile(
            blueprintId = blueprintId,
            lastModifiedAt = blueprint.lastModifiedAt,
            sizeBytes = blueprint.sizeBytes,
        )
        if (!forceRegenerate && output.isFile && output.length() >= MIN_VALID_GLB_BYTES) {
            progress.emit(PreviewStage.LoadingModel, 1f, force = true)
            return@withContext PreviewModel(
                file = output,
                title = blueprint.displayName,
                fromCache = true,
                missingNamespaces = missingNamespaces,
            )
        }

        output.parentFile?.mkdirs()
        val temp = File(output.parentFile, "${output.name}.tmp")
        if (temp.exists()) temp.delete()
        try {
            progress.emit(PreviewStage.Generating, 0f, force = true)
            temp.outputStream().use { stream ->
                BlockPrintToGlb.convert(
                    document,
                    listOf(assetRoot().toPath()),
                    stream,
                    0,
                    GlbExportOptions(floorHeight = 1),
                    { value ->
                        progress.emit(PreviewStage.Generating, value)
                    },
                )
            }
            progress.emit(PreviewStage.Generating, 1f, force = true)
            check(temp.length() >= MIN_VALID_GLB_BYTES) { "Generated preview model is empty" }
            check(temp.renameTo(output)) { "Cannot commit preview model" }
        } finally {
            if (temp.exists()) temp.delete()
        }
        progress.emit(PreviewStage.LoadingModel, 1f, force = true)
        cacheChanges.emit(blueprintId)
        PreviewModel(
            file = output,
            title = blueprint.displayName,
            fromCache = false,
            missingNamespaces = missingNamespaces,
        )
    }

    fun cacheRoot(): File = File(context.filesDir, CACHE_ROOT)

    private fun cacheInfo(
        blueprint: io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintEntity?,
    ): PreviewCacheInfo {
        if (blueprint == null) return PreviewCacheInfo()
        val output = cacheFile(
            blueprintId = blueprint.id,
            lastModifiedAt = blueprint.lastModifiedAt,
            sizeBytes = blueprint.sizeBytes,
        )
        return if (output.isFile && output.length() >= MIN_VALID_GLB_BYTES) {
            PreviewCacheInfo(
                status = PreviewCacheStatus.Ready,
                sizeBytes = output.length(),
            )
        } else {
            PreviewCacheInfo()
        }
    }

    private fun cacheFile(blueprintId: String, lastModifiedAt: Long, sizeBytes: Long): File {
        val safeId = blueprintId.replace(UNSAFE_ID, "_")
        return File(cacheRoot(), "${safeId}_${lastModifiedAt}_${sizeBytes}.glb")
    }

    private fun assetRoot(): File = resourcePackAssetLocator.assetsRootDir()

    private class PreviewProgressEmitter(
        private val callback: (PreviewProgress) -> Unit,
        private val minIntervalNanos: Long = 50_000_000L,
        private val minFractionDelta: Float = 0.01f,
    ) {
        private var lastStage: PreviewStage? = null
        private var lastFraction = -1f
        private var lastEmitNanos = 0L

        fun emit(stage: PreviewStage, fraction: Float, force: Boolean = false) {
            val value = fraction.coerceIn(0f, 1f)
            val now = System.nanoTime()
            val stageChanged = stage != lastStage
            if (!force && !stageChanged &&
                lastEmitNanos != 0L &&
                now - lastEmitNanos < minIntervalNanos &&
                value - lastFraction < minFractionDelta
            ) {
                return
            }
            lastStage = stage
            lastFraction = value
            lastEmitNanos = now
            callback(PreviewProgress(stage, value))
        }
    }

    private companion object {
        const val CACHE_ROOT = "blockprintcat/glb_cache"
        const val MIN_VALID_GLB_BYTES = 200L
        val UNSAFE_ID = Regex("[^a-zA-Z0-9._-]")
    }
}
