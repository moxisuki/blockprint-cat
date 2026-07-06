package io.github.moxisuki.blockprint.cat.ui.tools.texttoblueprint

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import java.io.File
import java.io.FileOutputStream

/** Render text to a pixel-art style bitmap using Canvas + monospace Paint, no anti-alias. */
internal fun renderTextToBitmap(text: String, fontSize: Float, maxWidth: Int = 800): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.textSize = fontSize
        isAntiAlias = false      // pixel-art feel: no blending on edges
        color = android.graphics.Color.BLACK
        typeface = Typeface.MONOSPACE
    }
    val lineHeight = (paint.fontSpacing * 1.2f).toInt()
    val lines = text.split("\n")
    var maxLineWidth = 0f
    val widths = FloatArray(lines.size)
    for (i in lines.indices) {
        widths[i] = paint.measureText(lines[i])
        if (widths[i] > maxLineWidth) maxLineWidth = widths[i]
    }
    val canvasW = (maxLineWidth.coerceAtMost(maxWidth.toFloat()) + 16f).toInt().coerceAtLeast(16)
    val canvasH = (lineHeight * lines.size + 16).coerceAtLeast(16)

    val bitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(android.graphics.Color.WHITE)
    val canvas = Canvas(bitmap)
    var y = paint.textSize - paint.descent()
    for (line in lines) {
        canvas.drawText(line, 8f, y, paint)
        y += lineHeight
    }
    return bitmap
}

/** Save bitmap to cache dir and return a content:// URI via FileProvider. */
internal fun saveTextBitmap(context: android.content.Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "text_render_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextToBlueprintScreen(
    onNavigateToImageToBlueprint: (Uri) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(24f) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.tool_text_to_blueprint_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            placeholder = { Text(stringResource(R.string.ttb_input_hint)) },
            supportingText = { Text(stringResource(R.string.ttb_input_supporting)) },
        )

        // Font size slider
        Text(
            text = "${stringResource(R.string.ttb_font_size)}: ${fontSize.toInt()}sp",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = fontSize,
            onValueChange = { fontSize = it },
            valueRange = 12f..48f,
            steps = 8,
            modifier = Modifier.fillMaxWidth(),
        )

        FilledTonalButton(
            onClick = {
                val bitmap = renderTextToBitmap(text.trim(), fontSize)
                val uri = saveTextBitmap(context, bitmap)
                onNavigateToImageToBlueprint(uri)
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.ttb_generate))
        }
    }
}
