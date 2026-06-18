package io.github.moxisuki.blockprint.cat.data.saf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import io.github.moxisuki.blockprint.cat.data.blueprint.StorageConfigDao
import io.github.moxisuki.blockprint.cat.data.blueprint.StorageConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SafPermissionManager"

@Singleton
class SafPermissionManager @Inject constructor(
    private val configDao: StorageConfigDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _safState = MutableStateFlow<SafState>(SafState.Idle)
    val safState: StateFlow<SafState> = _safState.asStateFlow()

    @Volatile
    private var cached: StorageConfigEntity? = null

    init {
        scope.launch {
            val entity = configDao.get()
            cached = entity
            _safState.value = if (entity != null) SafState.Ready(entity.displayName) else SafState.Idle
        }
        scope.launch {
            configDao.observe().collect { entity ->
                cached = entity
                _safState.value = if (entity != null) SafState.Ready(entity.displayName) else SafState.Idle
            }
        }
    }

    fun setTreeUri(context: Context, treeUri: Uri) {
        val resolver = context.contentResolver
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)

        // 查找或创建 blockprintCat 子目录；失败则 fallback 到根目录
        val subDirName = "blockprintCat"
        val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
        var folderDocId: String? = null
        resolver.query(childUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null)?.use { c ->
            val idCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (c.moveToNext()) {
                if (c.getString(nameCol) == subDirName) {
                    folderDocId = c.getString(idCol)
                    break
                }
            }
        }
        if (folderDocId == null) {
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            val created = DocumentsContract.createDocument(resolver, parentUri,
                DocumentsContract.Document.MIME_TYPE_DIR, subDirName)
            if (created != null) {
                folderDocId = DocumentsContract.getDocumentId(created)
            } else {
                // fallback: 某些 SAF provider 不允许创建子目录，用根目录
                Log.w(TAG, "setTreeUri: createDocument '$subDirName' failed, falling back to root")
                folderDocId = treeDocId
            }
        }

        val userDirName = runCatching {
            resolver.query(treeUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: "Documents"

        val displayName = if (folderDocId == treeDocId) userDirName else "$userDirName/$subDirName"

        val entity = StorageConfigEntity(
            treeUri = treeUri.toString(),
            folderDocId = folderDocId,
            displayName = displayName,
        )

        Log.i(TAG, "setTreeUri: $displayName folderDocId=$folderDocId")

        cached = entity
        _safState.value = SafState.Ready(entity.displayName)

        scope.launch { configDao.upsert(entity) }
    }

    fun clear() {
        scope.launch {
            configDao.clear()
        }
    }

    fun validate(context: Context): Boolean {
        val c = cached ?: return false
        return runCatching {
            val uri = Uri.parse(c.treeUri)
            val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, c.folderDocId)
            context.contentResolver.query(childUri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
                null, null, null)?.use { it.count >= 0 } ?: false
        }.getOrDefault(false)
    }

    internal fun treeUri(): Uri? = cached?.let { Uri.parse(it.treeUri) }
    internal fun folderDocId(): String? = cached?.folderDocId
}
