package io.github.moxisuki.blockprint.cat.app.feature.tools.imagetoblueprint

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolBlockPalette
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolDitherMethod
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolGrid
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolPreviewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.coroutines.coroutineContext

@HiltViewModel
internal class ImageToBlueprintViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ImageToBlueprintState())
    val state: StateFlow<ImageToBlueprintState> = mutableState
    private var convertJob: Job? = null

    fun selectImage(uri: Uri) {
        val oldBitmap = mutableState.value.previewBitmap
        val oldResultPreviewBitmap = mutableState.value.resultPreviewBitmap
        val oldConvertJob = convertJob
        oldConvertJob?.cancel()
        viewModelScope.launch {
            mutableState.update { it.copy(isConverting = true, errorMessage = null, imageUri = uri.toString()) }
            val result = runCatching {
                withContext(Dispatchers.IO) { decodeBitmap(uri) }
            }
            val bitmap = result.getOrNull()
            if (bitmap == null) {
                mutableState.update {
                    it.copy(
                        isConverting = false,
                        errorMessage = result.exceptionOrNull()?.message.orEmpty().ifBlank { "Cannot decode image" },
                    )
                }
                return@launch
            }
            mutableState.update {
                it.copy(
                    sourceWidth = bitmap.width,
                    sourceHeight = bitmap.height,
                    previewBitmap = bitmap,
                    resultPreviewBitmap = null,
                    grid = null,
                    previewMode = ToolPreviewMode.Source,
                    isConverting = false,
                    errorMessage = null,
                )
            }
            releaseBitmapWhenIdle(oldBitmap, oldConvertJob)
            if (oldResultPreviewBitmap != null && !oldResultPreviewBitmap.isRecycled) {
                oldResultPreviewBitmap.recycle()
            }
            convert()
        }
    }

    fun setTargetWidth(width: Int) {
        mutableState.update { it.copy(targetWidth = width.coerceIn(16, 192)) }
    }

    fun setDitherMethod(method: ToolDitherMethod) {
        mutableState.update { it.copy(ditherMethod = method) }
    }

    fun setCropHorizontal(value: Int) {
        mutableState.update { it.copy(cropHorizontal = value.coerceIn(0, 45)) }
    }

    fun setCropVertical(value: Int) {
        mutableState.update { it.copy(cropVertical = value.coerceIn(0, 45)) }
    }

    fun setExposure(value: Int) {
        mutableState.update { it.copy(exposure = value.coerceIn(-100, 100)) }
    }

    fun setContrast(value: Int) {
        mutableState.update { it.copy(contrast = value.coerceIn(-100, 100)) }
    }

    fun setTransparencyEnabled(enabled: Boolean) {
        mutableState.update { it.copy(transparencyEnabled = enabled) }
    }

    fun setAlphaThreshold(value: Int) {
        mutableState.update { it.copy(alphaThreshold = value.coerceIn(0, 255)) }
    }

    fun setPreviewMode(mode: ToolPreviewMode) {
        mutableState.update { it.copy(previewMode = mode) }
    }

    fun convert() {
        val bitmap = mutableState.value.previewBitmap ?: return
        convertJob?.cancel()
        convertJob = viewModelScope.launch {
            val snapshot = mutableState.value
            mutableState.update { it.copy(isConverting = true, errorMessage = null) }
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    convertBitmap(
                        bitmap = bitmap,
                        targetWidth = snapshot.targetWidth,
                        ditherMethod = snapshot.ditherMethod,
                        cropHorizontal = snapshot.cropHorizontal,
                        cropVertical = snapshot.cropVertical,
                        exposure = snapshot.exposure,
                        contrast = snapshot.contrast,
                        transparencyEnabled = snapshot.transparencyEnabled,
                        alphaThreshold = snapshot.alphaThreshold,
                    )
                }
            }
            mutableState.update {
                val oldResultPreview = it.resultPreviewBitmap
                val nextResultPreview = result.getOrNull()?.previewBitmap
                if (nextResultPreview != null && oldResultPreview != null && oldResultPreview !== nextResultPreview && !oldResultPreview.isRecycled) {
                    oldResultPreview.recycle()
                }
                it.copy(
                    grid = result.getOrNull()?.grid ?: it.grid,
                    resultPreviewBitmap = nextResultPreview ?: it.resultPreviewBitmap,
                    previewMode = if (result.isSuccess) ToolPreviewMode.Result else it.previewMode,
                    isConverting = false,
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    fun disposeResources() {
        val bitmap = mutableState.value.previewBitmap
        val resultPreviewBitmap = mutableState.value.resultPreviewBitmap
        val job = convertJob
        job?.cancel()
        releaseBitmapWhenIdle(bitmap, job)
        if (resultPreviewBitmap != null && !resultPreviewBitmap.isRecycled) {
            resultPreviewBitmap.recycle()
        }
        convertJob = null
        mutableState.value = ImageToBlueprintState()
    }

    override fun onCleared() {
        disposeResources()
        super.onCleared()
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MaxDecodedImageSize)
        }
        context.contentResolver.openInputStream(uri).use { stream ->
            return BitmapFactory.decodeStream(stream, null, options)
                ?: error("Cannot decode image")
        }
    }

    private suspend fun convertBitmap(
        bitmap: Bitmap,
        targetWidth: Int,
        ditherMethod: ToolDitherMethod,
        cropHorizontal: Int,
        cropVertical: Int,
        exposure: Int,
        contrast: Int,
        transparencyEnabled: Boolean,
        alphaThreshold: Int,
    ): ImageConversionResult {
        val cropRect = bitmap.cropRect(cropHorizontal, cropVertical)
        val width = targetWidth.coerceIn(16, 192)
        val height = (cropRect.height() * (width.toFloat() / cropRect.width().toFloat()))
            .roundToInt()
            .coerceIn(1, 192)
        val cells = MutableList<String?>(width * height) { null }
        val croppedBitmap = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, width, height, true)
        val pixelCount = width * height
        val sourcePixels = IntArray(pixelCount)
        val samples = FloatArray(pixelCount * 4)
        scaledBitmap.getPixels(sourcePixels, 0, width, 0, 0, width, height)
        if (scaledBitmap !== croppedBitmap && croppedBitmap !== bitmap && !croppedBitmap.isRecycled) {
            croppedBitmap.recycle()
        }
        val exposureOffset = exposure / 100f
        val contrastFactor = 1f + contrast / 100f
        for (y in 0 until height) {
            coroutineContext.ensureActive()
            val rowOffset = y * width
            for (x in 0 until width) {
                val argb = sourcePixels[rowOffset + x]
                val base = (rowOffset + x) * 4
                samples[base] = (((argb shr 16 and 0xFF) / 255f - 0.5f) * contrastFactor + 0.5f + exposureOffset).coerceIn(0f, 1f)
                samples[base + 1] = (((argb shr 8 and 0xFF) / 255f - 0.5f) * contrastFactor + 0.5f + exposureOffset).coerceIn(0f, 1f)
                samples[base + 2] = (((argb and 0xFF) / 255f - 0.5f) * contrastFactor + 0.5f + exposureOffset).coerceIn(0f, 1f)
                samples[base + 3] = (argb ushr 24 and 0xFF) / 255f
            }
        }
        if (scaledBitmap !== bitmap && !scaledBitmap.isRecycled) {
            scaledBitmap.recycle()
        }

        when (ditherMethod) {
            ToolDitherMethod.None -> fillNearest(cells, samples, transparencyEnabled, alphaThreshold)
            ToolDitherMethod.FloydSteinberg -> fillFloydSteinberg(
                cells = cells,
                samples = samples,
                width = width,
                height = height,
                transparencyEnabled = transparencyEnabled,
                alphaThreshold = alphaThreshold,
            )
            ToolDitherMethod.Bayer2x2 -> fillBayer(
                cells = cells,
                samples = samples,
                width = width,
                transparencyEnabled = transparencyEnabled,
                alphaThreshold = alphaThreshold,
                matrix = Bayer2x2,
                matrixSize = 2,
            )
            ToolDitherMethod.Bayer4x4 -> fillBayer(
                cells = cells,
                samples = samples,
                width = width,
                transparencyEnabled = transparencyEnabled,
                alphaThreshold = alphaThreshold,
                matrix = Bayer4x4,
                matrixSize = 4,
            )
        }
        val grid = ToolGrid(width, height, cells)
        return ImageConversionResult(grid = grid, previewBitmap = grid.toColorPreviewBitmap())
    }

    private fun Bitmap.cropRect(cropHorizontal: Int, cropVertical: Int): android.graphics.Rect {
        val horizontalInset = (width * (cropHorizontal / 100f) / 2f).roundToInt().coerceAtMost((width - 1) / 2)
        val verticalInset = (height * (cropVertical / 100f) / 2f).roundToInt().coerceAtMost((height - 1) / 2)
        return android.graphics.Rect(
            horizontalInset,
            verticalInset,
            width - horizontalInset,
            height - verticalInset,
        )
    }

    private fun ToolGrid.toColorPreviewBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height) { index ->
            ToolBlockPalette.block(cells[index])?.color?.toArgb() ?: android.graphics.Color.TRANSPARENT
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun releaseBitmapWhenIdle(bitmap: Bitmap?, job: Job?) {
        if (bitmap == null || bitmap.isRecycled) return
        if (job == null || job.isCompleted) {
            bitmap.recycle()
        } else {
            job.invokeOnCompletion {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sampleSize = 1
        var sampledWidth = width
        var sampledHeight = height
        while (sampledWidth / 2 >= maxSize || sampledHeight / 2 >= maxSize) {
            sampleSize *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }
        return sampleSize
    }

    private fun fillNearest(
        cells: MutableList<String?>,
        samples: FloatArray,
        transparencyEnabled: Boolean,
        alphaThreshold: Int,
    ) {
        for (index in cells.indices) {
            val base = index * 4
            cells[index] = if (transparencyEnabled && samples[base + 3] * 255f <= alphaThreshold) {
                null
            } else {
                ToolBlockPalette.blocks[ToolBlockPalette.nearestIndex(samples[base], samples[base + 1], samples[base + 2])].id
            }
        }
    }

    private suspend fun fillFloydSteinberg(
        cells: MutableList<String?>,
        samples: FloatArray,
        width: Int,
        height: Int,
        transparencyEnabled: Boolean,
        alphaThreshold: Int,
    ) {
        for (y in 0 until height) {
            coroutineContext.ensureActive()
            for (x in 0 until width) {
                val base = (y * width + x) * 4
                if (transparencyEnabled && samples[base + 3] * 255f <= alphaThreshold) {
                    cells[y * width + x] = null
                    continue
                }
                val block = ToolBlockPalette.blocks[
                    ToolBlockPalette.nearestIndex(samples[base], samples[base + 1], samples[base + 2]),
                ]
                cells[y * width + x] = block.id
                val errorR = samples[base] - block.color.red
                val errorG = samples[base + 1] - block.color.green
                val errorB = samples[base + 2] - block.color.blue
                diffuse(samples, width, height, x + 1, y, errorR, errorG, errorB, 7f / 16f)
                diffuse(samples, width, height, x - 1, y + 1, errorR, errorG, errorB, 3f / 16f)
                diffuse(samples, width, height, x, y + 1, errorR, errorG, errorB, 5f / 16f)
                diffuse(samples, width, height, x + 1, y + 1, errorR, errorG, errorB, 1f / 16f)
            }
        }
    }

    private fun fillBayer(
        cells: MutableList<String?>,
        samples: FloatArray,
        width: Int,
        transparencyEnabled: Boolean,
        alphaThreshold: Int,
        matrix: IntArray,
        matrixSize: Int,
    ) {
        val matrixArea = (matrixSize * matrixSize).toFloat()
        val height = cells.size / width
        for (y in 0 until height) {
            val matrixRow = (y % matrixSize) * matrixSize
            val rowOffset = y * width
            for (x in 0 until width) {
                val index = rowOffset + x
                val base = index * 4
                if (transparencyEnabled && samples[base + 3] * 255f <= alphaThreshold) {
                    cells[index] = null
                } else {
                    val threshold = (matrix[matrixRow + (x % matrixSize)] + 0.5f) / matrixArea - 0.5f
                    val offset = threshold * 0.16f
                    cells[index] = ToolBlockPalette.blocks[
                        ToolBlockPalette.nearestIndex(
                            (samples[base] + offset).coerceIn(0f, 1f),
                            (samples[base + 1] + offset).coerceIn(0f, 1f),
                            (samples[base + 2] + offset).coerceIn(0f, 1f),
                        ),
                    ].id
                }
            }
        }
    }

    private fun diffuse(
        samples: FloatArray,
        width: Int,
        height: Int,
        x: Int,
        y: Int,
        errorR: Float,
        errorG: Float,
        errorB: Float,
        factor: Float,
    ) {
        if (x !in 0 until width || y !in 0 until height) return
        val base = (y * width + x) * 4
        samples[base] = (samples[base] + errorR * factor).coerceIn(0f, 1f)
        samples[base + 1] = (samples[base + 1] + errorG * factor).coerceIn(0f, 1f)
        samples[base + 2] = (samples[base + 2] + errorB * factor).coerceIn(0f, 1f)
    }

    private companion object {
        val Bayer2x2 = intArrayOf(
            0, 2,
            3, 1,
        )
        val Bayer4x4 = intArrayOf(
            0, 8, 2, 10,
            12, 4, 14, 6,
            3, 11, 1, 9,
            15, 7, 13, 5,
        )
        const val MaxDecodedImageSize = 1600
    }

    private data class ImageConversionResult(
        val grid: ToolGrid,
        val previewBitmap: Bitmap,
    )
}
