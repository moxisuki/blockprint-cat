package io.github.moxisuki.blockprint.cat.app.feature.tools.texttoblueprint

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolPreviewMode
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.MaterialSummaryCard
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.SliderValueRow
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.ToolGridPreview
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.ToolPaletteStrip
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ToolPageBottomPadding = 36.dp

@Composable
internal fun TextToBlueprintScreen(
    state: TextToBlueprintState,
    onTextChanged: (String) -> Unit,
    onBlockSelected: (String) -> Unit,
    onFontSizeChanged: (Int) -> Unit,
    onPaddingChanged: (Int) -> Unit,
    onPreviewModeChanged: (ToolPreviewMode) -> Unit,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appScrollEndHaptic()
            .appMaxContentWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = ToolPageBottomPadding),
    ) {
        item(key = "preview") {
            TextPreviewFrame(
                state = state,
                onPreviewModeChanged = onPreviewModeChanged,
            )
        }
        item(key = "input") {
            TextInputCard(
                state = state,
                onTextChanged = onTextChanged,
                onGenerateClick = onGenerateClick,
            )
        }
        item(key = "style") {
            TextStyleCard(
                state = state,
                onBlockSelected = onBlockSelected,
                onFontSizeChanged = onFontSizeChanged,
                onPaddingChanged = onPaddingChanged,
            )
        }
        item(key = "status") {
            TextGenerationStatus(state = state)
        }
        item(key = "materials") {
            MaterialSummaryCard(grid = state.grid)
        }
    }
}

@Composable
private fun TextPreviewFrame(
    state: TextToBlueprintState,
    onPreviewModeChanged: (ToolPreviewMode) -> Unit,
) {
    val mode = if (state.previewMode == ToolPreviewMode.Source || state.grid == null) {
        ToolPreviewMode.Source
    } else {
        ToolPreviewMode.Result
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        insideMargin = PaddingValues(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 380.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            if (mode == ToolPreviewMode.Source) {
                SourceTextPreview(text = state.text)
            } else {
                ToolGridPreview(
                    grid = state.grid,
                    placeholder = stringResource(R.string.ttb_result_placeholder),
                    framed = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.86f))
                    .padding(start = 10.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = stringResource(if (mode == ToolPreviewMode.Source) R.string.ttb_section_text else R.string.ttb_section_output),
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    state.grid?.let { grid ->
                        Text(
                            text = stringResource(R.string.ttb_size_hint, grid.width, grid.height),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                            maxLines = 1,
                        )
                    }
                }
                IconButton(
                    onClick = {
                        onPreviewModeChanged(
                            if (mode == ToolPreviewMode.Source) ToolPreviewMode.Result else ToolPreviewMode.Source,
                        )
                    },
                    enabled = state.text.isNotBlank() && state.grid != null,
                    minWidth = 34.dp,
                    minHeight = 34.dp,
                ) {
                    Icon(
                        imageVector = if (mode == ToolPreviewMode.Source) Icons.Filled.GridView else Icons.Filled.TextFields,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceTextPreview(text: String) {
    Text(
        text = text.ifBlank { stringResource(R.string.ttb_preview_placeholder) },
        color = if (text.isBlank()) {
            MiuixTheme.colorScheme.onSurfaceVariantSummary
        } else {
            MiuixTheme.colorScheme.onSurface
        },
        style = MiuixTheme.textStyles.title2,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        maxLines = 6,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TextInputCard(
    state: TextToBlueprintState,
    onTextChanged: (String) -> Unit,
    onGenerateClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(16.dp),
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.text,
            onValueChange = onTextChanged,
            label = stringResource(R.string.ttb_input_hint),
            minLines = 3,
            maxLines = 5,
            textStyle = MiuixTheme.textStyles.body1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onGenerateClick,
            enabled = state.text.isNotBlank() && !state.isGenerating,
        ) {
            Text(stringResource(R.string.ttb_generate))
        }
    }
}

@Composable
private fun TextStyleCard(
    state: TextToBlueprintState,
    onBlockSelected: (String) -> Unit,
    onFontSizeChanged: (Int) -> Unit,
    onPaddingChanged: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(16.dp),
    ) {
        Text(
            text = stringResource(R.string.ttb_section_blocks),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        ToolPaletteStrip(
            selectedBlockId = state.selectedBlockId,
            onBlockSelected = onBlockSelected,
        )
        Spacer(modifier = Modifier.height(14.dp))
        SliderValueRow(
            label = stringResource(R.string.ttb_font_size),
            value = state.fontSize,
            range = 12..72,
            onValueChange = onFontSizeChanged,
            keyPoints = listOf(12, 24, 34, 48, 72),
        )
        Spacer(modifier = Modifier.height(10.dp))
        SliderValueRow(
            label = stringResource(R.string.ttb_spacing, state.padding),
            value = state.padding,
            range = 0..12,
            onValueChange = onPaddingChanged,
            keyPoints = listOf(0, 2, 6, 12),
        )
    }
}

@Composable
private fun TextGenerationStatus(state: TextToBlueprintState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.isGenerating) {
            val progress by animateFloatAsState(targetValue = 0.62f, label = "ttbSoftProgress")
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
        }
        state.errorMessage?.let { message ->
            Text(
                text = stringResource(R.string.bp_result_error, message),
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TextToBlueprintScreenPreview() {
    PreviewAppTheme {
        TextToBlueprintScreen(
            state = TextToBlueprintState(text = "BlockPrint"),
            onTextChanged = {},
            onBlockSelected = {},
            onFontSizeChanged = {},
            onPaddingChanged = {},
            onPreviewModeChanged = {},
            onGenerateClick = {},
        )
    }
}
