package io.github.moxisuki.blockprint.cat.app.feature.tools

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.prewarmToolBlockTextures
import io.github.moxisuki.blockprint.cat.app.feature.tools.texttoblueprint.TextToBlueprintScreen
import io.github.moxisuki.blockprint.cat.app.feature.tools.texttoblueprint.TextToBlueprintViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun TextToBlueprintRoute(
    modifier: Modifier = Modifier,
    viewModel: TextToBlueprintViewModel = hiltViewModel(),
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
    TextToBlueprintScreen(
        state = state,
        onTextChanged = viewModel::setText,
        onBlockSelected = viewModel::selectBlock,
        onFontSizeChanged = viewModel::setFontSize,
        onPaddingChanged = viewModel::setPadding,
        onPreviewModeChanged = viewModel::setPreviewMode,
        onGenerateClick = viewModel::generate,
        modifier = modifier,
    )
}
