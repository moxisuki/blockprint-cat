package io.github.moxisuki.blockprint.cat.ui.management

import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import android.net.Uri
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.activity.compose.rememberLauncherForActivityResult
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.activity.result.contract.ActivityResultContracts
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Arrangement
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Box
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Column
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Row
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.Spacer
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.padding
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.background
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.draw.clip
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.fillMaxSize
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.fillMaxWidth
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.height
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.padding
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.layout.width
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.lazy.LazyColumn
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.foundation.lazy.items
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.Icons
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.filled.Add
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.filled.Delete
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.filled.Description
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.filled.Edit
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material.icons.filled.Info
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.AlertDialog
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.Card
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.CardDefaults
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.CircularProgressIndicator
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.ExperimentalMaterial3Api
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.FloatingActionButton
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.Icon
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.IconButton
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.MaterialTheme
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.OutlinedTextField
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.Scaffold
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.SnackbarHost
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.SnackbarHostState
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.Text
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.TextButton
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.material3.TopAppBar
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.runtime.Composable
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.runtime.LaunchedEffect
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.runtime.collectAsState
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.runtime.getValue
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.runtime.mutableStateOf
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.runtime.remember
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.runtime.setValue
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.Alignment
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.Modifier
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.text.style.TextOverflow
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.ui.format.BadgeColor
import io.github.moxisuki.blockprint.cat.ui.format.FormatCatalog
import io.github.moxisuki.blockprint.cat.ui.format.formatShortLabelRes
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueprintManagementScreen(
    navController: NavController,
    viewModel: BlueprintViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var deleteTarget by remember { mutableStateOf<BlueprintMeta?>(null) }
    var renameTarget by remember { mutableStateOf<BlueprintMeta?>(null) }
    var renameText by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.loadWithContext(context, it) }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BlockPrint Cat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePicker.launch(arrayOf("application/octet-stream", "*/*")) },
            ) {
                Icon(Icons.Default.Add, contentDescription = "加载蓝图")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.blueprints.isEmpty()) {
                EmptyManagementState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.blueprints, key = { it.uuid }, contentType = { "blueprint" }) { bp ->
                        BlueprintCard(
                            blueprint = bp,
                            onDetail = { navController.navigate(NavRoutes.detailRoute(bp.uuid)) },
                            onDelete = { deleteTarget = bp },
                            onRename = {
                                renameTarget = bp
                                renameText = bp.fileName
                            },
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { bp ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_msg, bp.displayName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(context, bp.uuid)
                        deleteTarget = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    // Rename dialog
    renameTarget?.let { bp ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.dialog_rename_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(stringResource(R.string.dialog_rename_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "文件将保存为: $renameText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank() && renameText != bp.fileName) {
                            viewModel.rename(context, bp.uuid, renameText)
                        }
                        renameTarget = null
                    },
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun EmptyManagementState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.mgmt_empty), style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "点击右下角 + 按钮加载第一个蓝图",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BlueprintCard(
    blueprint: BlueprintMeta,
    onDetail: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = blueprint.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FormatChip(format = blueprint.format)
                    }
                    Text(
                        text = blueprint.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "作者: ${blueprint.author.ifEmpty { "未知" }}  |  区域: ${blueprint.regionCount}  |  方块: ${formatNumber(blueprint.blockCount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                IconButton(onClick = onDetail) {
                    Icon(Icons.Default.Info, contentDescription = "详情")
                }
                IconButton(onClick = onRename) {
                    Icon(Icons.Default.Edit, contentDescription = "重命名")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/**
 * Small color-coded badge that shows which NBT format a blueprint
 * was parsed as. See HomeScreen for the same component.
 */
@Composable
private fun FormatChip(
    format: io.github.moxisuki.blockprint.core.SchematicFormat,
) {
    val display = FormatCatalog.from(format)
    val bg = when (display.badgeColor) {
        BadgeColor.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        BadgeColor.Secondary -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        // Outline covers Structure / PartialNbt / BuildingHelper / Unknown per the catalog.
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(formatShortLabelRes(format)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}