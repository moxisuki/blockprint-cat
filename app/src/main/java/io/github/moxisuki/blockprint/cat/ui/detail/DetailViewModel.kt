package io.github.moxisuki.blockprint.cat.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.moxisuki.blockprint.cat.data.DispatcherProvider
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.data.blueprint.FullBlueprint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val fullBlueprint: FullBlueprint? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val blueprintManager: BlueprintManager,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun load(uuid: String) {
        viewModelScope.launch(dispatcherProvider.io) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val full = blueprintManager.loadDetail(uuid)
            if (full != null) {
                _uiState.value = DetailUiState(fullBlueprint = full, isLoading = false)
            } else {
                _uiState.value = DetailUiState(
                    fullBlueprint = null,
                    isLoading = false,
                    error = "该蓝图已不存在，可能已被移除",
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            blueprintManager.blueprints.collect { list ->
                val current = _uiState.value.fullBlueprint
                if (current != null) {
                    val stillExists = list.any { it.uuid == current.meta.uuid }
                    if (!stillExists) {
                        _uiState.value = _uiState.value.copy(
                            fullBlueprint = null,
                            error = "该蓝图已被移除",
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
