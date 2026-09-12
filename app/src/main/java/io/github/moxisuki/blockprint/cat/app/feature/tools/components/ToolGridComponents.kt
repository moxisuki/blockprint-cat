package io.github.moxisuki.blockprint.cat.app.feature.tools.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolBlock
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolBlockPalette
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolGrid
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.floor
import kotlin.math.ceil
import kotlin.math.min

@Composable
internal fun ToolGridPreview(
    grid: ToolGrid?,
    placeholder: String,
    modifier: Modifier = Modifier,
    editable: Boolean = false,
    framed: Boolean = true,
    onCellChange: (x: Int, y: Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val textureMap = remember(context, ToolBlockPalette.blocks) {
        ToolBlockPalette.blocks.associate { block ->
            block.id to getToolBlockTexture(context, block.drawableResId)
        }
    }
    if (framed) {
        Card(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            insideMargin = PaddingValues(12.dp),
        ) {
            ToolGridPreviewContent(
                grid = grid,
                placeholder = placeholder,
                textures = textureMap,
                editable = editable,
                framed = true,
                onCellChange = onCellChange,
            )
        }
    } else {
        Box(modifier = modifier) {
            ToolGridPreviewContent(
                grid = grid,
                placeholder = placeholder,
                textures = textureMap,
                editable = editable,
                framed = false,
                onCellChange = onCellChange,
            )
        }
    }
}

@Composable
private fun ToolGridPreviewContent(
    grid: ToolGrid?,
    placeholder: String,
    textures: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    editable: Boolean,
    framed: Boolean,
    onCellChange: (x: Int, y: Int) -> Unit,
) {
    if (grid == null || grid.width <= 0 || grid.height <= 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (framed) Modifier.height(220.dp) else Modifier.fillMaxSize())
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = placeholder,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
        return
    }

    val aspect = (grid.width.toFloat() / grid.height.toFloat()).coerceIn(0.45f, 2.2f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (framed) {
                    Modifier.heightIn(min = 220.dp, max = 430.dp).aspectRatio(aspect)
                } else {
                    Modifier.fillMaxSize()
                },
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.46f))
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .then(
                if (editable) {
                    Modifier.pointerInput(grid.width, grid.height) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial)
                            cellAt(down.position, grid.width, grid.height, size.width.toFloat(), size.height.toFloat())
                                ?.let { onCellChange(it.x, it.y) }
                            do {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        cellAt(
                                            change.position,
                                            grid.width,
                                            grid.height,
                                            size.width.toFloat(),
                                            size.height.toFloat(),
                                        )?.let { onCellChange(it.x, it.y) }
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        drawGrid(
            grid = grid,
            textures = textures,
            emptyColor = Color.Transparent,
            gridLineColor = Color.Black.copy(alpha = if (grid.width <= 64 && grid.height <= 64) 0.10f else 0.03f),
            backgroundColor = Color.White.copy(alpha = 0.08f),
        )
    }
}

@Composable
internal fun ToolPaletteStrip(
    selectedBlockId: String,
    onBlockSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolBlockPalette.blocks.forEach { block ->
            val selected = selectedBlockId == block.id
            val texture = getToolBlockTexture(LocalContext.current, block.drawableResId)
            Box(
                modifier = Modifier
                    .size(if (selected) 42.dp else 36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.outline.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { onBlockSelected(block.id) },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = texture,
                    contentDescription = block.namespaceId,
                    modifier = Modifier
                        .size(if (selected) 30.dp else 28.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.None,
                )
            }
        }
    }
}

