package io.github.moxisuki.blockprint.cat.app.feature.tools.imagetoblueprint

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolDitherMethod
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolGrid
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolPreviewMode

@Immutable
internal data class ImageToBlueprintState(
    val imageUri: String? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val targetWidth: Int = 96,
    val ditherMethod: ToolDitherMethod = ToolDitherMethod.None,
    val cropHorizontal: Int = 0,
    val cropVertical: Int = 0,
    val exposure: Int = 0,
    val contrast: Int = 0,
    val transparencyEnabled: Boolean = true,
    val alphaThreshold: Int = 24,
    val previewMode: ToolPreviewMode = ToolPreviewMode.Result,
    val previewBitmap: Bitmap? = null,
    val resultPreviewBitmap: Bitmap? = null,
    val grid: ToolGrid? = null,
    val isConverting: Boolean = false,
    val errorMessage: String? = null,
)
