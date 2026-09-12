package io.github.moxisuki.blockprint.cat.app.feature.tools.blockpaint

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.feature.tools.BlockPaintTool
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.SliderValueRow
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.ToolGridPreview
import io.github.moxisuki.blockprint.cat.app.feature.tools.components.ToolPaletteStrip
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BlockPaintScreen(
    state: BlockPaintState,
    onBlockSelected: (String) -> Unit,
    onToolSelected: (BlockPaintTool) -> Unit,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onPaint: (x: Int, y: Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCanvasSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
    ) {
        BlockPaintToolbar(
            activeTool = state.activeTool,
            paintedCount = state.grid.paintedCount,
            canvasLabel = "${state.grid.width} × ${state.grid.height}",
            onToolSelected = onToolSelected,
            onClear = onClear,
            onSettingsClick = { showCanvasSettings = true },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            cornerRadius = 16.dp,
            insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            ToolPaletteStrip(
                selectedBlockId = state.selectedBlockId,
                onBlockSelected = onBlockSelected,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.32f))
                .border(
                    width = 1.dp,
                    color = MiuixTheme.colorScheme.outline.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(18.dp),
                ),
        ) {
            ToolGridPreview(
                grid = state.grid,
                placeholder = stringResource(R.string.bp_result_empty),
                editable = true,
                framed = false,
                onCellChange = onPaint,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            )
        }
    }

    CanvasSettingsSheet(
        show = showCanvasSettings,
        width = state.grid.width,
        height = state.grid.height,
        onWidthChanged = onWidthChanged,
        onHeightChanged = onHeightChanged,
        onDismissRequest = { showCanvasSettings = false },
    )
}

@Composable
private fun BlockPaintToolbar(
    activeTool: BlockPaintTool,
    paintedCount: Int,
    canvasLabel: String,
    onToolSelected: (BlockPaintTool) -> Unit,
    onClear: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.bp_painted_count, paintedCount),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = canvasLabel,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
        ToolIconButton(
            icon = Icons.Filled.Edit,
            contentDescription = stringResource(R.string.bp_tool_paint),
            selected = activeTool == BlockPaintTool.Paint,
            onClick = { onToolSelected(BlockPaintTool.Paint) },
        )
        ToolIconButton(
            icon = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = stringResource(R.string.bp_tool_erase),
            selected = activeTool == BlockPaintTool.Erase,
            onClick = { onToolSelected(BlockPaintTool.Erase) },
        )
        ToolIconButton(
            icon = Icons.Filled.DeleteSweep,
            contentDescription = stringResource(R.string.bp_tool_clear),
            selected = false,
            onClick = onClear,
        )
        ToolIconButton(
            icon = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.bp_canvas_size),
            selected = false,
            onClick = onSettingsClick,
        )
    }
}

@Composable
private fun ToolIconButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f)
    }
    val tint = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = if (selected) MiuixTheme.colorScheme.primary.copy(alpha = 0.32f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CanvasSettingsSheet(
    show: Boolean,
    width: Int,
    height: Int,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.bp_canvas_size),
        onDismissRequest = onDismissRequest,
        insideMargin = androidx.compose.ui.unit.DpSize(width = 16.dp, height = 18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.bp_canvas_size),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "$width × $height",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                )
            }
            SliderValueRow(
                label = stringResource(R.string.bp_width),
                value = width,
                range = 4..96,
                onValueChange = onWidthChanged,
                keyPoints = listOf(16, 32, 64, 96),
            )
            SliderValueRow(
                label = stringResource(R.string.bp_height),
                value = height,
                range = 4..96,
                onValueChange = onHeightChanged,
                keyPoints = listOf(16, 32, 64, 96),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockPaintScreenPreview() {
    PreviewAppTheme {
        BlockPaintScreen(
            state = BlockPaintState(),
            onBlockSelected = {},
            onToolSelected = {},
            onWidthChanged = {},
            onHeightChanged = {},
            onPaint = { _, _ -> },
            onClear = {},
        )
    }
}
