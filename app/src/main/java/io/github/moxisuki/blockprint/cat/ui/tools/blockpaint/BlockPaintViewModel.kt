package io.github.moxisuki.blockprint.cat.ui.tools.blockpaint

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.data.blockpaint.PaintingDao
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockCatalog
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.ExportPayloadCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import java.util.UUID

/**
 * BlockPaint 状态机。
 *
 * 与 ITB/TTB 不同：paint 是用户主导的离散操作（点一下画一下），所以**不走 debounce**。
 * 渲染 resultBitmap 只在用户点"导出"时一次性发生（避免每次画笔落下都触发 ARGB 写入）。
 */
@HiltViewModel
class BlockPaintViewModel @Inject constructor(
    @ApplicationContext private val context: Context?,
    private val paintingDao: PaintingDao? = null,
) : ViewModel() {

    @VisibleForTesting
    constructor() : this(context = null, paintingDao = null)

    private val _state = MutableStateFlow(BlockPaintState())
    val state: StateFlow<BlockPaintState> = _state.asStateFlow()

    private val _scale = MutableStateFlow(1f)
    val scale: StateFlow<Float> = _scale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    fun transform(scale: Float, panOffset: Offset) {
        _scale.value = scale
        _panOffset.value = panOffset
    }

    fun zoomIn() {
        _scale.value = min(MAX_SCALE_VALUE, _scale.value * 1.25f)
    }

    fun zoomOut() {
        _scale.value = max(MIN_SCALE_VALUE, _scale.value / 1.25f)
    }

    fun zoomReset() {
        _scale.value = 1f
        _panOffset.value = Offset.Zero
    }

    private val _showPicker = MutableStateFlow(false)
    val showPicker: StateFlow<Boolean> = _showPicker.asStateFlow()
    fun openPicker() { _showPicker.value = true }
    fun closePicker() { _showPicker.value = false }

    private val _exportPayload = MutableStateFlow<String?>(null)
    val exportPayload: StateFlow<String?> = _exportPayload.asStateFlow()
    fun setExportPayload(payload: String?) { _exportPayload.value = payload }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateIn_(initial: T): StateFlow<T> {
        val state = MutableStateFlow(initial)
        viewModelScope.launch { collect { state.value = it } }
        return state.asStateFlow()
    }

    private companion object {
        const val MIN_SCALE_VALUE = 0.5f
        const val MAX_SCALE_VALUE = 6f
    }

    /** 切换画布大小。保留原内容（截断或补空）。 */
    fun setSize(width: Int, height: Int) {
        val w = width.coerceIn(BlockPaintState.MIN_SIZE, BlockPaintState.MAX_SIZE)
        val h = height.coerceIn(BlockPaintState.MIN_SIZE, BlockPaintState.MAX_SIZE)
        _state.update { s ->
            val next = BlockPaintState.emptyGrid(w, h)
            val copyW = minOf(w, s.grid.size)
            val copyH = if (s.grid.isNotEmpty()) minOf(h, s.grid[0].size) else 0
            for (x in 0 until copyW) {
                for (y in 0 until copyH) {
                    next[x][y] = s.grid[x][y]
                }
            }
            s.copy(width = w, height = h, grid = next, lastUpdatedAt = System.currentTimeMillis())
        }
    }

    fun setTool(tool: PaintTool) {
        _state.update { it.copy(tool = tool) }
    }

    fun selectBlock(blockId: String?) {
        _state.update {
            val resolved = if (blockId != null && BlockCatalog.all.any { e -> e.id == blockId }) {
                blockId
            } else {
                null
            }
            it.copy(selectedBlockId = resolved, tool = PaintTool.Paint)
        }
    }

    private val _paintings = MutableStateFlow<List<Painting>>(emptyList())
    val paintings: StateFlow<List<Painting>> = _paintings.asStateFlow()

    private val _currentPaintingId = MutableStateFlow("")
    val currentPaintingId: StateFlow<String> = _currentPaintingId.asStateFlow()

    /** 切换或新建时被设为 true，让 BlockPaintScreen 拉出 list bottom sheet。 */
    private val _showPaintingList = MutableStateFlow(false)
    val showPaintingList: StateFlow<Boolean> = _showPaintingList.asStateFlow()
    fun openPaintingList() { _showPaintingList.value = true }
    fun closePaintingList() { _showPaintingList.value = false }

    /**
     * 在 (x, y) 处画一笔。坐标超过画布范围直接忽略。
     * 连续 tap+drag 时由 UI 端每个 PointerEvent 触发一次。
     */
    fun paint(x: Int, y: Int) {
        val s = _state.value
        if (x !in 0 until s.width || y !in 0 until s.height) return
        val target: String? = when (s.tool) {
            PaintTool.Paint -> s.selectedBlockId
            PaintTool.Erase -> null
        }
        if (s.grid[x][y] == target) return
        val nextGrid = Array(s.width) { ix ->
            if (ix == x) {
                Array(s.height) { iy -> if (iy == y) target else s.grid[ix][iy] }
            } else s.grid[ix]
        }
        _state.update { it.copy(grid = nextGrid, lastUpdatedAt = System.currentTimeMillis()) }
        saveCurrentStateToPainting()
    }

    /** 整张画布清空（设回空气）。 */
    fun clearCanvas() {
        _state.update {
            it.copy(
                grid = BlockPaintState.emptyGrid(it.width, it.height),
                lastUpdatedAt = System.currentTimeMillis(),
            )
        }
        saveCurrentStateToPainting()
    }

    /** 把 state 同步回当前 painting 的字段（写 Room）。 */
    private fun saveCurrentStateToPainting() {
        val dao = paintingDao ?: return
        val s = _state.value
        val currentId = _currentPaintingId.value
        if (currentId.isEmpty()) return
        viewModelScope.launch {
            val current = paintingDao.getById(currentId) ?: return@launch
            dao.upsert(
                current.copy(
                    width = s.width,
                    height = s.height,
                    gridCsv = gridToCsv(s.grid),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /**
     * 新建一张图画。会保存当前到列表，再切到空白新建项。
     * 如果当前已经是空白且未改过，复用同一空白（避免一堆空白图）。
     */
    fun newPainting(name: String): String {
        val dao = paintingDao ?: return _currentPaintingId.value
        saveCurrentStateToPainting()
        val newP = Painting.empty().copy(name = name)
        _state.update {
            it.copy(
                width = newP.width,
                height = newP.height,
                grid = Array(newP.width) { arrayOfNulls<String>(newP.height) },
                resultBitmap = null,
                resultTotalBlocks = 0,
                resultMaterialCounts = emptyMap(),
                lastUpdatedAt = System.currentTimeMillis(),
            )
        }
        _currentPaintingId.value = newP.id
        viewModelScope.launch {
            dao.upsert(
                newP.toEntity(
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
        return newP.id
    }

    /** 切换到指定 id 的图画：先保存当前 state，再覆盖 state 为目标 grid。 */
    fun switchPainting(id: String) {
        val dao = paintingDao
        if (id == _currentPaintingId.value) {
            closePaintingList()
            return
        }
        saveCurrentStateToPainting()
        val target = _paintings.value.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                width = target.width,
                height = target.height,
                grid = Array(target.width) { x -> Array(target.height) { y -> target.grid[x][y] } },
                resultBitmap = null,
                resultTotalBlocks = 0,
                resultMaterialCounts = emptyMap(),
                lastUpdatedAt = System.currentTimeMillis(),
            )
        }
        _currentPaintingId.value = id
        closePaintingList()
    }

    fun renamePainting(id: String, newName: String) {
        if (newName.isBlank()) return
        val dao = paintingDao ?: return
        viewModelScope.launch {
            val current = dao.getById(id) ?: return@launch
            dao.upsert(current.copy(name = newName.trim(), updatedAt = System.currentTimeMillis()))
        }
    }

    fun deletePainting(id: String) {
        if (_paintings.value.size <= 1) return  // 至少保留一张
        viewModelScope.launch { paintingDao?.delete(id) }
        if (_currentPaintingId.value == id) {
            val firstId = _paintings.value.firstOrNull { it.id != id }?.id
            if (firstId != null) switchPainting(firstId) else closePaintingList()
        } else {
            closePaintingList()
        }
    }

    fun savePng(context: Context) {
        val bmp = _state.value.resultBitmap ?: return
        // 放大到每格至少 8px，不然 32×32 的图肉眼不可见
        val scale = maxOf(1, 256 / maxOf(bmp.width, bmp.height))
        val scaled = android.graphics.Bitmap.createScaledBitmap(bmp, bmp.width * scale, bmp.height * scale, false)
        val file = java.io.File(context.cacheDir, "blockpaint_${System.currentTimeMillis()}.png")
        java.io.FileOutputStream(file).use { scaled.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        if (scaled != bmp) scaled.recycle()
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, null))
    }

    val currentPaintingName: StateFlow<String> = kotlinx.coroutines.flow.combine(
        _paintings, _currentPaintingId
    ) { list, id ->
        list.firstOrNull { it.id == id }?.name
            ?: (context?.getString(io.github.moxisuki.blockprint.cat.R.string.bp_default_painting_name, 0) ?: "Untitled")
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "Untitled")

    init {
        val dao = paintingDao
        if (dao != null) {
            startAutoRestore(dao)
        }
    }

    private fun startAutoRestore(dao: PaintingDao) {
        viewModelScope.launch {
            dao.observeAll()
                .map { entities -> entities.map { it.toPainting() } }
                .collect { list ->
                    _paintings.value = list
                    val currentId = _currentPaintingId.value
                    // 只在启动初始化时才覆盖 state（currentId 为空且 list 非空）
                    // 后续 Room 变化（保存写入）不再覆盖用户当前绘画
                    if (currentId.isEmpty() && list.isNotEmpty()) {
                        val first = list.first()
                        _currentPaintingId.value = first.id
                        _state.update {
                            it.copy(
                                width = first.width,
                                height = first.height,
                                grid = Array(first.width) { x -> Array(first.height) { y -> first.grid[x][y] } },
                                resultBitmap = null,
                                resultTotalBlocks = 0,
                                resultMaterialCounts = emptyMap(),
                                lastUpdatedAt = System.currentTimeMillis(),
                            )
                        }
                    } else if (currentId.isEmpty() && list.isEmpty()) {
                        val ctx = context
                        val pName = if (ctx != null) ctx.getString(io.github.moxisuki.blockprint.cat.R.string.bp_default_painting_name, 1) else "Untitled 1"
                        val p = Painting.empty().copy(name = pName)
                        dao.upsert(
                            p.toEntity(System.currentTimeMillis(), System.currentTimeMillis())
                        )
                    }
                }
        }
    }

    /**
     * 用户点导出时调用：在 IO 线程渲染 grid → 1:1 bitmap，并把 materials 统计写回 state。
     * 完成后 UI 端根据 state.resultBitmap != null 启用导出按钮（弹底部 sheet）。
     */
    fun prepareExport() {
        val s = _state.value
        val ctx = context ?: return
        _state.update { it.copy(isUpdating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val (bitmap, materials) = withContext(Dispatchers.IO) {
                    BlockPaintRenderer.renderToBitmap(ctx, s.grid)
                }
                val totalNonAir = materials.values.sum()
                _state.update {
                    it.copy(
                        isUpdating = false,
                        resultBitmap = bitmap,
                        resultWidth = bitmap.width,
                        resultHeight = bitmap.height,
                        resultTotalBlocks = totalNonAir,
                        resultMaterialCounts = materials,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isUpdating = false,
                        errorMessage = e.message ?: "Render failed",
                    )
                }
            }
        }
    }

    /**
     * 把当前 resultBitmap 编码为可导航传输的 base64 字符串。
     * 如果 resultBitmap 还没准备好（用户没点导出）则返回 null。
     */
    fun encodeForExport(): String? {
        val s = _state.value
        val bitmap: Bitmap = s.resultBitmap ?: return null
        // 找出 painted blocks 涉及的 group（用于 BP 预览页重建时给 engine）
        val groupKeys = BlockCatalog.all
            .filter { it.id in s.resultMaterialCounts }
            .map { it.group.key }
            .distinct()
        // 逐格 blockId 列表（行优先，"" 表示空气）
        val ids = mutableListOf<String>()
        val g = s.grid
        for (y in 0 until s.resultHeight) {
            for (x in 0 until s.resultWidth) {
                ids.add(g[x][y] ?: "")
            }
        }
        return ExportPayloadCodec.encode(
            bitmap = bitmap,
            width = s.resultWidth,
            height = s.resultHeight,
            totalBlocks = s.resultTotalBlocks,
            materials = s.resultMaterialCounts,
            ditherMethod = io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.DitherMethod.NONE.id,
            transparencyEnabled = true,
            transparencyTolerance = 1,
            selectedGroups = groupKeys,
            sourceWidth = s.width,
            sourceHeight = s.height,
            blockIds = ids,
        )
    }
}
