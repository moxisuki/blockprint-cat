package io.github.moxisuki.blockprint.cat.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta
import io.github.moxisuki.blockprint.cat.data.category.CategoryManager
import io.github.moxisuki.blockprint.cat.data.category.CategoryRow
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Virtual ID for the "All" pseudo-category. */
val CATEGORY_ID_ALL: String? = null

/** UI sentinel for the "未分类" pseudo-category. Not stored in DB. */
const val UNCATEGORIZED_ID: String = "__uncategorized__"

sealed interface MultiSelectState {
    data object Off : MultiSelectState
    data class On(val selected: Set<String>) : MultiSelectState {
        fun toggle(uuid: String): On =
            if (selected.contains(uuid)) On(selected - uuid) else On(selected + uuid)
        fun selectAll(all: List<BlueprintMeta>): On = On(all.map { it.uuid }.toSet())
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val blueprintManager: BlueprintManager,
    private val categoryManager: CategoryManager,
) : ViewModel() {

    val blueprints: StateFlow<List<BlueprintMeta>> = blueprintManager.blueprints
    val blueprintCount: StateFlow<Int> = blueprintManager.blueprintCount
    val scanning: StateFlow<Boolean> = blueprintManager.scanning
    val categories: StateFlow<List<CategoryRow>> = categoryManager.categories
    val counts: StateFlow<Map<String?, Int>> = categoryManager.counts

    private val _selectedCategoryId = MutableStateFlow<String?>(CATEGORY_ID_ALL)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _multi = MutableStateFlow<MultiSelectState>(MultiSelectState.Off)
    val multi: StateFlow<MultiSelectState> = _multi.asStateFlow()

    val displayedBlueprints: StateFlow<List<BlueprintMeta>> =
        combine(blueprints, _selectedCategoryId) { list, id ->
            when (id) {
                null -> list  // ALL
                UNCATEGORIZED_ID -> list.filter { it.categoryId == null }
                else -> list.filter { it.categoryId == id }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun selectCategory(id: String?) { _selectedCategoryId.value = id; exitMulti() }
    fun clearFilter() { _selectedCategoryId.value = null; exitMulti() }

    fun enterMultiSelect(uuid: String) {
        _multi.value = MultiSelectState.On(setOf(uuid))
    }
    fun toggleSelected(uuid: String) {
        _multi.update {
            when (it) {
                MultiSelectState.Off -> MultiSelectState.On(setOf(uuid))
                is MultiSelectState.On -> it.toggle(uuid)
            }
        }
    }
    fun selectAll() {
        _multi.update {
            when (it) {
                MultiSelectState.Off -> MultiSelectState.On(emptySet())
                is MultiSelectState.On -> it.selectAll(displayedBlueprints.value)
            }
        }
    }
    fun exitMulti() { _multi.value = MultiSelectState.Off }

    suspend fun createCategory(name: String, color: Int, pattern: Int) =
        categoryManager.create(name, color, pattern)
    suspend fun renameCategory(id: String, name: String) = categoryManager.rename(id, name)
    suspend fun changeCover(id: String, color: Int, pattern: Int) =
        categoryManager.changeCover(id, color, pattern)
    suspend fun deleteCategory(id: String) = categoryManager.delete(id)
    suspend fun moveSelectedTo(targetId: String?) {
        val uuids = (_multi.value as? MultiSelectState.On)?.selected.orEmpty()
        if (uuids.isNotEmpty()) categoryManager.moveBlueprintsToCategory(uuids.toList(), targetId)
        exitMulti()
    }

    fun safFolderName(): String? {
        val state = blueprintManager.safState.value
        return (state as? io.github.moxisuki.blockprint.cat.data.saf.SafState.Ready)?.displayName
    }
}