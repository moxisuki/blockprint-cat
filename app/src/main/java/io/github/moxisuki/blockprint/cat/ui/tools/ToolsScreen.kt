package io.github.moxisuki.blockprint.cat.ui.tools

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.R
import kotlinx.coroutines.launch

@Composable
fun ToolsScreen(
    snackbarHostState: SnackbarHostState,
    onNavigateToImageToBlueprint: () -> Unit,
    onNavigateToTextToBlueprint: () -> Unit,
    viewModel: ToolsViewModel = hiltViewModel(),
) {
    val tools by viewModel.tools.collectAsStateWithLifecycle()
    val notImplMsg = stringResource(R.string.tool_not_implemented)
    val scope = rememberCoroutineScope()

    val onToolClick = remember(viewModel, notImplMsg) {
        { entry: ToolEntry ->
            when (viewModel.onToolClick(entry)) {
                ToolClickResult.NavigateToImageToBlueprint -> onNavigateToImageToBlueprint()
                ToolClickResult.NavigateToTextToBlueprint -> onNavigateToTextToBlueprint()
                ToolClickResult.NotImplemented -> scope.launch { snackbarHostState.showSnackbar(notImplMsg) }
            }.let { /* discard Job or Unit */ }
        }
    }

    ToolsContent(tools = tools, onToolClick = onToolClick)
}
