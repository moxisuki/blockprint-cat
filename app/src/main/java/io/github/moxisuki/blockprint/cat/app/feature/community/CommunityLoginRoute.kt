package io.github.moxisuki.blockprint.cat.app.feature.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.app.feature.community.components.McsLoginWebViewScreen

@Composable
internal fun CommunityLoginRoute(
    onLoginSuccess: () -> Unit,
    viewModel: CommunityLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    McsLoginWebViewScreen(
        state = state,
        onCookiesCaptured = { cookies ->
            viewModel.onCookiesCaptured(
                cookies = cookies,
                onSuccess = onLoginSuccess,
            )
        },
    )
}
