package io.github.moxisuki.blockprint.cat.ui.community

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import io.github.moxisuki.blockprint.cat.data.DispatcherProvider
import io.github.moxisuki.blockprint.cat.data.McschematicClient
import io.github.moxisuki.blockprint.cat.data.McschematicCookieStore
import io.github.moxisuki.blockprint.cat.data.community.CmsClient
import io.github.moxisuki.blockprint.cat.data.community.CmsCloudflareException
import io.github.moxisuki.blockprint.cat.data.community.CmsCookieStore
import io.github.moxisuki.blockprint.cat.data.community.CmsException
import io.github.moxisuki.blockprint.cat.data.community.CmsRepository
import io.github.moxisuki.blockprint.cat.data.error.AppError
import io.github.moxisuki.blockprint.cat.data.community.CommunityRepository
import io.github.moxisuki.blockprint.cat.data.community.CommunitySource
import io.github.moxisuki.blockprint.cat.data.community.CommunitySourcePersistence
import io.github.moxisuki.blockprint.cat.data.community.McschematicRepository
import io.github.moxisuki.blockprint.cat.data.community.UnifiedDetail
import io.github.moxisuki.blockprint.cat.data.community.UnifiedSchematic
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val PAGE_SIZE = 15

data class CommunityListState(
    val currentSource: CommunitySource = CommunitySource.MCS,
    val mcs: PerSourceState = PerSourceState(),
    val cms: PerSourceState = PerSourceState(),
    val isDownloading: Boolean = false,
    val downloadBytes: Long = -1L,
    val downloadTotal: Long = -1L,
    val downloadingName: String = "",
) {
    val active: PerSourceState get() =
        if (currentSource == CommunitySource.CMS) cms else mcs
}

data class PerSourceState(
    val ready: Boolean = false,
    val nickname: String = "",
    val loading: Boolean = false,
    val schematics: List<UnifiedSchematic> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val heatSort: Boolean = false,
    val filter: String = "",
    val filterDraft: String = "",
    val showFilter: Boolean = false,
    val error: io.github.moxisuki.blockprint.cat.data.error.AppError? = null,
)

internal fun CommunityListState.copyActive(
    block: PerSourceState.() -> PerSourceState,
): CommunityListState = when (currentSource) {
    CommunitySource.MCS -> copy(mcs = mcs.block())
    CommunitySource.CMS -> copy(cms = cms.block())
}

sealed class DownloadEvent {
    data class Progress(val schematicName: String, val bytes: Long) : DownloadEvent()
    data class Success(val schematic: UnifiedSchematic) : DownloadEvent()
    data class Failed(val schematicName: String, val error: AppError) : DownloadEvent()
}

