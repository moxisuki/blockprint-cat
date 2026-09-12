package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ResourcePacksRoute(
    initialQuery: String? = null,
    modifier: Modifier = Modifier,
    onAppBarTitleVisibleChange: (Boolean) -> Unit = {},
    viewModel: ResourcePacksViewModel = hiltViewModel(),
) {
    var consumedInitialQuery by rememberSaveable(initialQuery) { mutableStateOf(false) }
    LaunchedEffect(initialQuery, consumedInitialQuery) {
        if (!initialQuery.isNullOrBlank() && !consumedInitialQuery) {
            viewModel.onAction(ResourcePacksAction.ModSearchOpen(initialQuery))
            consumedInitialQuery = true
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(onAppBarTitleVisibleChange) {
        onDispose { onAppBarTitleVisibleChange(false) }
    }
    ResourcePacksScreen(
        state = state,
        onAction = viewModel::onAction,
        onAppBarTitleVisibleChange = onAppBarTitleVisibleChange,
        modifier = modifier,
    )
}