@Composable
internal fun ToolPalettePicker(
    selectedBlockId: String,
    onBlockSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = ToolBlockPalette.groups
    var selectedGroupIndex by remember {
        mutableIntStateOf(
            groups.indexOfFirst { group -> group.blocks.any { it.id == selectedBlockId } }.coerceAtLeast(0),
        )
    }
    LaunchedEffect(selectedBlockId, groups) {
        val nextIndex = groups.indexOfFirst { group -> group.blocks.any { it.id == selectedBlockId } }
        if (nextIndex >= 0) selectedGroupIndex = nextIndex
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        TabRow(
            tabs = groups.map { stringResource(it.labelRes) },
            selectedTabIndex = selectedGroupIndex,
            onTabSelected = { selectedGroupIndex = it.coerceIn(groups.indices) },
            modifier = Modifier.fillMaxWidth(),
        )
        val group = groups[selectedGroupIndex]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            group.blocks.forEach { block ->
                val selected = selectedBlockId == block.id
                val texture = getToolBlockTexture(LocalContext.current, block.drawableResId)
                Box(
                    modifier = Modifier
                        .size(if (selected) 44.dp else 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable { onBlockSelected(block.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = texture,
                        contentDescription = block.namespaceId,
                        modifier = Modifier
                            .size(if (selected) 32.dp else 30.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.FillBounds,
                        filterQuality = FilterQuality.None,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MaterialSummaryCard(
    grid: ToolGrid?,
    modifier: Modifier = Modifier,
) {
    val counts = remember(grid) { grid?.materialCounts.orEmpty() }
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.itb_result_materials),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            grid?.let {
                Text(
                    text = stringResource(R.string.bp_result_total, it.paintedCount.toString()),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (counts.isEmpty()) {
            Text(
                text = stringResource(R.string.bp_result_empty),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        } else {
            val maxCount = counts.maxOf { it.count }.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                counts.take(12).forEach { count ->
                    MaterialRow(
                        block = ToolBlockPalette.block(count.blockId),
                        fallbackId = count.blockId,
                        count = count.count,
                        fraction = count.count.toFloat() / maxCount.toFloat(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value.toString(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
        IconButton(
            onClick = { onValueChange((value - 1).coerceIn(range)) },
            enabled = value > range.first,
            minWidth = 38.dp,
            minHeight = 38.dp,
        ) {
            Icon(Icons.Filled.Remove, contentDescription = null)
        }
        IconButton(
            onClick = { onValueChange((value + 1).coerceIn(range)) },
            enabled = value < range.last,
            minWidth = 38.dp,
            minHeight = 38.dp,
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
}

@Composable
internal fun SliderValueRow(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    keyPoints: List<Int> = emptyList(),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value.toString(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(range)) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (range.last - range.first - 1).coerceAtLeast(0),
            showKeyPoints = keyPoints.isNotEmpty(),
            keyPoints = keyPoints.map { it.coerceIn(range).toFloat() },
            magnetThreshold = 0.035f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun DrawScope.drawGrid(
    grid: ToolGrid,
    textures: Map<String, androidx.compose.ui.graphics.ImageBitmap>,
    emptyColor: Color,
    gridLineColor: Color,
    backgroundColor: Color,
) {
    val cell = min(size.width / grid.width, size.height / grid.height)
    val drawnWidth = cell * grid.width
    val drawnHeight = cell * grid.height
    val startX = (size.width - drawnWidth) / 2f
    val startY = (size.height - drawnHeight) / 2f
    drawRect(backgroundColor, topLeft = Offset(startX, startY), size = Size(drawnWidth, drawnHeight))
    val useTextures = cell >= 7f
    for (y in 0 until grid.height) {
        for (x in 0 until grid.width) {
            val block = ToolBlockPalette.block(grid.cellAt(x, y))
            val texture = block?.let { textures[it.id] }
            if (texture != null && useTextures) {
                val sizePx = ceil(cell).toInt().coerceAtLeast(1)
                drawImage(
                    image = texture,
                    dstOffset = IntOffset((startX + x * cell).toInt(), (startY + y * cell).toInt()),
                    dstSize = IntSize(sizePx, sizePx),
                    filterQuality = FilterQuality.None,
                )
            } else {
                val color = block?.color ?: emptyColor
                if (color.alpha > 0f) {
                    drawRect(
                        color = color,
                        topLeft = Offset(startX + x * cell, startY + y * cell),
                        size = Size(cell + 0.25f, cell + 0.25f),
                    )
                }
            }
        }
    }
    if (cell >= 8f) {
        for (x in 0..grid.width) {
            val px = startX + x * cell
            drawLine(gridLineColor, Offset(px, startY), Offset(px, startY + drawnHeight), strokeWidth = 1f)
        }
        for (y in 0..grid.height) {
            val py = startY + y * cell
            drawLine(gridLineColor, Offset(startX, py), Offset(startX + drawnWidth, py), strokeWidth = 1f)
        }
    }
}

@Composable
private fun MaterialRow(
    block: ToolBlock?,
    fallbackId: String,
    count: Int,
    fraction: Float,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolMaterialIcon(block = block, fallbackId = fallbackId)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block?.displayName ?: fallbackId.substringAfter(':').replace("_", " "),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = block?.namespaceId ?: fallbackId,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = count.toString(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.72f)),
            )
        }
    }
}

@Composable
private fun ToolMaterialIcon(block: ToolBlock?, fallbackId: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (block != null) {
            Image(
                bitmap = getToolBlockTexture(LocalContext.current, block.drawableResId),
                contentDescription = block.namespaceId,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.None,
            )
        } else {
            Text(
                text = fallbackId.substringAfter(':', fallbackId).replace("_", "").take(2).uppercase(),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

private fun cellAt(position: Offset, width: Int, height: Int, canvasWidth: Float, canvasHeight: Float): GridCell? {
    val cell = min(canvasWidth / width, canvasHeight / height)
    val drawnWidth = cell * width
    val drawnHeight = cell * height
    val startX = (canvasWidth - drawnWidth) / 2f
    val startY = (canvasHeight - drawnHeight) / 2f
    val x = floor((position.x - startX) / cell).toInt()
    val y = floor((position.y - startY) / cell).toInt()
    return if (x in 0 until width && y in 0 until height) GridCell(x, y) else null
}

@Immutable
private data class GridCell(val x: Int, val y: Int)