@HiltViewModel
class CommunityViewModel @Inject constructor(
    app: Application,
    private val mcsRepo: McschematicRepository,
    private val cmsRepo: CmsRepository,
    private val cookieStore: McschematicCookieStore,
    private val cmsCookieStore: CmsCookieStore,
    private val communitySourcePersistence: CommunitySourcePersistence,
    private val blueprintManager: BlueprintManager,
    private val dispatcherProvider: DispatcherProvider,
) : AndroidViewModel(app) {

    private val TAG = "CommunityVM"

    private val _state = MutableStateFlow(CommunityListState())
    val state: StateFlow<CommunityListState> = _state.asStateFlow()

    private val _download = Channel<DownloadEvent>(
        capacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val download: Flow<DownloadEvent> = _download.receiveAsFlow()

    init {
        val savedSource = communitySourcePersistence.load()
        _state.value = _state.value.copy(currentSource = savedSource)
        cmsCookieStore.ensureLoaded()
        refreshLoginState()
    }

    private fun repositoryFor(source: CommunitySource): CommunityRepository =
        if (source == CommunitySource.CMS) cmsRepo else mcsRepo

    fun refreshLoginState() {
        val loggedIn = cookieStore.isLoggedIn()
        val nickname = cookieStore.nickname()
        _state.value = _state.value.copy(
            mcs = _state.value.mcs.copy(ready = loggedIn, nickname = nickname),
        )
    }

    fun switchSource(target: CommunitySource) {
        val s = _state.value
        if (s.currentSource == target && s.active.ready) return  // already ready, skip
        if (s.currentSource != target) {
            _state.value = s.copy(currentSource = target)
            communitySourcePersistence.save(target)
        }
        viewModelScope.launch {
            ensureReady(target)
            val st = _state.value
            if (st.currentSource == target && st.active.ready && st.active.schematics.isEmpty()) {
                refresh()
            }
        }
    }

    private suspend fun ensureReady(source: CommunitySource) {
        _state.value = _state.value.copyActive { copy(loading = true) }
        val repo = repositoryFor(source)
        val ok = runCatching { repo.isAvailable() }.getOrDefault(false)
        _state.value = when (source) {
            CommunitySource.MCS -> _state.value.copy(mcs = _state.value.mcs.copy(ready = ok, loading = false))
            CommunitySource.CMS -> _state.value.copy(cms = _state.value.cms.copy(ready = ok, loading = false))
        }
        if (source == CommunitySource.CMS) {
            cmsCookieStore.saveToDisk()
        }
    }

    fun refresh(
        heatSort: Boolean = _state.value.active.heatSort,
        filter: String = _state.value.active.filter,
    ) {
        val srcAtStart = _state.value.currentSource
        val repo = repositoryFor(srcAtStart)
        viewModelScope.launch {
            _state.value = _state.value.copyActive {
                copy(loading = true, error = null, heatSort = heatSort, filter = filter)
            }
            try {
                val total = runCatching { repo.listCount(filter) }.getOrDefault(-1)
                val first = repo.list(begin = 0, filter = filter, heatSort = heatSort)
                if (_state.value.currentSource != srcAtStart) return@launch
                _state.value = _state.value.copyActive {
                    copy(
                        loading = false,
                        schematics = first,
                        total = total,
                        hasMore = if (total > 0) first.size < total
                                  else first.size == CmsClient.PAGE_SIZE,
                    )
                }
            } catch (e: Exception) {
                if (_state.value.currentSource != srcAtStart) return@launch
                _state.value = _state.value.copyActive {
                    copy(loading = false, error = AppError.toAppError(e))
                }
            }
        }
    }

    fun loadMore() {
        val s = _state.value
        val active = s.active
        if (active.loading || !active.hasMore) return
        val srcAtStart = s.currentSource
        val repo = repositoryFor(srcAtStart)
        viewModelScope.launch {
            _state.value = _state.value.copyActive { copy(loading = true, error = null) }
            try {
                val more = repo.list(
                    begin = active.schematics.size,
                    filter = active.filter,
                    heatSort = active.heatSort,
                )
                if (_state.value.currentSource != srcAtStart) return@launch
                _state.value = _state.value.copyActive {
                    val combined = schematics + more
                    copy(
                        loading = false,
                        schematics = combined,
                        hasMore = if (total > 0) combined.size < total
                                  else more.size == CmsClient.PAGE_SIZE,
                    )
                }
            } catch (e: Exception) {
                if (_state.value.currentSource != srcAtStart) return@launch
                _state.value = _state.value.copyActive {
                    copy(loading = false, error = AppError.toAppError(e))
                }
            }
        }
    }

    fun toggleHeatSort() = refresh(heatSort = !_state.value.active.heatSort)

    fun applyFilter(filter: String) {
        _state.value = _state.value.copyActive { copy(showFilter = false, filterDraft = filter) }
        refresh(filter = filter)
    }

    fun toggleFilter() {
        _state.value = _state.value.copyActive {
            copy(showFilter = !showFilter, filterDraft = if (!showFilter) filter else "")
        }
    }

    fun setFilterDraft(draft: String) {
        _state.value = _state.value.copyActive { copy(filterDraft = draft) }
    }

    fun clearFilter() {
        _state.value = _state.value.copyActive {
            copy(filter = "", filterDraft = "", showFilter = false)
        }
        refresh(filter = "")
    }

    fun logout() {
        if (_state.value.currentSource != CommunitySource.MCS) return
        cookieStore.clear()
        _state.value = _state.value.copy(
            mcs = PerSourceState(),
        )
    }

    private fun toAppError(e: Throwable): AppError = AppError.toAppError(e)

    // ── Detail ──

    private val _detail = MutableStateFlow(
        UnifiedDetail(source = CommunitySource.MCS, id = "")
    )
    val detail: StateFlow<UnifiedDetail> = _detail.asStateFlow()

    fun loadDetail(source: CommunitySource, id: String) {
        _detail.value = UnifiedDetail(source = source, id = id, loading = true)
        val repo = repositoryFor(source)
        viewModelScope.launch {
            try {
                val d = repo.loadDetail(id)
                if (_detail.value.source != source || _detail.value.id != id) return@launch
                _detail.value = d.copy(loading = false)
            } catch (e: Exception) {
                if (_detail.value.source != source || _detail.value.id != id) return@launch
                _detail.value = _detail.value.copy(
                    loading = false, error = AppError.toAppError(e),
                )
            }
        }
    }

    fun loadPreview(source: CommunitySource, id: String) {
        if (source == CommunitySource.CMS) return
        _detail.value = _detail.value.copy(
            previewLoading = true,
            previewMissing = false,
            previewBitmap = null,
        )
        val repo = repositoryFor(source)
        viewModelScope.launch(dispatcherProvider.default) {
            try {
                val bytes = repo.loadPreview(id, _detail.value)
                if (_detail.value.source != source || _detail.value.id != id) return@launch
                if (bytes == null) {
                    _detail.value = _detail.value.copy(
                        previewLoading = false, previewMissing = true,
                    )
                } else {
                    val bmp = withContext(dispatcherProvider.default) {
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                    _detail.value = _detail.value.copy(
                        previewLoading = false,
                        previewBitmap = bmp?.asImageBitmap(),
                    )
                }
            } catch (e: Exception) {
                if (_detail.value.source != source || _detail.value.id != id) return@launch
                _detail.value = _detail.value.copy(
                    previewLoading = false, previewMissing = true,
                )
            }
        }
    }

    // ── Download ──

    fun downloadSchematic(context: Context, schematic: UnifiedSchematic) {
        val repo = repositoryFor(schematic.source)
        viewModelScope.launch(Dispatchers.IO) {
            val displayName = computeDisplayName(schematic)
            _state.value = _state.value.copy(isDownloading = true, downloadBytes = 0L, downloadTotal = -1L, downloadingName = displayName)
            _download.send(DownloadEvent.Progress(displayName, 0L))
            try {
                val targetDir = File(context.cacheDir, "blockprintcat-downloads").also { it.mkdirs() }
                val file = repo.downloadToFile(context, schematic, _detail.value.takeIf { it.id == schematic.id }, targetDir) { bytes, total ->
                    _state.value = _state.value.copy(downloadBytes = bytes, downloadTotal = total)
                    _download.send(DownloadEvent.Progress(displayName, bytes))
                }
                val fileBytes = file.readBytes()
                file.delete()
                val meta = withContext(Dispatchers.IO) { blueprintManager.ingest(displayName, fileBytes) }
                _state.value = _state.value.copy(isDownloading = false, downloadBytes = -1L, downloadingName = "")
                Log.i(TAG, "download OK: ${schematic.id} → ${meta.uuid}")
                _download.send(DownloadEvent.Success(schematic))
            } catch (e: Exception) {
                Log.e(TAG, "downloadSchematic failed: ${schematic.id} ${schematic.name}", e)
                _state.value = _state.value.copy(
                    isDownloading = false, downloadBytes = -1L, downloadingName = "",
                )
                _download.send(DownloadEvent.Failed(schematic.name, AppError.toAppError(e)))
            }
        }
    }

    private fun computeDisplayName(s: UnifiedSchematic): String = when (s.source) {
        CommunitySource.MCS -> {
            val suffix = McschematicClient.suffixForSchematicType(s.typeForSuffix)
            val bare = s.name
                .removeSuffix(".litematic").removeSuffix(".schematic")
                .removeSuffix(".schem").removeSuffix(".nbt")
            "$bare$suffix"
        }
        CommunitySource.CMS -> {
            if (s.name.endsWith(".nbt", ignoreCase = true)) s.name
            else "${s.name}.nbt"
        }
    }
}
