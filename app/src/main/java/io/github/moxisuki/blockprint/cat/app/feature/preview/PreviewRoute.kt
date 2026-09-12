package io.github.moxisuki.blockprint.cat.app.feature.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PreviewRoute(
    blueprintId: String,
    forceRegenerate: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    LaunchedEffect(blueprintId, forceRegenerate) {
        viewModel.setBlueprintId(blueprintId, forceRegenerate)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PreviewScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}
