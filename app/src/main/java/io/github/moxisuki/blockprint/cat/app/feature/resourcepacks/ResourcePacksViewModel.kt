package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackRepository
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "ResourcePacksViewModel"

@HiltViewModel
class ResourcePacksViewModel @Inject constructor(
    private val repository: ResourcePackRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(ResourcePacksState())
    val state: StateFlow<ResourcePacksState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            combine(repository.installedPacks, repository.activeInstalls) { installed, active ->
                installed to active
            }.collect { (installed, active) ->
                _state.update { it.copy(installed = installed, activeInstalls = active) }
            }
        }
    }

    fun onAction(action: ResourcePacksAction) {
        when (action) {
            ResourcePacksAction.Opened -> Unit
            ResourcePacksAction.VanillaDownload, ResourcePacksAction.VanillaRedownload -> viewModelScope.launch {
                Log.i(TAG, "vanilla install requested action=$action")
                runCatching { repository.installVanilla() }
                    .onFailure { t ->
                        Log.e(TAG, "vanilla install request failed", t)
                        _state.update { it.copy(feedback = ResourcePacksFeedback.Error(t.message.orEmpty())) }
                    }
            }
            ResourcePacksAction.VanillaDelete -> _state.update {
                it.copy(
                    pendingDelete = PendingResourcePackDelete(
                        id = ResourcePackId.Vanilla,
                        displayName = context.getString(R.string.resourcepacks_vanilla_name),
                    ),
                )
            }
            ResourcePacksAction.VanillaCancel -> repository.cancel(ResourcePackId.Vanilla)

            is ResourcePacksAction.ModRedownload -> viewModelScope.launch {
                Log.i(TAG, "mod reinstall requested id=${action.id.value}")
                runCatching { repository.reinstall(action.id) }
                    .onFailure { t ->
                        Log.e(TAG, "mod reinstall request failed id=${action.id.value}", t)
                        _state.update { it.copy(feedback = ResourcePacksFeedback.Error(t.message.orEmpty())) }
                    }
            }
            is ResourcePacksAction.ModDelete -> {
                val displayName = _state.value.installed.firstOrNull { it.id == action.id }?.displayName ?: action.id.modSlug
                _state.update {
                    it.copy(
                        pendingDelete = PendingResourcePackDelete(
                            id = action.id,
                            displayName = displayName,
                        ),
                    )
                }
            }
            is ResourcePacksAction.ModCancel -> repository.cancel(action.id)
            ResourcePacksAction.PendingDeleteConfirm -> viewModelScope.launch {
                val pending = _state.value.pendingDelete ?: return@launch
                runCatching { repository.delete(pending.id) }
                    .onFailure { t -> _state.update { it.copy(feedback = ResourcePacksFeedback.Error(t.message.orEmpty())) } }
                _state.update { it.copy(pendingDelete = null) }
            }
            ResourcePacksAction.PendingDeleteDismiss -> _state.update { it.copy(pendingDelete = null) }

            is ResourcePacksAction.ModSearchOpen -> {
                val query = normalizeNamespaceQuery(action.initialQuery)
                _state.update { it.copy(modSearch = ModSearchState.Results(emptyList(), query)) }
                if (query.isNotBlank()) searchMods(query)
            }
            is ResourcePacksAction.ModSearchQueryChanged -> {
                val current = _state.value.modSearch
                if (current is ModSearchState.Results) {
                    _state.update { it.copy(modSearch = current.copy(query = action.query)) }
                }
            }
            ResourcePacksAction.ModSearchSubmit -> searchMods()
            is ResourcePacksAction.ModSearchHitSelected -> {
                val results = _state.value.modSearch as? ModSearchState.Results ?: return
                fetchVersionsFor(action.hit, results)
            }
            is ResourcePacksAction.ModSearchVersionSelected -> viewModelScope.launch {
                val hit = (_state.value.modSearch as? ModSearchState.Versions)?.hit ?: return@launch
                Log.i(TAG, "mod install requested slug=${hit.slug} version=${action.version.name}")
                runCatching { repository.installMod(hit, action.version) }
                    .onFailure { t ->
                        Log.e(TAG, "mod install request failed slug=${hit.slug}", t)
                        _state.update { it.copy(feedback = ResourcePacksFeedback.Error(t.message.orEmpty())) }
                    }
                _state.update { it.copy(modSearch = ModSearchState.Closed) }
            }
            ResourcePacksAction.ModSearchBack -> _state.update {
                val previous = when (val current = it.modSearch) {
                    is ModSearchState.LoadingVersions -> current.previousResults
                    is ModSearchState.Versions -> current.previousResults
                    is ModSearchState.VersionError -> current.previousResults
                    else -> null
                }
                it.copy(modSearch = previous ?: ModSearchState.Closed)
            }
            ResourcePacksAction.ModSearchClose -> {
                searchJob?.cancel()
                _state.update { it.copy(modSearch = ModSearchState.Closed) }
            }

            ResourcePacksAction.DeleteAllClicked -> _state.update { it.copy(isDeleteAllConfirmVisible = true) }
            ResourcePacksAction.DeleteAllConfirm -> viewModelScope.launch {
                runCatching { repository.deleteAll() }
                    .onFailure { t -> _state.update { it.copy(feedback = ResourcePacksFeedback.Error(t.message.orEmpty())) } }
                _state.update { it.copy(isDeleteAllConfirmVisible = false) }
            }
            ResourcePacksAction.DeleteAllDismissed -> _state.update { it.copy(isDeleteAllConfirmVisible = false) }

            ResourcePacksAction.FeedbackDismissed -> _state.update { it.copy(feedback = null) }
        }
    }

    private fun searchMods(queryOverride: String? = null) {
        val query = queryOverride ?: (_state.value.modSearch as? ModSearchState.Results)?.query ?: return
        if (query.isBlank()) return
        searchJob?.cancel()
        _state.update { it.copy(modSearch = ModSearchState.Searching) }
        searchJob = viewModelScope.launch {
            val hits = runCatching { repository.searchMods(query) }
                .onFailure { Log.e(TAG, "mod search failed query=$query", it) }
                .getOrDefault(emptyList())
            _state.update { state ->
                if (state.modSearch == ModSearchState.Searching) {
                    state.copy(modSearch = ModSearchState.Results(hits, query))
                } else {
                    state
                }
            }
        }
    }

    private fun normalizeNamespaceQuery(namespace: String): String = namespace
        .trim()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotBlank() }
        .joinToString(" ")

    private fun fetchVersionsFor(
        hit: io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit,
        previousResults: ModSearchState.Results,
    ) {
        _state.update { it.copy(modSearch = ModSearchState.LoadingVersions(hit, previousResults)) }
        viewModelScope.launch {
            val versions = runCatching { repository.fetchModVersions(hit, mcVersion = null) }
                .onFailure { Log.e(TAG, "mod versions failed slug=${hit.slug}", it) }
                .getOrDefault(emptyList())
            val loading = _state.value.modSearch as? ModSearchState.LoadingVersions
            if (loading?.hit?.projectId != hit.projectId) {
                return@launch
            }
            _state.update {
                if (versions.isEmpty()) {
                    it.copy(
                        modSearch = ModSearchState.VersionError(
                            hit,
                            context.getString(R.string.resourcepacks_no_versions),
                            loading.previousResults,
                        ),
                    )
                } else {
                    it.copy(modSearch = ModSearchState.Versions(hit, versions, loading.previousResults))
                }
            }
        }
    }
}
