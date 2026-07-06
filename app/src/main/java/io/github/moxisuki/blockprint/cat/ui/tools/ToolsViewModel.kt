package io.github.moxisuki.blockprint.cat.ui.tools

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface ToolClickResult {
    data object NotImplemented : ToolClickResult
}

@HiltViewModel
class ToolsViewModel @Inject constructor() : ViewModel() {
    private val _tools = MutableStateFlow(ToolCatalog.entries)
    val tools: StateFlow<List<ToolEntry>> = _tools.asStateFlow()

    fun onToolClick(entry: ToolEntry): ToolClickResult {
        return ToolClickResult.NotImplemented
    }
}
