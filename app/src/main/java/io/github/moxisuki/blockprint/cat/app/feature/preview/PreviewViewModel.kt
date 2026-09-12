package io.github.moxisuki.blockprint.cat.app.feature.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.core.preview.PreviewRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val repository: PreviewRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(PreviewState())
    val state: StateFlow<PreviewState> = _state.asStateFlow()
    private var loadJob: Job? = null
    private var forceRegenerate = false

    fun setBlueprintId(blueprintId: String, forceRegenerate: Boolean = false) {
        if (blueprintId.isBlank()) return
        if (_state.value.blueprintId == blueprintId && this.forceRegenerate == forceRegenerate) return
        this.forceRegenerate = forceRegenerate
        _state.value = PreviewState(blueprintId = blueprintId)
        load()
    }

    fun retry() {
        if (_state.value.blueprintId.isNotBlank()) load()
    }

    private fun load() {
        val blueprintId = _state.value.blueprintId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    model = null,
                    stage = io.github.moxisuki.blockprint.cat.app.core.preview.PreviewStage.Preparing,
                    progress = 0f,
                    isLoading = true,
                    errorMessage = null,
                )
            }
            try {
                val model = repository.prepare(blueprintId, forceRegenerate) { progress ->
                    _state.update {
                        it.copy(stage = progress.stage, progress = progress.fraction)
                    }
                }
                _state.update {
                    it.copy(
                        title = model.title,
                        model = model,
                        stage = io.github.moxisuki.blockprint.cat.app.core.preview.PreviewStage.LoadingModel,
                        progress = 1f,
                        isLoading = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: error.javaClass.simpleName)
                }
            }
        }
    }
}
