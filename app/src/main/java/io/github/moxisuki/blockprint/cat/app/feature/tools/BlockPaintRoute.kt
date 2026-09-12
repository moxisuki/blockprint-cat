package io.github.moxisuki.blockprint.cat.app.feature.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.prewarmToolBlockTextures
import io.github.moxisuki.blockprint.cat.app.feature.tools.blockpaint.BlockPaintScreen
import io.github.moxisuki.blockprint.cat.app.feature.tools.blockpaint.BlockPaintViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun BlockPaintRoute(
    modifier: Modifier = Modifier,
    viewModel: BlockPaintViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            prewarmToolBlockTextures(
                context = context,
                resIds = ToolBlockPalette.blocks.map { it.drawableResId },
            )
        }
    }
    BlockPaintScreen(
        state = state,
        onBlockSelected = viewModel::selectBlock,
        onToolSelected = viewModel::selectTool,
        onWidthChanged = { viewModel.resize(width = it) },
        onHeightChanged = { viewModel.resize(height = it) },
        onPaint = viewModel::paint,
        onClear = viewModel::clear,
        modifier = modifier,
    )
}
