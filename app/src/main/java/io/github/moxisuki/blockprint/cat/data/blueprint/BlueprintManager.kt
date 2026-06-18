package io.github.moxisuki.blockprint.cat.data.blueprint

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import io.github.moxisuki.blockprint.core.Litematic
import io.github.moxisuki.blockprint.core.LitematicReader
import io.github.moxisuki.blockprint.core.MaterialList
import io.github.moxisuki.blockprint.core.SchematicFormat
import io.github.moxisuki.blockprint.cat.data.saf.LitematicFileStorage
import io.github.moxisuki.blockprint.cat.data.saf.SafPermissionManager
import io.github.moxisuki.blockprint.cat.data.saf.SafState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BlueprintManager"

@Singleton
class BlueprintManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val metaDao: BlueprintMetaDao,
    private val storage: LitematicFileStorage,
    private val safPermissionManager: SafPermissionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _blueprints = MutableStateFlow<List<BlueprintMeta>>(emptyList())
    val blueprints: StateFlow<List<BlueprintMeta>> = _blueprints.asStateFlow()

    private val _blueprintCount = MutableStateFlow(0)
    val blueprintCount: StateFlow<Int> = _blueprintCount.asStateFlow()

    val safState: StateFlow<SafState> = safPermissionManager.safState

    init {
        scope.launch {
            metaDao.observeAll().collect { entities ->
                _blueprints.value = entities.map { it.toMeta() }
            }
        }
        scope.launch {
            metaDao.observeCount().collect { _blueprintCount.value = it }
        }
    }

    // ── SAF 配置 ──

    fun setSafFolder(context: Context, treeUri: Uri) {
        context.contentResolver.takePersistableUriPermission(treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        safPermissionManager.setTreeUri(context, treeUri)
        scope.launch { refresh() }
    }

    // ── 数据入口 ──

    suspend fun ingest(name: String, bytes: ByteArray): BlueprintMeta = withContext(Dispatchers.IO) {
        val lit = LitematicReader.readLenient(bytes)
        val uniqueName = resolveUniqueName(name)
        val docId = storage.write(uniqueName, bytes)
        val meta = metaFromLit(lit, docId, uniqueName)
        metaDao.upsert(meta.toEntity())
        Log.d(TAG, "ingest: $uniqueName → ${meta.uuid}")
        meta.toMeta()
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        if (safPermissionManager.treeUri() == null) return@withContext
        _scanning.value = true
        val now = System.currentTimeMillis()

        val files = storage.list().associateBy { it.docId }
        val known = metaDao.getAll().associateBy { it.fileDocId }

        coroutineScope {
            for ((docId, entry) in files) {
                val existing = known[docId]
                if (existing == null || entry.lastModified > existing.lastScannedAt) {
                    launch {
                        runCatching {
                            val bytes = storage.read(docId)
                            val lit = LitematicReader.readLenient(bytes)
                            val meta = metaFromLit(lit, docId, entry.name)
                            metaDao.upsert(meta.copy(lastScannedAt = now).toEntity())
                            Log.d(TAG, "refresh: upsert ${entry.name}")
                        }.onFailure { Log.e(TAG, "refresh: fail ${entry.name}", it) }
                    }
                }
            }
        }

        for ((docId, entity) in known) {
            if (docId !in files) {
                metaDao.delete(entity.uuid)
                Log.d(TAG, "refresh: deleted ${entity.fileName}")
            }
        }

        _scanning.value = false
    }

    // ── CRUD ──

    suspend fun delete(uuid: String) = withContext(Dispatchers.IO) {
        val entity = metaDao.getByUuid(uuid) ?: return@withContext
        runCatching { storage.delete(entity.fileDocId) }
        metaDao.delete(uuid)
    }

    suspend fun rename(uuid: String, newName: String) = withContext(Dispatchers.IO) {
        val entity = metaDao.getByUuid(uuid)
            ?: throw IllegalStateException("Blueprint not found")
        val all = metaDao.getAll()
        if (all.any { it.uuid != uuid && it.fileName == newName })
            throw IllegalStateException("文件名 \"$newName\" 已存在")
        val newDocId = storage.rename(entity.fileDocId, newName)
        metaDao.upsert(entity.copy(fileDocId = newDocId, fileName = newName, lastScannedAt = System.currentTimeMillis()))
    }

    suspend fun readBytes(uuid: String): ByteArray? = withContext(Dispatchers.IO) {
        val entity = metaDao.getByUuid(uuid) ?: return@withContext null
        runCatching { storage.read(entity.fileDocId) }.getOrNull()
    }

    suspend fun loadDetail(uuid: String): FullBlueprint? = withContext(Dispatchers.IO) {
        val entity = metaDao.getByUuid(uuid) ?: return@withContext null
        runCatching {
            val bytes = storage.read(entity.fileDocId)
            val lit = LitematicReader.readLenient(bytes)
            val materials = MaterialList.from(lit, includeAir = false)
                .toSortedByCount()
                .map { (n, c) -> n to c }
            FullBlueprint(meta = entity.toMeta(), materials = materials, raw = lit)
        }.getOrNull()
    }

    // ── helpers ──

    private fun metaFromLit(lit: Litematic, docId: String, fileName: String): BlueprintMetaEntity {
        val base = fileName.substringBeforeLast('.', fileName)
        val displayName = when {
            lit.name.isNotBlank() -> lit.name
            base.isNotBlank() && base != fileName -> base
            lit.format != SchematicFormat.Unknown -> base.ifEmpty { fileName }
            else -> fileName
        }
        return BlueprintMetaEntity(
            uuid = UUID.randomUUID().toString(),
            fileDocId = docId,
            fileName = fileName,
            displayName = displayName,
            author = lit.author,
            regionCount = lit.regions.size,
            blockCount = lit.blockCount(includeAir = false),
            format = lit.format.name,
            lastScannedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun resolveUniqueName(name: String): String {
        if (!storage.exists(name)) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        for (s in 1..999) {
            val candidate = "$base-$s$ext"
            if (!storage.exists(candidate)) return candidate
        }
        return "$base-${System.currentTimeMillis()}$ext"
    }

    private fun BlueprintMetaEntity.toMeta() = BlueprintMeta(
        uuid = uuid, fileDocId = fileDocId, fileName = fileName,
        displayName = displayName, author = author, regionCount = regionCount,
        blockCount = blockCount,
        format = runCatching { SchematicFormat.valueOf(format) }.getOrDefault(SchematicFormat.Unknown),
    )

    private fun BlueprintMetaEntity.toEntity() = this

    suspend fun clearAllBlueprints(): Int = withContext(Dispatchers.IO) {
        val count = metaDao.getAll().size
        metaDao.deleteAll()
        count
    }

    // ── Backup ──

    data class BackupResult(val fileCount: Int, val totalBytes: Long)

    suspend fun estimateBackupSize(): Pair<Int, Long> = withContext(Dispatchers.IO) {
        val meta = metaDao.getAll()
        var total = 0L
        for (m in meta) {
            total += storage.list().firstOrNull { it.docId == m.fileDocId }?.size ?: 0L
        }
        meta.size to total
    }

    suspend fun backupToZip(onProgress: (Int, Int) -> Unit): Result<BackupResult> = withContext(Dispatchers.IO) {
        try {
            val reg = metaDao.getAll()
            if (reg.isEmpty()) return@withContext Result.failure(IllegalStateException("empty"))
            val ts = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
            val zb = ByteArrayOutputStream()
            ZipOutputStream(zb).use { z ->
                reg.forEachIndexed { i, m ->
                    val b = storage.read(m.fileDocId)
                    z.putNextEntry(java.util.zip.ZipEntry(m.fileName).apply { time = System.currentTimeMillis() })
                    z.write(b)
                    z.closeEntry()
                    onProgress(i + 1, reg.size)
                }
                z.finish()
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "BlockPrintCat-Backup-$ts.zip")
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/")
                }
                appContext.contentResolver.openOutputStream(
                    appContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)!!
                )?.use { it.write(zb.toByteArray()) }
            }
            Result.success(BackupResult(reg.size, zb.size().toLong()))
        } catch (e: Exception) {
            Log.e(TAG, "backupToZip", e)
            Result.failure(e)
        }
    }
}
