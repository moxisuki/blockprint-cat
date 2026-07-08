@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.moxisuki.blockprint.cat.ui.tools.texttoblueprint

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.BlockCatalog
import io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.components.prewarmPixelArt
import kotlinx.coroutines.launch

@Composable
fun TextToBlueprintScreen(
    navController: androidx.navigation.NavController,
    viewModel: TextToBlueprintViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            prewarmPixelArt(context, BlockCatalog.all.map { it.drawableResId })
        }
    }

    LaunchedEffect(state.exportPayload) {
        if (state.exportPayload != null) sheetState.show()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.gridW > 0 && state.gridH > 0) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                val d = LocalDensity.current
                val vw = with(d) { maxWidth.toPx() }; val vh = with(d) { maxHeight.toPx() }
                val raw = kotlin.math.min(vw / state.gridW, vh / state.gridH).coerceAtLeast(1f)
                val csi = raw.toInt().coerceAtLeast(1)
                val bp = csi.toFloat()
                val cox = ((vw.toInt() - csi * state.gridW) / 2).toFloat()
                val coy = ((vh.toInt() - csi * state.gridH) / 2).toFloat()
                io.github.moxisuki.blockprint.cat.ui.tools.blockpaint.BlockCanvas(
                    state.gridW, state.gridH, state.grid, context,
                    1f, androidx.compose.ui.geometry.Offset.Zero, bp, cox, coy,
                    { _, _ -> }, { _, _ -> }, Modifier.fillMaxSize(),
                )
            }
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.ttb_preview_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        OutlinedTextField(
            value = state.text, onValueChange = viewModel::setText,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp, max = 120.dp).padding(horizontal = 12.dp, vertical = 4.dp),
            placeholder = { Text(stringResource(R.string.ttb_input_hint)) },
        )

        TtbBlockPicker(selectedBlockId = state.selectedBlockId, onSelect = viewModel::setSelectedBlock)

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
            Text(stringResource(R.string.ttb_scale, state.scale), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
            Slider(state.scale.toFloat(), { viewModel.setScale(it.toInt()) }, valueRange = 1f..8f, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.ttb_spacing, state.spacing), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
            Slider(state.spacing.toFloat(), { viewModel.setSpacing(it.toInt()) }, valueRange = 0f..8f, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp), Arrangement.spacedBy(4.dp), Alignment.CenterVertically) {
            Text(stringResource(R.string.ttb_height, state.height), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
            Slider(state.height.toFloat(), { viewModel.setHeight(it.toInt()) }, valueRange = TextToBlueprintState.MIN_HEIGHT.toFloat()..TextToBlueprintState.MAX_HEIGHT.toFloat(), modifier = Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }

        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ttb_size_hint, state.gridW, state.gridH), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::prepareExport, enabled = state.gridW > 0 && !state.isUpdating) {
                    Icon(Icons.Outlined.IosShare, null, Modifier.size(18.dp), tint = if (state.gridW > 0 && !state.isUpdating) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.bp_export_short))
                }
            }
        }
    }

    val suffix = stringResource(R.string.bp_painting_suffix)
    val name = (state.text.take(20).ifBlank { "text" } + "_$suffix").trim()
    io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon.ExportBottomSheet(
        exportPayload = state.exportPayload,
        resultBitmap = state.resultBitmap,
        defaultSaveName = name,
        sheetState = sheetState,
        context = context,
        onClose = { viewModel.clearExport() },
        onViewBlueprint = { uuid ->
            navController.navigate(io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes.detailRoute(uuid))
        },
    )
}

@Composable
private fun TtbBlockPicker(selectedBlockId: String?, onSelect: (String?) -> Unit) {
    io.github.moxisuki.blockprint.cat.ui.tools.blockpaint.BlockPickerStrip(
        selectedBlockId = selectedBlockId,
        onSelect = onSelect,
    )
}
