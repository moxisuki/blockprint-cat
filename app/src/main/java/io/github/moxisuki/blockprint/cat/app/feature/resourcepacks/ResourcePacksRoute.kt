package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ResourcePacksRoute(
    modifier: Modifier = Modifier,
    viewModel: ResourcePacksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ResourcePacksScreen(state = state, onAction = viewModel::onAction, modifier = modifier)
}
