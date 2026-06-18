package io.github.moxisuki.blockprint.cat.ui.management

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlueprintUiState(
    val blueprints: List<BlueprintMeta> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class ManagementEvent {
    data class Delete(val context: Context, val uuid: String)
}

@HiltViewModel
class BlueprintViewModel @Inject constructor(
    private val blueprintManager: BlueprintManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlueprintUiState())
    val uiState: StateFlow<BlueprintUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            blueprintManager.blueprints.collect { list ->
                _uiState.value = _uiState.value.copy(blueprints = list)
            }
        }
    }

    fun onEvent(event: ManagementEvent) {
        when (event) {
            is ManagementEvent.Delete -> viewModelScope.launch { blueprintManager.delete(event.uuid) }
        }
    }

    fun delete(context: Context, uuid: String) {
        viewModelScope.launch { blueprintManager.delete(uuid) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun loadWithContext(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val name = uri.lastPathSegment?.substringAfterLast('/') ?: "untitled.litematic"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Cannot read")
                blueprintManager.ingest(name, bytes)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败",
                )
            }
        }
    }

    fun rename(context: Context, uuid: String, newFileName: String) {
        viewModelScope.launch {
            runCatching { blueprintManager.rename(uuid, newFileName) }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message ?: "重命名失败")
                }
        }
    }

    suspend fun readBytes(uuid: String): ByteArray? = blueprintManager.readBytes(uuid)
}
