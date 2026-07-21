package io.github.moxisuki.blockprint.cat.app.feature.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BlueprintDetailRoute(
    blueprintId: String,
    modifier: Modifier = Modifier,
    viewModel: BlueprintDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(blueprintId) {
        viewModel.setBlueprintId(blueprintId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    BlueprintDetailScreen(
        state = state,
        modifier = modifier,
    )
}
