package io.github.moxisuki.blockprint.cat.app.core.cache

import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintDao
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.IconIndexResolver
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackRepository
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.LangReader
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppCacheCategory {
    BlueprintMetadata,
    RenderResources,
    PreviewModels,
    Temporary,
    All,
}

data class AppCacheStats(
    val blueprintRecordCount: Int = 0,
    val blueprintStorageBytes: Long = 0L,
    val renderFileCount: Int = 0,
    val renderStorageBytes: Long = 0L,
    val previewModelFileCount: Int = 0,
    val previewModelStorageBytes: Long = 0L,
    val temporaryFileCount: Int = 0,
    val temporaryStorageBytes: Long = 0L,
) {
    val totalStorageBytes: Long
        get() = blueprintStorageBytes + renderStorageBytes +
            previewModelStorageBytes + temporaryStorageBytes
}

interface AppCacheManager {
    suspend fun inspect(): AppCacheStats
    suspend fun clear(category: AppCacheCategory)
}

@Singleton
class DefaultAppCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blueprintDao: BlueprintDao,
    private val iconIndexResolver: IconIndexResolver,
    private val resourcePackRepository: ResourcePackRepository,
) : AppCacheManager {

    override suspend fun inspect(): AppCacheStats = withContext(Dispatchers.IO) {
        val databaseFiles = databaseFiles()
        val renderFiles = filesIn(renderRoot())
        val previewModelFiles = filesIn(previewCacheRoot())
        val temporaryFiles = filesIn(context.cacheDir) + filesIn(renderStagingRoot())
        AppCacheStats(
            blueprintRecordCount = blueprintDao.countBlueprints(),
            blueprintStorageBytes = databaseFiles.sumOf(File::length),
            renderFileCount = renderFiles.size,
            renderStorageBytes = renderFiles.sumOf(File::length),
            previewModelFileCount = previewModelFiles.size,
            previewModelStorageBytes = previewModelFiles.sumOf(File::length),
            temporaryFileCount = temporaryFiles.size,
            temporaryStorageBytes = temporaryFiles.sumOf(File::length),
        )
    }

    override suspend fun clear(category: AppCacheCategory) = withContext(Dispatchers.IO) {
        when (category) {
            AppCacheCategory.BlueprintMetadata -> blueprintDao.clearBlueprintMetadata()
            AppCacheCategory.RenderResources -> clearRenderResources()
            AppCacheCategory.PreviewModels -> clearPreviewModels()
            AppCacheCategory.Temporary -> clearTemporaryCache()
            AppCacheCategory.All -> {
                blueprintDao.clearBlueprintMetadata()
                clearRenderResources()
                clearPreviewModels()
                clearTemporaryCache()
            }
        }
    }

    private suspend fun clearRenderResources() {
        resourcePackRepository.deleteAll()
        LangReader.clearCache()
    }

    private suspend fun clearTemporaryCache() {
        context.cacheDir.listFiles()?.forEach(File::deleteRecursively)
        context.cacheDir.mkdirs()
        renderStagingRoot().deleteRecursively()
        iconIndexResolver.invalidateCache()
        LangReader.clearCache()
    }

    private fun clearPreviewModels() {
        previewCacheRoot().deleteRecursively()
        previewCacheRoot().mkdirs()
    }

    private fun databaseFiles(): List<File> {
        val database = context.getDatabasePath(DATABASE_NAME)
        return listOf(
            database,
            File(database.path + "-wal"),
            File(database.path + "-shm"),
        ).filter(File::isFile)
    }

    private fun renderRoot(): File = File(context.filesDir, RENDER_ROOT)

    private fun renderStagingRoot(): File = File(context.filesDir, RENDER_STAGING_ROOT)

    private fun previewCacheRoot(): File = File(context.filesDir, PREVIEW_CACHE_ROOT)

    private fun filesIn(root: File): List<File> =
        if (root.isDirectory) root.walkTopDown().filter(File::isFile).toList() else emptyList()

    private companion object {
        const val DATABASE_NAME = "blockprint_cat.db"
        const val RENDER_ROOT = "blockprintcat/render_assets"
        const val RENDER_STAGING_ROOT = "blockprintcat/render_assets_staging"
        const val PREVIEW_CACHE_ROOT = "blockprintcat/glb_cache"
    }
}
