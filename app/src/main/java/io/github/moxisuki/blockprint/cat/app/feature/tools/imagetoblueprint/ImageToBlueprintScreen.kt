package io.github.moxisuki.blockprint.cat.app.feature.tools.imagetoblueprint

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolDitherMethod
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolPreviewMode
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.MaterialSummaryCard
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.SliderValueRow
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ToolPageBottomPadding = 40.dp

private val DitherMethods = listOf(
    ToolDitherMethod.None,
    ToolDitherMethod.FloydSteinberg,
    ToolDitherMethod.Bayer2x2,
    ToolDitherMethod.Bayer4x4,
)

@Composable
internal fun ImageToBlueprintScreen(
    state: ImageToBlueprintState,
    onPickImage: () -> Unit,
    onTargetWidthChanged: (Int) -> Unit,
    onDitherMethodChanged: (ToolDitherMethod) -> Unit,
    onCropHorizontalChanged: (Int) -> Unit,
    onCropVerticalChanged: (Int) -> Unit,
    onExposureChanged: (Int) -> Unit,
    onContrastChanged: (Int) -> Unit,
    onTransparencyEnabledChanged: (Boolean) -> Unit,
    onAlphaThresholdChanged: (Int) -> Unit,
    onPreviewModeChanged: (ToolPreviewMode) -> Unit,
    onConvertClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appScrollEndHaptic()
            .appMaxContentWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp, bottom = ToolPageBottomPadding),
    ) {
        item(key = "preview") {
            ImagePreviewFrame(
                state = state,
                onPickImage = onPickImage,
                onPreviewModeChanged = onPreviewModeChanged,
            )
        }
        item(key = "controls") {
            ConversionControlsCard(
                state = state,
                onTargetWidthChanged = onTargetWidthChanged,
                onDitherMethodChanged = onDitherMethodChanged,
                onCropHorizontalChanged = onCropHorizontalChanged,
                onCropVerticalChanged = onCropVerticalChanged,
                onExposureChanged = onExposureChanged,
                onContrastChanged = onContrastChanged,
                onTransparencyEnabledChanged = onTransparencyEnabledChanged,
                onAlphaThresholdChanged = onAlphaThresholdChanged,
                onConvertClick = onConvertClick,
            )
        }
        item(key = "status") {
            ConversionStatus(state = state)
        }
        item(key = "materials") {
            MaterialSummaryCard(grid = state.grid)
        }
    }
}

