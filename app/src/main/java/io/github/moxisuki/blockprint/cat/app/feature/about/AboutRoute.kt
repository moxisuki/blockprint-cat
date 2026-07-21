package io.github.moxisuki.blockprint.cat.app.feature.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AboutRoute(
    modifier: Modifier = Modifier,
    onAppBarTitleVisibleChange: (Boolean) -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(onAppBarTitleVisibleChange) {
        onDispose {
            onAppBarTitleVisibleChange(false)
        }
    }

    AboutScreen(
        state = state,
        onAction = viewModel::onAction,
        onAppBarTitleVisibleChange = onAppBarTitleVisibleChange,
        modifier = modifier,
    )
}
