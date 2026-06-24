package io.github.moxisuki.blockprint.cat.data.blueprint

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import io.github.moxisuki.blockprint.core.BlueprintConverter
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

/**
 * Build the output filename for a converted blueprint.
 *
 * Rules:
 * - stem is everything before the last '.' in the source name (or the whole
 *   name if there is no '.'), so ".litematic" / ".schematic" / ".schem" /
 *   ".nbt" / ".json" are stripped and the stem (which may contain its own
 *   dots) is preserved.
 * - "_converted" is appended between stem and the caller-supplied
 *   [extension] (e.g. "litematic", "schem", "schematic", "nbt", "json").
 *   The extension is supplied by the caller (the UI dialog) because
 *   Sponge format on blockprint-core's side covers both .schem and
 *   .schematic; the extension is a user-facing choice, not a
 *   format-derived one.
 *
 * The result is a *candidate* name — uniqueness is checked separately
 * via `resolveUniqueName` before writing.
 */
internal fun outputFileName(sourceName: String, extension: String): String {
    val dot = sourceName.lastIndexOf('.')
    val stem = if (dot > 0) sourceName.substring(0, dot) else sourceName
    return "${stem}_converted.$extension"
}

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

    suspend fun ingest(name: String, bytes: ByteArray, onProgress: ((Long, Long) -> Unit)? = null): BlueprintMeta = withContext(Dispatchers.IO) {
        // Write first so progress callback fires immediately (no blocking SAF query before it)
        val docId = storage.write(name, bytes, onProgress)
        val lit = LitematicReader.readLenient(bytes)
        val meta = metaFromLit(lit, docId, name)
        metaDao.upsert(meta.toEntity())
        Log.d(TAG, "ingest: $name → ${meta.uuid}")
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

    /**
     * Convert an existing blueprint into [target] format and save the result
     * to the same folder. The new file is automatically ingested and will
     * appear in the local list via the standard `observeAll` flow.
     *
     * If [target] is not Litematica and the source has multiple regions,
     * only the primary (first) region is written — `BlueprintConverter`
     * rejects multi-region inputs for Sponge / Structure / BuildingHelper.
     * Single-region inputs are written verbatim.
     *
     * The output filename is `<stem>_converted.<extension>` with `-1`, `-2`, ...
     * appended on collision (see [resolveUniqueName]).
     *
     * @throws LitematicException for read-side target formats
     *   (PartialNbt / Unknown) — the UI should never offer these, but we
     *   surface the error rather than silently misbehave.
     * @throws java.io.IOException / [IllegalStateException] from SAF on
     *   disk failure.
     */
    suspend fun convert(uuid: String, target: SchematicFormat, extension: String): Result<BlueprintMeta> = withContext(Dispatchers.IO) {
        val t0 = System.currentTimeMillis()
        var t: Long
        runCatching {
            t = System.currentTimeMillis()
            val entity = metaDao.getByUuid(uuid)
                ?: throw IllegalStateException("Blueprint not found: $uuid")
            Log.d(TAG, "convert stage[1] metaDao.getByUuid: ${System.currentTimeMillis() - t} ms")

            t = System.currentTimeMillis()
            val srcBytes = storage.read(entity.fileDocId)
            Log.d(TAG, "convert stage[2] storage.read(source, ${srcBytes.size} bytes): ${System.currentTimeMillis() - t} ms")

            t = System.currentTimeMillis()
            val lit = LitematicReader.readLenient(srcBytes)
            Log.d(TAG, "convert stage[3] LitematicReader.readLenient: ${System.currentTimeMillis() - t} ms")

            t = System.currentTimeMillis()
            // For non-Litematica targets, BlueprintConverter rejects multi-region
            // input. Fall back to the primary region so the user still gets a
            // usable file rather than an exception.
            val sourceForConvert = if (target != SchematicFormat.Litematica && lit.regions.size > 1) {
                lit.copy(regions = listOfNotNull(lit.primaryRegion))
            } else {
                lit
            }
            Log.d(TAG, "convert stage[4] multi-region fallback: ${System.currentTimeMillis() - t} ms")

            t = System.currentTimeMillis()
            val candidate = outputFileName(entity.fileName, extension)
            val finalName = resolveUniqueName(candidate)
            Log.d(TAG, "convert stage[5] outputFileName+resolveUniqueName: ${System.currentTimeMillis() - t} ms")

            t = System.currentTimeMillis()
            val newDocId = storage.writeStream(finalName) { out ->
                BlueprintConverter.convert(sourceForConvert, target, out)
            }
            Log.d(TAG, "convert stage[6] storage.writeStream+convert: ${System.currentTimeMillis() - t} ms")

            t = System.currentTimeMillis()
            // Re-read the bytes the streamer just produced so `ingest` parses
            // the file's metadata (regions, blocks, format) and registers it in
            // the meta DAO. This is one extra read, but it's bounded — the new
            // file is on local storage and the SAF read is cheap.
            val newBytes = storage.read(newDocId)
            Log.d(TAG, "convert stage[7] storage.read(target, ${newBytes.size} bytes): ${System.currentTimeMillis() - t} ms")

            t = System.currentTimeMillis()
            val result = ingest(finalName, newBytes)
            Log.d(TAG, "convert stage[8] ingest: ${System.currentTimeMillis() - t} ms")

            Log.d(TAG, "convert TOTAL: ${System.currentTimeMillis() - t0} ms")
            result
        }
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
