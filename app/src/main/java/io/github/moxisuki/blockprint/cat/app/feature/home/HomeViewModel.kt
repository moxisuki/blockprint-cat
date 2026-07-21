package io.github.moxisuki.blockprint.cat.app.feature.home

import androidx.lifecycle.viewModelScope
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintImportPreview
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintRepository
import io.github.moxisuki.blockprint.cat.app.core.persistence.AppSettingsRepository
import io.github.moxisuki.blockprint.cat.app.feature.home.category.HomeCategoryId
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: AppSettingsRepository,
    private val blueprintRepository: BlueprintRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()
    private var pendingImportPreview: BlueprintImportPreview? = null

    init {
        onAction(HomeAction.Opened)
        observeSafDirectory()
        observeLocalBlueprints()
        observeRefreshState()
        observeCategories()
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.Opened -> Unit
            is HomeAction.SafDirectorySelected -> {
                viewModelScope.launch {
                    settingsRepository.setLocalBlueprintTree(
                        treeUri = action.treeUri,
                        treeDocumentId = action.treeDocumentId,
                    )
                }
            }
            is HomeAction.SourceSelected -> {
                _state.update { state ->
                    if (state.selectedSource == action.source) {
                        state
                    } else {
                        state.copy(
                            selectedSource = action.source,
                            selectedCategoryId = HomeCategoryId.All,
                            isCategoryManageVisible = false,
                            selectedBlueprintIds = emptySet(),
                            categoryMoveBlueprintIds = emptySet(),
                        )
                    }
                }
            }
            HomeAction.SearchToggled -> {
                _state.update { it.copy(isSearchExpanded = !it.isSearchExpanded) }
            }
            is HomeAction.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = action.query) }
            }
            HomeAction.SearchCleared -> {
                _state.update { it.copy(searchQuery = "") }
            }
            HomeAction.RefreshClicked -> {
                if (_state.value.selectedSource == HomeBlueprintSource.Local &&
                    _state.value.isSafDirectorySelected
                ) {
                    viewModelScope.launch {
                        blueprintRepository.refreshLocalBlueprints()
                    }
                }
            }
            is HomeAction.ImportFileSelected -> {
                viewModelScope.launch {
                    pendingImportPreview = null
                    _state.update {
                        it.copy(
                            importPreview = HomeImportPreview(
                                sourceUri = action.uri,
                                fileName = action.uri.substringAfterLast('/'),
                                displayName = action.uri.substringAfterLast('/'),
                                format = BlueprintFormat.Unknown.toHomeBlueprintFormat(),
                                author = "",
                                blockCount = "-",
                                regionCount = 0,
                                size = "-",
                            ),
                            isImportPreviewLoading = true,
                            isImporting = false,
                        )
                    }
                    runCatching {
                        blueprintRepository.readImportPreview(action.uri)
                    }.onSuccess { preview ->
                        pendingImportPreview = preview
                        _state.update {
                            it.copy(
                                importPreview = preview.toHomeImportPreview(),
                                isImportPreviewLoading = false,
                                isImporting = false,
                            )
                        }
                    }.onFailure { error ->
                        _state.update {
                            it.copy(
                                importPreview = it.importPreview?.copy(
                                    errorMessage = error.message.orEmpty().ifBlank {
                                        error::class.java.simpleName
                                    },
                                ),
                                isImportPreviewLoading = false,
                                isImporting = false,
                            )
                        }
                    }
                }
            }
            HomeAction.ImportConfirmed -> {
                val preview = pendingImportPreview ?: return
                viewModelScope.launch {
                    _state.update { it.copy(isImporting = true) }
                    runCatching {
                        blueprintRepository.importBlueprint(preview)
                    }.onSuccess {
                        pendingImportPreview = null
                        _state.update {
                            it.copy(
                                importPreview = null,
                                isImporting = false,
                                selectedSource = HomeBlueprintSource.Local,
                            )
                        }
                    }.onFailure { error ->
                        _state.update {
                            it.copy(
                                importPreview = it.importPreview?.copy(
                                    errorMessage = error.message.orEmpty().ifBlank {
                                        error::class.java.simpleName
                                    },
                                ),
                                isImporting = false,
                            )
                        }
                    }
                }
            }
            HomeAction.ImportDismissed -> {
                pendingImportPreview = null
                _state.update {
                    it.copy(
                        importPreview = null,
                        isImportPreviewLoading = false,
                        isImporting = false,
                    )
                }
            }
            HomeAction.CategoryBarToggled -> {
                _state.update { state ->
                    state.copy(
                        isCategoryBarVisible = !state.isCategoryBarVisible,
                        isCategoryManageVisible = if (state.isCategoryBarVisible) {
                            false
                        } else {
                            state.isCategoryManageVisible
                        },
                    )
                }
            }
            is HomeAction.CategorySelected -> {
                _state.update { it.copy(selectedCategoryId = action.categoryId) }
            }
            is HomeAction.CategoryManageVisibilityChanged -> {
                _state.update { it.copy(isCategoryManageVisible = action.visible) }
            }
            is HomeAction.CategoryCreated -> {
                val name = action.name.trim()
                if (name.isNotBlank()) {
                    viewModelScope.launch {
                        blueprintRepository.createCategory(name)
                    }
                }
            }
            is HomeAction.CategoryRenamed -> {
                val oldName = action.oldName
                val newName = action.newName.trim()
                if (oldName.isNotBlank() && newName.isNotBlank() && oldName != newName) {
                    viewModelScope.launch {
                        blueprintRepository.renameCategory(oldName, newName)
                    }
                    _state.update { state ->
                        state.copy(
                            selectedCategoryId = if (state.selectedCategoryId == oldName) newName else state.selectedCategoryId,
                        )
                    }
                }
            }
            is HomeAction.CategoryDeleted -> {
                val name = action.name
                if (name.isNotBlank()) {
                    _state.update { state ->
                        state.copy(
                            customCategories = state.customCategories.filterNot { it == name },
                            selectedCategoryId = if (state.selectedCategoryId == name) {
                                HomeCategoryId.All
                            } else {
                                state.selectedCategoryId
                            },
                        )
                    }
                    viewModelScope.launch {
                        blueprintRepository.deleteCategory(name)
                    }
                }
            }
            is HomeAction.BlueprintLongPressed -> {
                _state.update { state ->
                    state.copy(selectedBlueprintIds = state.selectedBlueprintIds + action.blueprintId)
                }
            }
            is HomeAction.BlueprintSelectionToggled -> {
                _state.update { state ->
                    val selectedIds = if (action.blueprintId in state.selectedBlueprintIds) {
                        state.selectedBlueprintIds - action.blueprintId
                    } else {
                        state.selectedBlueprintIds + action.blueprintId
                    }
                    state.copy(selectedBlueprintIds = selectedIds)
                }
            }
            HomeAction.BlueprintSelectionCleared -> {
                _state.update {
                    it.copy(
                        selectedBlueprintIds = emptySet(),
                        categoryMoveBlueprintIds = emptySet(),
                    )
                }
            }
            is HomeAction.MoveCategoryRequested -> {
                if (action.blueprintIds.isNotEmpty()) {
                    _state.update { it.copy(categoryMoveBlueprintIds = action.blueprintIds) }
                }
            }
            is HomeAction.MoveCategorySelected -> {
                val ids = _state.value.categoryMoveBlueprintIds
                val category = if (action.categoryId == HomeCategoryId.Uncategorized) {
                    ""
                } else {
                    action.categoryId
                }
                _state.update {
                    it.copy(
                        categoryMoveBlueprintIds = emptySet(),
                        selectedBlueprintIds = emptySet(),
                    )
                }
                viewModelScope.launch {
                    blueprintRepository.moveBlueprintsToCategory(ids.toList(), category)
                }
            }
            HomeAction.MoveCategoryDismissed -> {
                _state.update { it.copy(categoryMoveBlueprintIds = emptySet()) }
            }
            is HomeAction.RenameRequested -> {
                _state.update { it.copy(renameBlueprintId = action.blueprintId) }
            }
            is HomeAction.RenameConfirmed -> {
                _state.update { it.copy(renameBlueprintId = null) }
                viewModelScope.launch {
                    blueprintRepository.renameBlueprint(action.blueprintId, action.fileName)
                }
            }
            HomeAction.RenameDismissed -> {
                _state.update { it.copy(renameBlueprintId = null) }
            }
            is HomeAction.DeleteRequested -> {
                _state.update { it.copy(deleteBlueprintId = action.blueprintId) }
            }
            is HomeAction.DeleteConfirmed -> {
                _state.update {
                    it.copy(
                        deleteBlueprintId = null,
                        selectedBlueprintIds = it.selectedBlueprintIds - action.blueprintId,
                    )
                }
                viewModelScope.launch {
                    blueprintRepository.deleteBlueprint(action.blueprintId)
                }
            }
            HomeAction.DeleteDismissed -> {
                _state.update { it.copy(deleteBlueprintId = null) }
            }
        }
    }

    private fun observeSafDirectory() {
        viewModelScope.launch {
            combine(
                settingsRepository.localBlueprintTreeUri,
                settingsRepository.localBlueprintTreeDocumentId,
            ) { treeUri, treeDocumentId ->
                treeUri to treeDocumentId
            }
                .distinctUntilChanged()
                .collect { (treeUri, treeDocumentId) ->
                    _state.update {
                        it.copy(
                            localBlueprintTreeUri = treeUri,
                            localBlueprintTreeDocumentId = treeDocumentId,
                        )
                    }
                    if (!treeUri.isNullOrBlank()) {
                        blueprintRepository.refreshLocalBlueprints()
                    }
                }
        }
    }

    private fun observeLocalBlueprints() {
        viewModelScope.launch {
            blueprintRepository.observeLocalBlueprints().collect { blueprints ->
                val validIds = blueprints.map { it.id }.toSet()
                _state.update {
                    it.copy(
                        localBlueprints = blueprints.map { blueprint -> blueprint.toHomeBlueprintItem() },
                        selectedBlueprintIds = it.selectedBlueprintIds.intersect(validIds),
                    )
                }
            }
        }
    }

    private fun observeRefreshState() {
        viewModelScope.launch {
            blueprintRepository.isRefreshing.collect { isRefreshing ->
                _state.update { it.copy(isRefreshing = isRefreshing) }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            blueprintRepository.observeCategories().collect { categories ->
                _state.update {
                    it.copy(customCategories = categories)
                }
            }
        }
    }
}
