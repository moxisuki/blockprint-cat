package io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.pixelart.api.ExportApi
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.format.formatShortLabelRes
import io.github.moxisuki.blockprint.core.SchematicFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintPreviewContent(
    encodedResult: String,
    onDismiss: () -> Unit,
    viewModel: BlueprintPreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(encodedResult) {
        viewModel.init(encodedResult)
    }

    LaunchedEffect(state.saveMessage) {
        val msg = state.saveMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSaveMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        // 导出类型（FilterChip + FlowRow 自动换行，不会被裁切）
        Text(
            stringResource(R.string.bp_export_type),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExportType.entries.forEach { type ->
                FilterChip(
                    selected = state.exportType == type,
                    onClick = { viewModel.setExportType(type) },
                    label = { Text(stringResource(type.labelRes())) },
                )
            }
        }

        // 分支 1: MC 命令 → 6 方向选择 + 命令预览 + 复制/分享
        if (state.exportType == ExportType.MC_COMMANDS) {
            McCommandBranch(
                direction = state.commandDirection,
                onDirectionChange = viewModel::setCommandDirection,
                commandsText = state.commandsText,
                isBuilding = state.isBuilding,
                progress = state.buildProgress,
                onGenerate = viewModel::buildBlueprint,
            )
        } else {
            // 分支 2: 蓝图 → WALL/FLAT + 名称 + 保存
            BlueprintSaveBranch(
                mode = state.blueprintMode,
                onModeChange = viewModel::setBlueprintMode,
                blueprintBytes = state.blueprintBytes,
                isBuilding = state.isBuilding,
                progress = state.buildProgress,
                onBuild = viewModel::buildBlueprint,
                onSaveClick = { showSaveDialog = true },
            )
        }
        Spacer(Modifier.height(4.dp))
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }

    if (showSaveDialog) {
        SaveNameDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name ->
                showSaveDialog = false
                viewModel.save(name)
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun McCommandBranch(
    direction: ExportApi.CommandDirection,
    onDirectionChange: (ExportApi.CommandDirection) -> Unit,
    commandsText: String,
    isBuilding: Boolean,
    progress: Float,
    onGenerate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.itb_export_direction_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExportApi.CommandDirection.entries.forEach { dir ->
                AssistChip(
                    onClick = { onDirectionChange(dir) },
                    label = { Text(dir.shortLabel()) },
                    colors = if (direction == dir) {
                        AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        AssistChipDefaults.assistChipColors()
                    },
                )
            }
        }
        FilledTonalButton(
            onClick = onGenerate,
            enabled = !isBuilding,
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Text(stringResource(R.string.itb_export_generate))
        }
        // 进度条（统一组件，两个分支都用）
        GenerationProgressBar(progress = progress, isBuilding = isBuilding)
        if (commandsText.isNotEmpty()) {
            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
            val context = androidx.compose.ui.platform.LocalContext.current
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
            var copied by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = commandsText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                )
            }
            // 复制 + 分享为 .mcfunction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString(commandsText))
                        copied = true
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = if (copied) androidx.compose.material.icons.Icons.Outlined.Check else androidx.compose.material.icons.Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(16.dp),
                    )
                    Text(stringResource(if (copied) R.string.bp_action_copied else R.string.bp_action_copy))
                }
                FilledTonalButton(
                    onClick = {
                        // 把命令写入 cache 目录，以 .mcfunction 分享
                        val file = java.io.File(context.cacheDir, "mc_commands_${System.currentTimeMillis()}.mcfunction")
                        file.writeText(commandsText)
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/*"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "MC Commands")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            android.content.Intent.createChooser(intent, "分享 MC 命令")
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(16.dp),
                    )
                    Text(stringResource(R.string.bp_action_share))
                }
            }
            LaunchedEffect(copied) {
                if (copied) {
                    kotlinx.coroutines.delay(1200)
                    copied = false
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlueprintSaveBranch(
    mode: BlueprintMode,
    onModeChange: (BlueprintMode) -> Unit,
    blueprintBytes: ByteArray?,
    isBuilding: Boolean,
    progress: Float,
    onBuild: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 模式选择
        val modes = BlueprintMode.entries
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modes.forEachIndexed { index, m ->
                SegmentedButton(
                    selected = mode == m,
                    onClick = { onModeChange(m) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                ) {
                    Text(stringResource(m.labelRes()))
                }
            }
        }
        // 构建 + 保存
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(
                onClick = onBuild,
                enabled = !isBuilding,
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Text(stringResource(R.string.bp_build))
            }
            // 进度条（统一组件，两个分支都用）
            GenerationProgressBar(progress = progress, isBuilding = isBuilding)
            FilledTonalButton(
                onClick = onSaveClick,
                enabled = blueprintBytes != null,
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(stringResource(R.string.bp_save))
            }
        }
    }
}

/** 简短标签，便于在 chip 里展示 */
private fun ExportApi.CommandDirection.shortLabel(): String = when (this) {
    ExportApi.CommandDirection.ES -> "ES · 东南"
    ExportApi.CommandDirection.WS -> "WS · 西南"
    ExportApi.CommandDirection.EN -> "EN · 东北"
    ExportApi.CommandDirection.WN -> "WN · 西北"
    ExportApi.CommandDirection.EU -> "EU · 东上"
    ExportApi.CommandDirection.NU -> "NU · 北上"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveNameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember {
        mutableStateOf("blueprint_" + System.currentTimeMillis().toString())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bp_save_dialog_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.bp_save_dialog_hint)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(stringResource(R.string.bp_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

private fun ExportType.labelRes(): Int = when (this) {
    // MC 命令用专属 label
    ExportType.MC_COMMANDS -> R.string.bp_type_mc_commands
    // 蓝图类型用蓝图列表 chip 同一套 i18n（formatShortLabelRes），保证全 app 一致
    ExportType.BLUEPRINT_LITEMATICA -> formatShortLabelRes(SchematicFormat.Litematica)
    ExportType.BLUEPRINT_SPONGE -> formatShortLabelRes(SchematicFormat.Sponge)
    ExportType.BLUEPRINT_STRUCTURE -> formatShortLabelRes(SchematicFormat.Structure)
    ExportType.BLUEPRINT_BUILDING_HELPER -> formatShortLabelRes(SchematicFormat.BuildingHelper)
}

private fun BlueprintMode.labelRes(): Int = when (this) {
    BlueprintMode.WALL -> R.string.bp_mode_wall
    BlueprintMode.FLAT -> R.string.bp_mode_flat
}

/**
 * 统一的生成进度条。MC 命令分支和蓝图分支都用。
 *
 * - 未在生成：bar 高度保留 4dp（占位），不显示文字
 * - 生成中：determinate 进度 0→1，从 VM 的 buildProgress 推
 * - 生成完毕：progress 回到 0f，bar 仍占位但视觉上是空的，等下次
 */
@Composable
private fun GenerationProgressBar(
    progress: Float,
    isBuilding: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isBuilding) progress else 0f,
        animationSpec = androidx.compose.animation.core.tween<Float>(durationMillis = 200),
        label = "build-progress",
    )
    androidx.compose.material3.LinearProgressIndicator(
        progress = { animatedProgress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth(),
    )
}