@Composable
private fun ImagePreviewFrame(
    state: ImageToBlueprintState,
    onPickImage: () -> Unit,
    onPreviewModeChanged: (ToolPreviewMode) -> Unit,
) {
    val mode = if (state.previewMode == ToolPreviewMode.Source || state.grid == null) {
        ToolPreviewMode.Source
    } else {
        ToolPreviewMode.Result
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .clip(RoundedCornerShape(14.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)),
        ) {
            if (mode == ToolPreviewMode.Source) {
                val bitmap = state.previewBitmap
                if (bitmap == null) {
                    Button(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp),
                        onClick = onPickImage,
                    ) {
                        Text(stringResource(R.string.itb_select_image))
                    }
                } else {
                    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = stringResource(R.string.itb_select_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            } else {
                val bitmap = state.resultPreviewBitmap
                if (bitmap == null) {
                    Text(
                        text = stringResource(R.string.itb_no_result),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = stringResource(R.string.itb_section_result),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        PreviewStatusBar(
            mode = mode,
            sizeLabel = previewSizeLabel(state, mode) ?: stringResource(R.string.itb_select_image_hint),
            canToggle = state.previewBitmap != null && state.grid != null,
            onToggleClick = {
                onPreviewModeChanged(
                    if (mode == ToolPreviewMode.Source) ToolPreviewMode.Result else ToolPreviewMode.Source,
                )
            },
            onPickImage = onPickImage,
        )
    }
}

@Composable
private fun PreviewStatusBar(
    mode: ToolPreviewMode,
    sizeLabel: String,
    canToggle: Boolean,
    onToggleClick: () -> Unit,
    onPickImage: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.66f))
            .padding(start = 12.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(if (mode == ToolPreviewMode.Source) R.string.itb_select_image else R.string.itb_section_result),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = sizeLabel,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = onToggleClick,
            enabled = canToggle,
            minWidth = 36.dp,
            minHeight = 36.dp,
        ) {
            Icon(
                imageVector = if (mode == ToolPreviewMode.Source) Icons.Filled.GridView else Icons.Filled.Image,
                contentDescription = null,
                modifier = Modifier.size(19.dp),
            )
        }
        TextButton(onClick = onPickImage, text = stringResource(R.string.itb_reselect))
    }
}

@Composable
private fun previewSizeLabel(state: ImageToBlueprintState, mode: ToolPreviewMode): String? =
    when (mode) {
        ToolPreviewMode.Source -> state.previewBitmap?.let {
            stringResource(R.string.itb_image_size, state.sourceWidth, state.sourceHeight)
        }
        ToolPreviewMode.Result -> state.grid?.let { stringResource(R.string.itb_result_size, it.width, it.height) }
    }

@Composable
private fun ConversionControlsCard(
    state: ImageToBlueprintState,
    onTargetWidthChanged: (Int) -> Unit,
    onDitherMethodChanged: (ToolDitherMethod) -> Unit,
    onCropHorizontalChanged: (Int) -> Unit,
    onCropVerticalChanged: (Int) -> Unit,
    onExposureChanged: (Int) -> Unit,
    onContrastChanged: (Int) -> Unit,
    onTransparencyEnabledChanged: (Boolean) -> Unit,
    onAlphaThresholdChanged: (Int) -> Unit,
    onConvertClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.itb_section_output),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.itb_result_size, state.targetWidth, state.estimatedTargetHeight()),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        SliderValueRow(
            label = stringResource(R.string.itb_width),
            value = state.targetWidth,
            range = 16..192,
            onValueChange = onTargetWidthChanged,
            keyPoints = listOf(32, 64, 96, 128, 192),
        )
        Spacer(modifier = Modifier.height(10.dp))
        OverlaySpinnerPreference(
            items = DitherMethods.map { DropdownItem(text = stringResource(it.labelRes())) },
            selectedIndex = DitherMethods.indexOf(state.ditherMethod).coerceAtLeast(0),
            title = stringResource(R.string.itb_section_dither),
            onSelectedIndexChange = { index -> onDitherMethodChanged(DitherMethods[index]) },
        )
        Spacer(modifier = Modifier.height(10.dp))
        SliderValueRow(
            label = stringResource(R.string.itb_crop_horizontal),
            value = state.cropHorizontal,
            range = 0..45,
            onValueChange = onCropHorizontalChanged,
            keyPoints = listOf(0, 10, 25, 45),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SliderValueRow(
            label = stringResource(R.string.itb_crop_vertical),
            value = state.cropVertical,
            range = 0..45,
            onValueChange = onCropVerticalChanged,
            keyPoints = listOf(0, 10, 25, 45),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SliderValueRow(
            label = stringResource(R.string.itb_exposure),
            value = state.exposure,
            range = -100..100,
            onValueChange = onExposureChanged,
            keyPoints = listOf(-100, -50, 0, 50, 100),
        )
        Spacer(modifier = Modifier.height(8.dp))
        SliderValueRow(
            label = stringResource(R.string.itb_contrast),
            value = state.contrast,
            range = -100..100,
            onValueChange = onContrastChanged,
            keyPoints = listOf(-100, -50, 0, 50, 100),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.itb_transparency_enable),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.itb_transparency_tolerance, state.alphaThreshold),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            Switch(
                checked = state.transparencyEnabled,
                onCheckedChange = onTransparencyEnabledChanged,
            )
        }
        AnimatedVisibility(visible = state.transparencyEnabled) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                SliderValueRow(
                    label = stringResource(R.string.itb_section_transparency),
                    value = state.alphaThreshold,
                    range = 0..255,
                    onValueChange = onAlphaThresholdChanged,
                    keyPoints = listOf(0, 24, 64, 128, 255),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onConvertClick,
            enabled = state.previewBitmap != null && !state.isConverting,
        ) {
            Text(stringResource(if (state.grid == null) R.string.itb_convert else R.string.bp_action_regenerate))
        }
    }
}

@Composable
private fun ConversionStatus(state: ImageToBlueprintState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.isConverting) {
            val progress by animateFloatAsState(targetValue = 0.72f, label = "itbSoftProgress")
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
        }
        state.errorMessage?.let { message ->
            Text(
                text = stringResource(R.string.itb_result_error, message),
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

private fun ToolDitherMethod.labelRes(): Int =
    when (this) {
        ToolDitherMethod.None -> R.string.itb_dither_none
        ToolDitherMethod.FloydSteinberg -> R.string.itb_dither_floyd_steinberg
        ToolDitherMethod.Bayer2x2 -> R.string.itb_dither_bayer_2x2
        ToolDitherMethod.Bayer4x4 -> R.string.itb_dither_bayer_4x4
    }

private fun ImageToBlueprintState.estimatedTargetHeight(): Int =
    if (sourceWidth > 0 && sourceHeight > 0) {
        (sourceHeight * (targetWidth.toFloat() / sourceWidth.toFloat())).toInt().coerceIn(1, 192)
    } else {
        targetWidth
    }

@Preview(showBackground = true)
@Composable
private fun ImageToBlueprintScreenPreview() {
    PreviewAppTheme {
        ImageToBlueprintScreen(
            state = ImageToBlueprintState(),
            onPickImage = {},
            onTargetWidthChanged = {},
            onDitherMethodChanged = {},
            onCropHorizontalChanged = {},
            onCropVerticalChanged = {},
            onExposureChanged = {},
            onContrastChanged = {},
            onTransparencyEnabledChanged = {},
            onAlphaThresholdChanged = {},
            onPreviewModeChanged = {},
            onConvertClick = {},
        )
    }
}
