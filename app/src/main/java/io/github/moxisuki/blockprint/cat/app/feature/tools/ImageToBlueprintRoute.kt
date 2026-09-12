package io.github.moxisuki.blockprint.cat.app.feature.tools

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.prewarmToolBlockTextures
import io.github.moxisuki.blockprint.cat.app.feature.tools.imagetoblueprint.ImageToBlueprintScreen
import io.github.moxisuki.blockprint.cat.app.feature.tools.imagetoblueprint.ImageToBlueprintViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ImageToBlueprintRoute(
    modifier: Modifier = Modifier,
    viewModel: ImageToBlueprintViewModel = hiltViewModel(),
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
    DisposableEffect(Unit) {
        onDispose {
            viewModel.disposeResources()
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.selectImage(uri)
    }

    ImageToBlueprintScreen(
        state = state,
        onPickImage = { imagePicker.launch(arrayOf("image/*")) },
        onTargetWidthChanged = viewModel::setTargetWidth,
        onDitherMethodChanged = viewModel::setDitherMethod,
        onCropHorizontalChanged = viewModel::setCropHorizontal,
        onCropVerticalChanged = viewModel::setCropVertical,
        onExposureChanged = viewModel::setExposure,
        onContrastChanged = viewModel::setContrast,
        onTransparencyEnabledChanged = viewModel::setTransparencyEnabled,
        onAlphaThresholdChanged = viewModel::setAlphaThreshold,
        onPreviewModeChanged = viewModel::setPreviewMode,
        onConvertClick = viewModel::convert,
        modifier = modifier,
    )
}
