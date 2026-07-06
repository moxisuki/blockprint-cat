package io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintPreviewScreen(
    encodedResult: String,
    onBack: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bp_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            // 3D 占位预览区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.bp_placeholder),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // 导出类型（5 选 1）
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.bp_export_type),
                    style = MaterialTheme.typography.titleSmall,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = ExportType.entries
                    types.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = state.exportType == type,
                            onClick = { viewModel.setExportType(type) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                        ) {
                            Text(
                                text = stringResource(type.labelRes()),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            // 蓝图模式（WALL / FLAT，仅在不是 MC_COMMANDS 时显示）
            if (state.exportType != ExportType.MC_COMMANDS) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.bp_export_type) + " · mode",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val modes = BlueprintMode.entries
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = state.blueprintMode == mode,
                                onClick = { viewModel.setBlueprintMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            ) {
                                Text(stringResource(mode.labelRes()))
                            }
                        }
                    }
                }
            }
            // 操作按钮：build + save
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.buildBlueprint()
                    },
                    modifier = Modifier.height(48.dp),
                ) {
                    Text(stringResource(R.string.bp_build))
                }
                FilledTonalButton(
                    onClick = { showSaveDialog = true },
                    enabled = state.blueprintBytes != null || state.exportType == ExportType.MC_COMMANDS,
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                    Text(stringResource(R.string.bp_save))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
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
    ExportType.MC_COMMANDS -> R.string.bp_type_mc_commands
    ExportType.BLUEPRINT_LITEMATICA -> R.string.bp_type_litematica
    ExportType.BLUEPRINT_SPONGE -> R.string.bp_type_schematic
    ExportType.BLUEPRINT_STRUCTURE -> R.string.bp_type_structure
    ExportType.BLUEPRINT_BUILDING_HELPER -> R.string.bp_type_buildinghelper
}

private fun BlueprintMode.labelRes(): Int = when (this) {
    BlueprintMode.WALL -> R.string.bp_mode_wall
    BlueprintMode.FLAT -> R.string.bp_mode_flat
}