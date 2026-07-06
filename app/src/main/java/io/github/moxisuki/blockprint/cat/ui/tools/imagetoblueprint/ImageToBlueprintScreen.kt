package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components.AdjustSlider
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components.BlockGroupSection
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components.prewarmPixelArt
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components.DitherDropdown
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components.PreviewHero
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components.ResultMaterialsPanel
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components.TransparencySection
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components.WidthInput
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Grid4x4
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.WidthNormal
import androidx.compose.material.icons.sharp.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.moxisuki.blockprint.cat.R

private const val SEC_DITHER = "dither"
private const val SEC_OUTPUT = "output"
private const val SEC_ADJUST = "adjust"
private const val SEC_TRANSPARENCY = "transparency"
private const val SEC_BLOCKS = "blocks"
private const val SEC_RESULT = "result"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageToBlueprintScreen(
    navController: androidx.navigation.NavController,
    viewModel: ImageToBlueprintViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var exportPayload by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // 预热方块缩略图 cache：在 IO 线程把 140 个方块都 decode 一次，
    // 之后展开任何方块组都是 0 延迟。
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            prewarmPixelArt(
                context = context,
                resIds = io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.BlockCatalog.all
                    .map { it.drawableResId },
            )
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { resolved ->
            val (w, h) = readImageDimensions(context, resolved)
            viewModel.setImage(resolved, w, h)
        }
    }

    var expanded by remember { mutableStateOf(setOf(SEC_DITHER)) }
    var enlargedPreview by remember { mutableStateOf(false) }

    LaunchedEffect(state.resultBitmap) {
        if (state.resultBitmap != null) expanded = expanded + SEC_RESULT
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) expanded = expanded + SEC_RESULT
    }

    val enlarged = state.resultBitmap
    if (enlarged != null && enlargedPreview) {
        EnlargedPreviewDialog(
            bitmap = enlarged,
            width = state.resultWidth,
            height = state.resultHeight,
            totalBlocks = state.resultTotalBlocks,
            onDismiss = { enlargedPreview = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        PreviewHero(
            sourceUri = state.imageUri,
            sourceWidth = state.imageWidth,
            sourceHeight = state.imageHeight,
            resultBitmap = state.resultBitmap,
            resultWidth = state.resultWidth,
            resultHeight = state.resultHeight,
            previewMode = state.previewMode,
            isUpdating = state.isUpdating,
            lastUpdatedAt = state.lastUpdatedAt,
            onReselect = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onEnlarge = { enlargedPreview = true },
        )

        Spacer(Modifier.height(20.dp))

        CollapsibleSection(
            id = SEC_OUTPUT,
            icon = Icons.Outlined.WidthNormal,
            title = stringResource(R.string.itb_section_output),
            expanded = expanded,
            onToggle = { expanded = expanded.toggle(it) },
        ) {
            WidthInput(state.targetWidth, viewModel::setTargetWidth)
        }

        CollapsibleSection(
            id = SEC_DITHER,
            icon = Icons.Outlined.Grid4x4,
            title = stringResource(R.string.itb_section_dither),
            expanded = expanded,
            onToggle = { expanded = expanded.toggle(it) },
        ) {
            DitherDropdown(state.ditherMethod, viewModel::setDitherMethod)
        }

        CollapsibleSection(
            id = SEC_ADJUST,
            icon = Icons.Outlined.Brightness5,
            title = stringResource(R.string.itb_section_adjust),
            expanded = expanded,
            onToggle = { expanded = expanded.toggle(it) },
            trailing = {
                if (SEC_ADJUST in expanded) {
                    Text(
                        stringResource(R.string.itb_reset),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { viewModel.resetAdjustments() },
                    )
                }
            },
        ) {
            AdjustSlider(stringResource(R.string.itb_brightness), state.brightness, viewModel::setBrightness)
            AdjustSlider(stringResource(R.string.itb_contrast), state.contrast, viewModel::setContrast)
            AdjustSlider(stringResource(R.string.itb_saturation), state.saturation, viewModel::setSaturation)
        }

        CollapsibleSection(
            id = SEC_TRANSPARENCY,
            icon = Icons.Outlined.Opacity,
            title = stringResource(R.string.itb_section_transparency),
            expanded = expanded,
            onToggle = { expanded = expanded.toggle(it) },
        ) {
            TransparencySection(
                enabled = state.transparencyEnabled,
                tolerance = state.transparencyTolerance,
                onEnabledChange = viewModel::setTransparencyEnabled,
                onToleranceChange = viewModel::setTransparencyTolerance,
            )
        }

        CollapsibleSection(
            id = SEC_BLOCKS,
            icon = Icons.Outlined.FilterList,
            title = stringResource(R.string.itb_section_blocks),
            expanded = expanded,
            onToggle = { expanded = expanded.toggle(it) },
        ) {
            BlockGroupSection(selectedGroups = state.selectedGroups, onToggleGroup = viewModel::toggleGroup)
        }

CollapsibleSection(
            id = SEC_RESULT,
            title = stringResource(R.string.itb_section_result),
            expanded = expanded,
            onToggle = { expanded = expanded.toggle(it) },
        ) {
            ResultMaterialsPanel(
                totalBlocks = state.resultTotalBlocks,
                materialCounts = state.resultMaterialCounts,
                errorMessage = state.errorMessage,
            )
        }

        // MC 命令导出已合并到 BlueprintPreviewScreen 独立分支（命令导出 → 选方向 → 预览）
        Spacer(Modifier.height(20.dp))

        ExportButton(
            enabled = state.imageUri != null && state.resultBitmap != null,
            isUpdating = state.isUpdating,
            onClick = {
                viewModel.encodeForExport()?.let { encoded ->
                    exportPayload = encoded
                    scope.launch { sheetState.show() }
                }
            },
        )

        Spacer(Modifier.height(24.dp))
    }

    // 底部弹出框：导出配置（MC 命令 / 蓝图），不是独立路由
    if (exportPayload != null) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) exportPayload = null
                }
            },
            sheetState = sheetState,
        ) {
            io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview.BlueprintPreviewContent(
                encodedResult = exportPayload!!,
                onViewBlueprint = { uuid ->
                    // 关弹框 → 跳详情页
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            exportPayload = null
                            navController.navigate(io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes.detailRoute(uuid))
                        }
                    }
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) exportPayload = null
                    }
                },
            )
        }
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item

