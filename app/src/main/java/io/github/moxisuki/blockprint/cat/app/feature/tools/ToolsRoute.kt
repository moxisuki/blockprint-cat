package io.github.moxisuki.blockprint.cat.app.feature.tools

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun ToolsRoute(
    onToolClick: (ToolDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToolsScreen(
        entries = ToolEntries,
        onToolClick = onToolClick,
        modifier = modifier,
    )
}
