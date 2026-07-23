package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.ResourcePackRepository
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ResourcePacksViewModel @Inject constructor(
    private val repository: ResourcePackRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ResourcePacksState())
    val state: StateFlow<ResourcePacksState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repository.installedPacks, repository.activeInstalls) { installed, active ->
                _state.value.copy(installed = installed, activeInstalls = active)
            }.collect { _state.value = it }
        }
    }

    fun onAction(action: ResourcePacksAction) {
        when (action) {
            ResourcePacksAction.Opened -> Unit
            ResourcePacksAction.VanillaDownload, ResourcePacksAction.VanillaRedownload -> viewModelScope.launch {
                runCatching { repository.installVanilla() }
                    .onFailure { t -> _state.update { it.copy(feedback = ResourcePacksFeedback.Error(t.message.orEmpty())) } }
            }
            ResourcePacksAction.VanillaDelete ->
                _state.update { it.copy(feedback = ResourcePacksFeedback.Info("确认删除原版？")) }
            ResourcePacksAction.VanillaDeleteConfirm -> viewModelScope.launch { repository.delete(ResourcePackId.Vanilla) }
            ResourcePacksAction.VanillaCancel -> repository.cancel(ResourcePackId.Vanilla)
            is ResourcePacksAction.VanillaFeedbackDismiss -> _state.update { it.copy(feedback = null) }

            is ResourcePacksAction.ModRedownload -> viewModelScope.launch { repository.reinstall(action.id) }
            is ResourcePacksAction.ModDelete ->
                _state.update { it.copy(feedback = ResourcePacksFeedback.Info("删除 ${action.id.modSlug}?")) }
            is ResourcePacksAction.ModDeleteConfirm -> viewModelScope.launch { repository.delete(action.id) }
            is ResourcePacksAction.ModCancel -> repository.cancel(action.id)

            ResourcePacksAction.ModSearchOpen -> _state.update { it.copy(modSearch = ModSearchState.Results(emptyList(), "")) }
            is ResourcePacksAction.ModSearchQueryChanged -> {
                val current = _state.value.modSearch
                if (current is ModSearchState.Results) {
                    _state.update { it.copy(modSearch = current.copy(query = action.query)) }
                }
            }
            ResourcePacksAction.ModSearchSubmit -> searchMods()
            is ResourcePacksAction.ModSearchHitSelected -> fetchVersionsFor(action.hit)
            is ResourcePacksAction.ModSearchVersionSelected -> viewModelScope.launch {
                val hit = (_state.value.modSearch as? ModSearchState.Versions)?.hit ?: return@launch
                runCatching { repository.installMod(hit, action.version) }
                _state.update { it.copy(modSearch = ModSearchState.Closed) }
            }
            ResourcePacksAction.ModSearchBack -> _state.update { it.copy(modSearch = ModSearchState.Closed) }
            ResourcePacksAction.ModSearchClose -> _state.update { it.copy(modSearch = ModSearchState.Closed) }

            ResourcePacksAction.DeleteAllClicked -> _state.update { it.copy(isDeleteAllConfirmVisible = true) }
            ResourcePacksAction.DeleteAllConfirm -> viewModelScope.launch { repository.deleteAll() }
            ResourcePacksAction.DeleteAllDismissed -> _state.update { it.copy(isDeleteAllConfirmVisible = false) }

            ResourcePacksAction.FeedbackDismissed -> _state.update { it.copy(feedback = null) }
        }
    }

    private fun searchMods() {
        val query = (_state.value.modSearch as? ModSearchState.Results)?.query ?: return
        _state.update { it.copy(modSearch = ModSearchState.Searching) }
        viewModelScope.launch {
            val hits = runCatching { repository.searchMods(query) }.getOrDefault(emptyList())
            _state.update { it.copy(modSearch = ModSearchState.Results(hits, query)) }
        }
    }

    private fun fetchVersionsFor(hit: io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit) {
        _state.update { it.copy(modSearch = ModSearchState.Searching) }
        viewModelScope.launch {
            val versions = runCatching { repository.fetchModVersions(hit.slug, mcVersion = null) }.getOrDefault(emptyList())
            _state.update {
                if (versions.isEmpty()) {
                    it.copy(modSearch = ModSearchState.VersionError("没有版本"))
                } else {
                    it.copy(modSearch = ModSearchState.Versions(hit, versions))
                }
            }
        }
    }
}
