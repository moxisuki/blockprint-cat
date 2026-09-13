package io.github.moxisuki.blockprint.cat.app.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackRepository
import io.github.moxisuki.blockprint.cat.app.feature.detail.BlueprintNamespaceItem
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

@HiltViewModel
internal class CommunityDetailViewModel @Inject constructor(
    private val detailInteractor: CommunityDetailInteractor,
    private val resourcePackRepository: ResourcePackRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CommunityDetailState())
    val state: StateFlow<CommunityDetailState> = _state.asStateFlow()

    private var loadedBlueprintId: String? = null
    private var resourcePackObservationJob: Job? = null

    fun setSeed(seed: CommunityDetailSeed) {
        if (loadedBlueprintId == seed.blueprintId && _state.value.seed == seed) return
        resourcePackObservationJob?.cancel()
        loadedBlueprintId = seed.blueprintId
        _state.value = CommunityDetailState(seed = seed)
        resourcePackObservationJob = viewModelScope.launch {
            resourcePackRepository.installedPacks.collect { packs ->
                val installedNamespaces = packs
                    .asSequence()
                    .flatMap { it.namespaces.asSequence() }
                    .map(String::normalizeNamespace)
                    .toSet()
                _state.update { state ->
                    state.copy(
                        namespaces = state.payload.namespaces.toNamespaceItems(installedNamespaces),
                    )
                }
            }
        }
        loadDetail(seed)
    }

    fun download() {
        val seed = _state.value.seed
        if (!seed.downloadable ||
            seed.source !in setOf(CommunitySourceUi.MCS.displayName, CommunitySourceUi.CMS.displayName) ||
            seed.blueprintId.isBlank() ||
            _state.value.downloadState == CommunityDownloadState.Downloading
        ) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    downloadState = CommunityDownloadState.Downloading,
                    downloadProgress = 0f,
                    downloadMessage = null,
                )
            }
            runCatching {
                var lastProgressPercent = -1
                detailInteractor.download(seed) { bytes, total ->
                    if (total > 0L) {
                        val progress = (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        val percent = (progress * 100).toInt()
                        if (percent != lastProgressPercent) {
                            lastProgressPercent = percent
                            _state.update { state ->
                                state.copy(downloadProgress = progress)
                            }
                        }
                    } else if (_state.value.downloadProgress != null) {
                        _state.update { state ->
                            state.copy(downloadProgress = null)
                        }
                    }
                }
            }.onSuccess {
                _state.update {
                    it.copy(
                        downloadState = CommunityDownloadState.Downloaded,
                        downloadProgress = 1f,
                        downloadMessage = seed.title,
                    )
                }
            }.onFailure { error ->
                if (error !is CancellationException) {
                    _state.update {
                        it.copy(
                            downloadState = CommunityDownloadState.Failed,
                            downloadProgress = null,
                            downloadMessage = error.toDisplayMessage(),
                        )
                    }
                }
            }
        }
    }

    private fun loadDetail(seed: CommunityDetailSeed) {
        if (seed.blueprintId.isBlank()) return
        viewModelScope.launch {
            _state.update {
                it.copy(isLoadingDetail = true, detailErrorMessage = null)
            }
            runCatching {
                detailInteractor.load(seed)
            }.onSuccess { payload ->
                val installedNamespaces = resourcePackRepository.installedPacks
                    .first()
                    .asSequence()
                    .flatMap { pack -> pack.namespaces.asSequence() }
                    .map(String::normalizeNamespace)
                    .toSet()
                _state.update {
                    it.copy(
                        seed = it.seed.copy(
                            coverUrl = payload.coverUrl ?: it.seed.coverUrl,
                            downloadable = payload.downloadable ?: it.seed.downloadable,
                            dimensions = payload.dimensions ?: it.seed.dimensions,
                            sizeText = payload.sizeText ?: it.seed.sizeText,
                            stress = payload.stress ?: it.seed.stress,
                            format = payload.viewerSourceFormat
                                ?.toCommunityBlueprintFormat()
                                ?.takeUnless { format -> format == BlueprintFormat.Unknown }
                                ?: it.seed.format,
                            categoryName = payload.categoryName ?: it.seed.categoryName,
                            formatLabel = payload.viewerSourceFormat ?: it.seed.formatLabel,
                        ),
                        payload = payload,
                        namespaces = payload.namespaces.toNamespaceItems(installedNamespaces),
                        isLoadingDetail = false,
                    )
                }
            }.onFailure { error ->
                if (error !is CancellationException) {
                    _state.update {
                        it.copy(
                            isLoadingDetail = false,
                            detailErrorMessage = error.toDisplayMessage(),
                        )
                    }
                }
            }
        }
    }
}

private fun List<String>.toNamespaceItems(
    installedNamespaces: Set<String>,
): List<BlueprintNamespaceItem> =
    asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .map(String::normalizeNamespace)
        .distinct()
        .sortedWith(compareBy<String> { it != "minecraft" }.thenBy { it })
        .map { namespace ->
            BlueprintNamespaceItem(
                namespace = namespace,
                isInstalled = namespace in installedNamespaces,
            )
        }
        .toList()

private fun String.normalizeNamespace(): String = trim().lowercase(Locale.ROOT)

private fun Throwable.toDisplayMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
