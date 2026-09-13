package io.github.moxisuki.blockprint.cat.app.feature.community

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

@Composable
internal fun CommunityRoute(
    onBlueprintClick: (CommunityBlueprintUiItem) -> Unit,
    viewModel: CommunityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CommunityScreen(
        state = state,
        onAction = viewModel::onAction,
        onBlueprintClick = onBlueprintClick,
    )
}