@Composable
private fun CollapsibleSection(
    id: String,
    title: String,
    expanded: Set<String>,
    onToggle: (String) -> Unit,
    icon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isExpanded = id in expanded
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(id) }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            trailing?.invoke()
            val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "chevron")
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp).rotate(rotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                content()
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    }
}

@Composable
private fun ResultPreview(
    bitmap: android.graphics.Bitmap?,
    width: Int,
    height: Int,
    totalBlocks: Int,
    materialCounts: Map<String, Int>,
    errorMessage: String?,
    onEnlarge: () -> Unit,
) {
    when {
        errorMessage != null -> {
            Text(
                stringResource(R.string.itb_result_error, errorMessage),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        bitmap != null -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val shape = RoundedCornerShape(12.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 320.dp)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            shape,
                        )
                        .clickable { onEnlarge() },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.itb_enlarge_hint),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).padding(8.dp),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None,
                    )
                }
                Text(
                    stringResource(R.string.itb_result_size, width, height) +
                        "  ·  " +
                        stringResource(R.string.itb_result_total, totalBlocks),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (materialCounts.isNotEmpty()) {
                    Text(
                        stringResource(R.string.itb_result_materials),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    )
                    val sorted = materialCounts.entries.sortedByDescending { it.value }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        sorted.take(10).forEach { (name, count) ->
                            val displayName = name.replace('_', ' ')
                                .split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(displayName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "× $count",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (sorted.size > 10) {
                            Text(
                                "+ ${sorted.size - 10} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
        else -> {
            Text(
                stringResource(R.string.itb_no_result),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun EnlargedPreviewDialog(
    bitmap: android.graphics.Bitmap,
    width: Int,
    height: Int,
    totalBlocks: Int,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.95f))
                .clickable { onDismiss() },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(top = 56.dp, bottom = 80.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.None,
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.itb_result_size, width, height) +
                        "  ·  " +
                        stringResource(R.string.itb_result_total, totalBlocks),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.itb_enlarge_dismiss),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

private fun readImageDimensions(
    context: android.content.Context,
    uri: Uri,
): Pair<Int, Int> {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, opts)
            (opts.outWidth to opts.outHeight).takeIf { it.first > 0 && it.second > 0 } ?: (0 to 0)
        } ?: (0 to 0)
    } catch (_: Exception) {
        0 to 0
    }
}

@Composable
private fun ImagePreviewSection(
    sourceUri: Uri?,
    sourceWidth: Int,
    sourceHeight: Int,
    resultBitmap: android.graphics.Bitmap?,
    resultWidth: Int,
    resultHeight: Int,
    onReselect: () -> Unit,
    onEnlarge: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val bitmapToShow = resultBitmap
    val labelToShow: String? = if (resultBitmap != null) {
        stringResource(R.string.itb_result_size, resultWidth, resultHeight)
    } else if (sourceWidth > 0 && sourceHeight > 0) {
        stringResource(R.string.itb_image_size, sourceWidth, sourceHeight)
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 280.dp)
                .clip(shape)
                .then(
                    if (sourceUri == null && resultBitmap == null) {
                        Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), shape)
                            .clickable { onReselect() }
                    } else {
                        Modifier.clickable { onEnlarge() }
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                bitmapToShow != null -> {
                    Image(
                        bitmap = bitmapToShow.asImageBitmap(),
                        contentDescription = stringResource(R.string.itb_enlarge_hint),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.None,
                    )
                }
                sourceUri != null -> {
                    AsyncImage(
                        model = sourceUri,
                        contentDescription = stringResource(R.string.itb_select_image),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.itb_select_image_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            if (labelToShow != null) {
                Text(
                    labelToShow,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (sourceUri != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                androidx.compose.material3.TextButton(onClick = onReselect) {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.itb_reselect))
                }
            }
        }
    }
}

