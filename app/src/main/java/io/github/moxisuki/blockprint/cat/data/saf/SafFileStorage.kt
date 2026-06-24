package io.github.moxisuki.blockprint.cat.data.saf

import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import io.github.moxisuki.blockprint.cat.data.blueprint.FileEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.io.OutputStream

private const val TAG = "SafFileStorage"
private const val MIME_OCTET = "application/octet-stream"

@Singleton
class SafFileStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionManager: SafPermissionManager,
) : LitematicFileStorage {

    override suspend fun list(): List<FileEntry> = withContext(Dispatchers.IO) {
        val treeUri = permissionManager.treeUri() ?: return@withContext emptyList()
        val folderDocId = permissionManager.folderDocId() ?: return@withContext emptyList()
        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocId)
        val result = mutableListOf<FileEntry>()
        context.contentResolver.query(childUri, arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        ), null, null, null)?.use { c ->
            val idCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val sizeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            val mtimeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (c.moveToNext()) {
                val name = c.getString(nameCol) ?: continue
                if (!isBlueprintFile(name)) continue
                result.add(FileEntry(
                    docId = c.getString(idCol) ?: continue,
                    name = name,
                    size = if (sizeCol >= 0) c.getLong(sizeCol) else -1L,
                    lastModified = if (mtimeCol >= 0) c.getLong(mtimeCol) else 0L,
                ))
            }
        }
        result
    }

    override suspend fun read(docId: String): ByteArray = withContext(Dispatchers.IO) {
        val treeUri = permissionManager.treeUri() ?: throw IllegalStateException("SAF not configured")
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        context.contentResolver.openInputStream(docUri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read document: $docId")
    }

    override suspend fun write(name: String, bytes: ByteArray, onProgress: ((Long, Long) -> Unit)?): String = withContext(Dispatchers.IO) {
        val treeUri = permissionManager.treeUri() ?: throw IllegalStateException("SAF not configured")
        val folderDocId = permissionManager.folderDocId() ?: throw IllegalStateException("SAF not configured")
        val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderDocId)
        val docUri = DocumentsContract.createDocument(context.contentResolver, folderUri, MIME_OCTET, name)
            ?: throw IllegalStateException("SAF createDocument failed: $name")
        val total = bytes.size.toLong()
        onProgress?.invoke(0L, total)
        context.contentResolver.openOutputStream(docUri)?.use { out ->
            val chunkSize = 64 * 1024
            var written = 0
            while (written < bytes.size) {
                val end = minOf(written + chunkSize, bytes.size)
                out.write(bytes, written, end - written)
                written = end
                onProgress?.invoke(written.toLong(), total)
            }
        } ?: throw IllegalStateException("SAF write failed: $name")
        DocumentsContract.getDocumentId(docUri)
    }

    override suspend fun writeStream(name: String, writer: (OutputStream) -> Unit): String = withContext(Dispatchers.IO) {
        val treeUri = permissionManager.treeUri() ?: throw IllegalStateException("SAF not configured")
        val folderDocId = permissionManager.folderDocId() ?: throw IllegalStateException("SAF not configured")
        val folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderDocId)
        val docUri = DocumentsContract.createDocument(context.contentResolver, folderUri, MIME_OCTET, name)
            ?: throw IllegalStateException("SAF createDocument failed: $name")
        context.contentResolver.openOutputStream(docUri)?.use { out ->
            writer(out)
        } ?: throw IllegalStateException("SAF writeStream failed: $name")
        DocumentsContract.getDocumentId(docUri)
    }

    override suspend fun delete(docId: String) = withContext(Dispatchers.IO) {
        val treeUri = permissionManager.treeUri() ?: throw IllegalStateException("SAF not configured")
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        DocumentsContract.deleteDocument(context.contentResolver, docUri)
        Unit
    }

    override suspend fun rename(docId: String, newName: String): String = withContext(Dispatchers.IO) {
        val treeUri = permissionManager.treeUri() ?: throw IllegalStateException("SAF not configured")
        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
        val newUri = DocumentsContract.renameDocument(context.contentResolver, docUri, newName)
            ?: throw IllegalStateException("SAF rename failed: $docId → $newName")
        DocumentsContract.getDocumentId(newUri)
    }

    override suspend fun exists(name: String): Boolean = withContext(Dispatchers.IO) {
        val treeUri = permissionManager.treeUri() ?: return@withContext false
        val folderDocId = permissionManager.folderDocId() ?: return@withContext false
        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocId)
        context.contentResolver.query(childUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            "${DocumentsContract.Document.COLUMN_DISPLAY_NAME}=?", arrayOf(name), null
        )?.use { it.count > 0 } ?: false
    }

    private fun isBlueprintFile(name: String): Boolean {
        val l = name.lowercase()
        return l.endsWith(".litematic") || l.endsWith(".nbt") || l.endsWith(".schematic") || l.endsWith(".schem") || l.endsWith(".json")
    }
}
