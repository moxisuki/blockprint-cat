package io.github.moxisuki.blockprint.cat.app.feature.tools.texttoblueprint

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolBlockPalette
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolGrid
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolPreviewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
internal class TextToBlueprintViewModel @Inject constructor() : ViewModel() {
    private val mutableState = MutableStateFlow(TextToBlueprintState())
    val state: StateFlow<TextToBlueprintState> = mutableState
    private var generateJob: Job? = null

    fun setText(value: String) {
        mutableState.update { it.copy(text = value.take(180)) }
        generate()
    }

    fun selectBlock(blockId: String) {
        if (ToolBlockPalette.block(blockId) == null) return
        mutableState.update { it.copy(selectedBlockId = blockId) }
        generate()
    }

    fun setFontSize(value: Int) {
        mutableState.update { it.copy(fontSize = value.coerceIn(12, 72)) }
        generate()
    }

    fun setPadding(value: Int) {
        mutableState.update { it.copy(padding = value.coerceIn(0, 12)) }
        generate()
    }

    fun setPreviewMode(mode: ToolPreviewMode) {
        mutableState.update { it.copy(previewMode = mode) }
    }

    fun generate() {
        val snapshot = mutableState.value
        generateJob?.cancel()
        if (snapshot.text.isBlank()) {
            mutableState.update { it.copy(grid = null, isGenerating = false, errorMessage = null) }
            return
        }
        generateJob = viewModelScope.launch {
            mutableState.update { it.copy(isGenerating = true, errorMessage = null) }
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    renderText(
                        text = snapshot.text,
                        fontSize = snapshot.fontSize,
                        padding = snapshot.padding,
                        blockId = snapshot.selectedBlockId,
                    )
                }
            }
            mutableState.update {
                it.copy(
                    grid = result.getOrNull() ?: it.grid,
                    isGenerating = false,
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun renderText(text: String, fontSize: Int, padding: Int, blockId: String): ToolGrid {
        val paint = Paint().apply {
            isAntiAlias = false
            color = Color.BLACK
            textSize = fontSize.toFloat()
            typeface = Typeface.DEFAULT_BOLD
        }
        val lines = text.lines().ifEmpty { listOf(text) }.map { it.ifBlank { " " } }
        val metrics = paint.fontMetrics
        val lineHeight = ceil(metrics.descent - metrics.ascent).toInt().coerceAtLeast(1)
        val maxWidth = lines.maxOf { ceil(paint.measureText(it)).toInt() }.coerceAtLeast(1)
        val bitmapWidth = (maxWidth + padding * 2).coerceIn(1, 256)
        val bitmapHeight = (lineHeight * lines.size + padding * 2).coerceIn(1, 256)
        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        lines.forEachIndexed { index, line ->
            val baseline = padding - metrics.ascent + index * lineHeight
            canvas.drawText(line, padding.toFloat(), baseline, paint)
        }
        val pixels = IntArray(bitmapWidth * bitmapHeight)
        bitmap.getPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight)
        val cells = List(bitmapWidth * bitmapHeight) { index ->
            val alpha = pixels[index] ushr 24 and 0xFF
            if (alpha > 0) blockId else null
        }
        bitmap.recycle()
        return ToolGrid(bitmapWidth, bitmapHeight, cells)
    }
}
