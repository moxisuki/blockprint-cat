package io.github.moxisuki.blockprint.cat.app.core.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintRepository
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.SafBlueprintStorage
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.isSupportedBlueprintFile
import io.github.moxisuki.blockprint.cat.app.core.persistence.AppSettingsRepository
import java.io.File
import java.io.FilterOutputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class BlueprintBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: AppSettingsRepository,
    private val storage: SafBlueprintStorage,
    private val blueprintRepository: BlueprintRepository,
) {
    suspend fun backupToDownloads(): BlueprintBackupResult = withContext(Dispatchers.IO) {
        val blueprints = blueprintRepository.getLocalBlueprints()
        val savedCategories = blueprintRepository.getCategories()
        if (blueprints.isEmpty() && savedCategories.isEmpty()) {
            throw IllegalStateException("No blueprints to back up")
        }

        val categories = (savedCategories + blueprints.map { it.category })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val fileEntries = blueprints.mapIndexed { index, blueprint ->
            BackupFileManifest(
                path = "$BLUEPRINTS_DIR/${index.toString().padStart(3, '0')}_${blueprint.fileName.toZipSafeName()}",
                name = blueprint.fileName,
                category = blueprint.category,
            )
        }
        val manifestJson = buildManifestJson(
            categories = categories,
            files = fileEntries,
        )
        val fileName = "blockprint-cat-backup-${timestamp()}.zip"
        val sizeBytes = saveZipToDownloads(fileName) { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_NAME))
                zip.write(manifestJson.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                blueprints.zip(fileEntries).forEach { (blueprint, entry) ->
                    zip.putNextEntry(ZipEntry(entry.path))
                    blueprintRepository.copyBlueprintFileToOutputStream(
                        documentId = blueprint.documentId,
                        output = zip,
                    )
                    zip.closeEntry()
                }
            }
        }

        BlueprintBackupResult(
            fileName = fileName,
            fileCount = blueprints.size,
            sizeBytes = sizeBytes,
        )
    }

    suspend fun restoreFromZip(uriString: String): BlueprintRestoreResult = withContext(Dispatchers.IO) {
        val treeUri = settingsRepository.localBlueprintTreeUri.first()
            ?: throw IllegalStateException("SAF directory is not selected")
        val treeDocumentId = settingsRepository.localBlueprintTreeDocumentId.first()
        val zipUri = Uri.parse(uriString)
        val restoredFiles = mutableListOf<RestoredBackupFile>()
        val manifest = readManifest(zipUri)

        context.contentResolver.openInputStream(zipUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    if (!entry.isDirectory) {
                        when {
                            entry.name == MANIFEST_NAME -> Unit
                            isSupportedBlueprintFile(entry.name) -> {
                                val originalName = manifest?.fileName(
                                    path = entry.name,
                                ) ?: entry.name.substringAfterLast('/')
                                val restored = storage.writeStreamToBlueprintFolder(
                                    treeUriString = treeUri,
                                    treeDocumentId = treeDocumentId,
                                    displayName = originalName,
                                    input = zip,
                                )
                                restoredFiles += RestoredBackupFile(
                                    entryPath = entry.name,
                                    originalName = originalName,
                                    restoredDocumentId = restored.documentId,
                                )
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        } ?: throw IllegalStateException("Cannot read backup file")

        blueprintRepository.refreshLocalBlueprints()

        val parsedManifest = manifest
        val categoryByDocumentId = if (parsedManifest == null) {
            emptyMap()
        } else {
            restoredFiles.mapNotNull { restored ->
                val category = parsedManifest.fileCategory(
                    path = restored.entryPath,
                    name = restored.originalName,
                ).trim()
                if (category.isBlank()) {
                    null
                } else {
                    restored.restoredDocumentId to category
                }
            }.toMap()
        }
        blueprintRepository.restoreCategoryManifest(
            categories = parsedManifest?.categories.orEmpty(),
            categoryByDocumentId = categoryByDocumentId,
        )

        BlueprintRestoreResult(
            fileCount = restoredFiles.size,
            hasManifest = parsedManifest != null,
        )
    }

    private suspend fun saveZipToDownloads(
        fileName: String,
        writeZip: suspend (OutputStream) -> Unit,
    ): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, ZIP_MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Cannot create backup in Downloads")
            var sizeBytes = 0L
            try {
                resolver.openOutputStream(uri, "w")?.use { output ->
                    CountingOutputStream(output).use { countingOutput ->
                        writeZip(countingOutput)
                        sizeBytes = countingOutput.bytesWritten
                    }
                }
                    ?: throw IllegalStateException("Cannot write backup")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (throwable: Throwable) {
                resolver.delete(uri, null, null)
                throw throwable
            }
            return sizeBytes
        } else {
            @Suppress("DEPRECATION")
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloads.mkdirs()
            var sizeBytes = 0L
            FileOutputStream(File(downloads, fileName)).use { output ->
                CountingOutputStream(output).use { countingOutput ->
                    writeZip(countingOutput)
                    sizeBytes = countingOutput.bytesWritten
                }
            }
            return sizeBytes
        }
    }

    private fun readManifest(zipUri: Uri): BackupManifest? {
        context.contentResolver.openInputStream(zipUri)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    try {
                        if (!entry.isDirectory && entry.name == MANIFEST_NAME) {
                            return parseManifest(zip.readBytes().toString(Charsets.UTF_8))
                        }
                    } finally {
                        zip.closeEntry()
                    }
                }
            }
        }
        return null
    }

    private fun buildManifestJson(
        categories: List<String>,
        files: List<BackupFileManifest>,
    ): String {
        val root = JSONObject()
            .put("version", 1)
            .put("categories", JSONArray(categories))
        val fileArray = JSONArray()
        files.forEach { file ->
            fileArray.put(
                JSONObject()
                    .put("path", file.path)
                    .put("name", file.name)
                    .put("category", file.category),
            )
        }
        root.put("files", fileArray)
        return root.toString(2)
    }

    private fun parseManifest(json: String): BackupManifest {
        val root = JSONObject(json)
        val categories = root.optJSONArray("categories")
            ?.let { array -> List(array.length()) { index -> array.optString(index) } }
            .orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val files = root.optJSONArray("files")
            ?.let { array ->
                List(array.length()) { index ->
                    val item = array.optJSONObject(index) ?: JSONObject()
                    BackupFileManifest(
                        path = item.optString("path"),
                        name = item.optString("name"),
                        category = item.optString("category"),
                    )
                }
            }
            .orEmpty()

        return BackupManifest(
            categories = categories,
            files = files,
        )
    }

    private fun BackupManifest.fileCategory(path: String, name: String): String =
        files.firstOrNull { it.path == path }?.category
            ?: files.firstOrNull { it.name == name }?.category
            ?: ""

    private fun BackupManifest.fileName(path: String): String? =
        files.firstOrNull { it.path == path }?.name?.takeIf { it.isNotBlank() }

    private fun String.toZipSafeName(): String =
        replace('\\', '_')
            .replace('/', '_')
            .ifBlank { "blueprint" }

    private fun timestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))

    private companion object {
        const val MANIFEST_NAME = "manifest.json"
        const val BLUEPRINTS_DIR = "blueprints"
        const val ZIP_MIME_TYPE = "application/zip"
    }
}

private class CountingOutputStream(
    output: OutputStream,
) : FilterOutputStream(output) {
    var bytesWritten: Long = 0L
        private set

    override fun write(b: Int) {
        out.write(b)
        bytesWritten += 1L
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
        bytesWritten += len.toLong()
    }
}

data class BlueprintBackupResult(
    val fileName: String,
    val fileCount: Int,
    val sizeBytes: Long,
)

data class BlueprintRestoreResult(
    val fileCount: Int,
    val hasManifest: Boolean,
)

private data class BackupManifest(
    val categories: List<String>,
    val files: List<BackupFileManifest>,
)

private data class BackupFileManifest(
    val path: String,
    val name: String,
    val category: String,
)

private data class RestoredBackupFile(
    val entryPath: String,
    val originalName: String,
    val restoredDocumentId: String,
)
