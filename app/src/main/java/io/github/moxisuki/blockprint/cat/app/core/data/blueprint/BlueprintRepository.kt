package io.github.moxisuki.blockprint.cat.app.core.data.blueprint

import io.github.moxisuki.blockprint.cat.app.core.persistence.AppSettingsRepository
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class BlueprintRepository @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val storage: SafBlueprintStorage,
    private val reader: BlueprintCoreReader,
    private val dao: BlueprintDao,
) {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    private val refreshMutex = Mutex()

    fun observeLocalBlueprints(): Flow<List<LocalBlueprint>> =
        dao.observeLocalBlueprints().map { entities ->
            entities.map { it.toLocalBlueprint() }
        }

    fun observeBlueprint(id: String): Flow<LocalBlueprint?> =
        dao.observeBlueprint(id).map { it?.toLocalBlueprint() }

    fun observeMaterials(blueprintId: String): Flow<List<BlueprintMaterial>> =
        dao.observeMaterials(blueprintId).map { entities ->
            entities.map { it.toBlueprintMaterial() }
        }

    fun observeCategories(): Flow<List<String>> =
        dao.observeCategories().map { entities ->
            entities.map { it.name }
        }

    internal suspend fun getLocalBlueprints(): List<BlueprintEntity> =
        dao.getAllBlueprints()

    internal suspend fun getCategories(): List<String> =
        dao.getCategories().map { it.name }

    internal suspend fun copyBlueprintFileToOutputStream(
        documentId: String,
        output: OutputStream,
    ): Long {
        val treeUri = settingsRepository.localBlueprintTreeUri.first()
            ?: throw IllegalStateException("SAF directory is not selected")
        return storage.copyFileToOutputStream(
            treeUriString = treeUri,
            documentId = documentId,
            output = output,
        )
    }

    suspend fun readImportPreview(sourceUri: String): BlueprintImportPreview = withContext(Dispatchers.IO) {
        val fileInfo = storage.getExternalFileInfo(sourceUri)
        if (!isSupportedBlueprintFile(fileInfo.name)) {
            throw IllegalArgumentException("Unsupported blueprint file: ${fileInfo.name}")
        }
        val bytes = storage.readExternalFile(sourceUri)
        val parsed = reader.read(bytes, fileInfo.name)
        BlueprintImportPreview(
            sourceUri = sourceUri,
            fileName = fileInfo.name,
            displayName = parsed.displayName,
            author = parsed.author,
            format = parsed.format,
            blockCount = parsed.blockCount,
            regionCount = parsed.regionCount,
            sizeBytes = if (fileInfo.sizeBytes >= 0L) fileInfo.sizeBytes else bytes.size.toLong(),
        )
    }

    suspend fun importBlueprint(preview: BlueprintImportPreview) {
        var imported = false
        refreshMutex.withLock {
            val treeUri = settingsRepository.localBlueprintTreeUri.first() ?: return@withLock
            val treeDocumentId = settingsRepository.localBlueprintTreeDocumentId.first()

            _isRefreshing.value = true
            try {
                withContext(Dispatchers.IO) {
                    val bytes = storage.readExternalFile(preview.sourceUri)
                    val parsed = reader.read(bytes, preview.fileName)
                    val importedFile = storage.copyExternalFileToBlueprintFolder(
                        treeUriString = treeUri,
                        treeDocumentId = treeDocumentId,
                        sourceUriString = preview.sourceUri,
                        displayName = preview.fileName,
                    )
                    upsertParsedBlueprint(
                        file = importedFile,
                        parsed = parsed,
                        blueprintId = UUID.randomUUID().toString(),
                        category = "",
                        sizeBytes = if (importedFile.sizeBytes >= 0L) {
                            importedFile.sizeBytes
                        } else {
                            bytes.size.toLong()
                        },
                    )
                    imported = true
                }
            } finally {
                _isRefreshing.value = false
            }
        }
        if (imported) {
            refreshLocalBlueprints()
        }
    }

    suspend fun moveBlueprintsToCategory(ids: List<String>, category: String) {
        if (ids.isEmpty()) return
        if (category.isNotBlank()) {
            dao.createCategory(category)
        }
        dao.updateCategory(ids, category)
    }

    suspend fun createCategory(category: String) {
        val name = category.trim()
        if (name.isBlank()) return
        dao.createCategory(name)
    }

    suspend fun renameCategory(oldCategory: String, newCategory: String) {
        if (oldCategory.isBlank() || newCategory.isBlank() || oldCategory == newCategory) return
        dao.renameCategory(oldCategory, newCategory)
    }

    suspend fun deleteCategory(category: String) {
        if (category.isBlank()) return
        dao.deleteCategory(category)
    }

    internal suspend fun restoreCategoryManifest(
        categories: List<String>,
        categoryByDocumentId: Map<String, String>,
    ) {
        dao.upsertCategoryNames(categories)
        categoryByDocumentId.forEach { (documentId, category) ->
            dao.updateCategoryByDocumentId(documentId, category)
        }
    }

    suspend fun deleteBlueprint(id: String) = withContext(Dispatchers.IO) {
        val treeUri = settingsRepository.localBlueprintTreeUri.first() ?: return@withContext
        val blueprint = dao.getBlueprint(id) ?: return@withContext
        runCatching {
            storage.deleteFile(
                treeUriString = treeUri,
                documentId = blueprint.documentId,
            )
        }
        dao.deleteBlueprint(id)
    }

    suspend fun renameBlueprint(id: String, newName: String) = withContext(Dispatchers.IO) {
        val treeUri = settingsRepository.localBlueprintTreeUri.first() ?: return@withContext
        val blueprint = dao.getBlueprint(id) ?: return@withContext
        val sanitizedName = newName.toSupportedBlueprintFileName(blueprint.fileName)
        if (sanitizedName.isBlank()) return@withContext

        val documentId = storage.renameFile(
            treeUriString = treeUri,
            documentId = blueprint.documentId,
            newName = sanitizedName,
        )
        dao.updateFileName(
            id = id,
            documentId = documentId,
            fileName = sanitizedName,
            displayName = sanitizedName.substringBeforeLast('.', sanitizedName),
            lastModifiedAt = System.currentTimeMillis(),
            scannedAt = System.currentTimeMillis(),
        )
    }

    suspend fun refreshLocalBlueprints() = refreshMutex.withLock {
        val treeUri = settingsRepository.localBlueprintTreeUri.first() ?: return@withLock
        val treeDocumentId = settingsRepository.localBlueprintTreeDocumentId.first()

        _isRefreshing.value = true
        try {
            withContext(Dispatchers.IO) {
                val files = storage.listBlueprintFiles(
                    treeUriString = treeUri,
                    treeDocumentId = treeDocumentId,
                )
                if (files.isEmpty()) {
                    dao.deleteAllBlueprints()
                    return@withContext
                }

                val knownByDocumentId = dao.getAllBlueprints().associateBy { it.documentId }
                dao.deleteBlueprintsMissingFrom(files.map { it.documentId })

                for (file in files) {
                    val known = knownByDocumentId[file.documentId]
                    if (known != null &&
                        known.lastModifiedAt == file.lastModifiedAt &&
                        known.sizeBytes == file.sizeBytes
                    ) {
                        continue
                    }

                    runCatching {
                        val bytes = storage.readFile(
                            treeUriString = treeUri,
                            documentId = file.documentId,
                        )
                        val parsed = reader.read(bytes, file.name)
                        val blueprintId = known?.id ?: UUID.randomUUID().toString()
                        upsertParsedBlueprint(
                            file = file,
                            parsed = parsed,
                            blueprintId = blueprintId,
                            category = known?.category.orEmpty(),
                            sizeBytes = file.sizeBytes,
                        )
                    }
                }
            }
        } finally {
            _isRefreshing.value = false
        }
    }

    private fun String.toSupportedBlueprintFileName(currentFileName: String): String {
        val rawName = trim()
            .replace('\\', '_')
            .replace('/', '_')
        if (rawName.isBlank()) return ""

        val candidate = if (rawName.substringAfterLast('.', missingDelimiterValue = "").isBlank()) {
            val extension = currentFileName.substringAfterLast('.', missingDelimiterValue = "")
            if (extension.isBlank()) rawName else "$rawName.$extension"
        } else {
            rawName
        }
        require(isSupportedBlueprintFile(candidate)) {
            "Unsupported blueprint file name: $candidate"
        }
        return candidate
    }

    private suspend fun upsertParsedBlueprint(
        file: BlueprintFileEntry,
        parsed: ParsedBlueprint,
        blueprintId: String,
        category: String,
        sizeBytes: Long,
    ) {
        val blueprint = BlueprintEntity(
            id = blueprintId,
            documentId = file.documentId,
            fileName = file.name,
            displayName = parsed.displayName,
            author = parsed.author,
            format = parsed.format.name,
            category = category,
            blockCount = parsed.blockCount,
            regionCount = parsed.regionCount,
            sizeBytes = sizeBytes,
            lastModifiedAt = file.lastModifiedAt,
            scannedAt = System.currentTimeMillis(),
        )
        val materials = parsed.materials.map { material ->
            BlueprintMaterialEntity(
                blueprintId = blueprintId,
                blockId = material.blockId,
                count = material.count,
            )
        }
        dao.upsertBlueprintWithMaterials(blueprint, materials)
    }
}
