package io.github.moxisuki.blockprint.cat.app.core.data.blueprint

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafBlueprintStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver get() = context.contentResolver

    internal suspend fun listBlueprintFiles(
        treeUriString: String,
        treeDocumentId: String?,
    ): List<BlueprintFileEntry> = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriString)
        val folderDocumentId = ensureBlueprintFolder(
            treeUri = treeUri,
            treeDocumentId = treeDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri),
        )
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocumentId)
        val result = mutableListOf<BlueprintFileEntry>()

        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val lastModifiedColumn =
                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val documentId = cursor.getString(idColumn) ?: continue
                val name = cursor.getString(nameColumn) ?: continue
                if (!isSupportedBlueprintFile(name)) continue

                result += BlueprintFileEntry(
                    documentId = documentId,
                    name = name,
                    sizeBytes = if (sizeColumn >= 0) cursor.getLong(sizeColumn) else -1L,
                    lastModifiedAt = if (lastModifiedColumn >= 0) {
                        cursor.getLong(lastModifiedColumn)
                    } else {
                        0L
                    },
                )
            }
        }

        result
    }

    suspend fun readFile(treeUriString: String, documentId: String): ByteArray = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriString)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        resolver.openInputStream(documentUri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read blueprint document: $documentId")
    }

    internal suspend fun readExternalFile(uriString: String): ByteArray = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read blueprint file: $uriString")
    }

    internal suspend fun copyFileToOutputStream(
        treeUriString: String,
        documentId: String,
        output: OutputStream,
    ): Long = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriString)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        resolver.openInputStream(documentUri)?.use { input ->
            input.copyTo(output)
        } ?: throw IllegalStateException("Cannot read blueprint document: $documentId")
    }

    internal suspend fun getExternalFileInfo(uriString: String): ExternalBlueprintFileInfo = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        var name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "blueprint"
        var sizeBytes = -1L

        resolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME,
                OpenableColumns.SIZE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameColumn >= 0) {
                    name = cursor.getString(nameColumn)?.takeIf { it.isNotBlank() } ?: name
                }
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
                    sizeBytes = cursor.getLong(sizeColumn)
                }
            }
        }

        ExternalBlueprintFileInfo(
            name = name,
            sizeBytes = sizeBytes,
        )
    }

    internal suspend fun copyExternalFileToBlueprintFolder(
        treeUriString: String,
        treeDocumentId: String?,
        sourceUriString: String,
        displayName: String,
    ): BlueprintFileEntry = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriString)
        val folderDocumentId = ensureBlueprintFolder(
            treeUri = treeUri,
            treeDocumentId = treeDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri),
        )
        val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderDocumentId)
        val sourceUri = Uri.parse(sourceUriString)
        val createdUri = DocumentsContract.createDocument(
            resolver,
            folderUri,
            resolver.getType(sourceUri) ?: "application/octet-stream",
            displayName,
        ) ?: throw IllegalStateException("Cannot create blueprint document: $displayName")

        resolver.openInputStream(sourceUri)?.use { input ->
            resolver.openOutputStream(createdUri, "w")?.use { output ->
                input.copyTo(output)
            } ?: throw IllegalStateException("Cannot write imported blueprint: $displayName")
        } ?: throw IllegalStateException("Cannot read blueprint file: $sourceUriString")

        val documentId = DocumentsContract.getDocumentId(createdUri)
        queryDocumentInfo(treeUri, documentId, fallbackName = displayName)
    }

    internal suspend fun writeStreamToBlueprintFolder(
        treeUriString: String,
        treeDocumentId: String?,
        displayName: String,
        input: InputStream,
    ): BlueprintFileEntry = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriString)
        val folderDocumentId = ensureBlueprintFolder(
            treeUri = treeUri,
            treeDocumentId = treeDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri),
        )
        val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderDocumentId)
        val createdUri = DocumentsContract.createDocument(
            resolver,
            folderUri,
            "application/octet-stream",
            displayName,
        ) ?: throw IllegalStateException("Cannot create blueprint document: $displayName")

        resolver.openOutputStream(createdUri, "w")?.use { output ->
            input.copyTo(output)
        } ?: throw IllegalStateException("Cannot write restored blueprint: $displayName")

        val documentId = DocumentsContract.getDocumentId(createdUri)
        queryDocumentInfo(treeUri, documentId, fallbackName = displayName)
    }

    internal suspend fun writeGeneratedBlueprintToFolder(
        treeUriString: String,
        treeDocumentId: String?,
        displayName: String,
        writer: (OutputStream) -> Unit,
    ): BlueprintFileEntry = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriString)
        val folderDocumentId = ensureBlueprintFolder(
            treeUri = treeUri,
            treeDocumentId = treeDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri),
        )
        val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderDocumentId)
        val createdUri = DocumentsContract.createDocument(
            resolver,
            folderUri,
            "application/octet-stream",
            displayName,
        ) ?: throw IllegalStateException("Cannot create blueprint document: $displayName")

        try {
            resolver.openOutputStream(createdUri, "w")?.use(writer)
                ?: throw IllegalStateException("Cannot write generated blueprint: $displayName")
        } catch (t: Throwable) {
            runCatching { DocumentsContract.deleteDocument(resolver, createdUri) }
            throw t
        }

        val documentId = DocumentsContract.getDocumentId(createdUri)
        queryDocumentInfo(treeUri, documentId, fallbackName = displayName)
    }

    suspend fun deleteFile(treeUriString: String, documentId: String) = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriString)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        DocumentsContract.deleteDocument(resolver, documentUri)
    }

    suspend fun renameFile(
        treeUriString: String,
        documentId: String,
        newName: String,
    ): String = withContext(Dispatchers.IO) {
        val treeUri = Uri.parse(treeUriString)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val renamedUri = DocumentsContract.renameDocument(resolver, documentUri, newName)
            ?: throw IllegalStateException("Cannot rename blueprint document: $documentId")
        DocumentsContract.getDocumentId(renamedUri)
    }

    private fun ensureBlueprintFolder(
        treeUri: Uri,
        treeDocumentId: String,
    ): String {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)

        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameColumn)
                val mimeType = if (mimeColumn >= 0) cursor.getString(mimeColumn) else null
                if (name == BLUEPRINT_FOLDER_NAME &&
                    mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                ) {
                    return cursor.getString(idColumn)
                }
            }
        }

        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocumentId)
        val createdUri = DocumentsContract.createDocument(
            resolver,
            parentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            BLUEPRINT_FOLDER_NAME,
        ) ?: throw IllegalStateException("Cannot create $BLUEPRINT_FOLDER_NAME folder")

        return DocumentsContract.getDocumentId(createdUri)
    }

    private fun queryDocumentInfo(
        treeUri: Uri,
        documentId: String,
        fallbackName: String,
    ): BlueprintFileEntry {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        resolver.query(
            documentUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                val lastModifiedColumn =
                    cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                return BlueprintFileEntry(
                    documentId = documentId,
                    name = if (nameColumn >= 0) cursor.getString(nameColumn) ?: fallbackName else fallbackName,
                    sizeBytes = if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn) else -1L,
                    lastModifiedAt = if (lastModifiedColumn >= 0 && !cursor.isNull(lastModifiedColumn)) {
                        cursor.getLong(lastModifiedColumn)
                    } else {
                        System.currentTimeMillis()
                    },
                )
            }
        }

        return BlueprintFileEntry(
            documentId = documentId,
            name = fallbackName,
            sizeBytes = -1L,
            lastModifiedAt = System.currentTimeMillis(),
        )
    }

    private companion object {
        const val BLUEPRINT_FOLDER_NAME = "blockprintCat"
    }
}

internal data class ExternalBlueprintFileInfo(
    val name: String,
    val sizeBytes: Long,
)
