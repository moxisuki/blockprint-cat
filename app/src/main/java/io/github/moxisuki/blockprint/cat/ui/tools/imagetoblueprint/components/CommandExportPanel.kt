package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.github.moxisuki.pixelart.api.ExportApi
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.ImageToBlueprintState

/**
 * MC 命令导出面板：6 方向选择 + "生成预览"按钮 + 命令文本预览。
 * 方向定义见 ExportApi.CommandDirection。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CommandExportPanel(
    state: ImageToBlueprintState,
    onDirectionChange: (ExportApi.CommandDirection) -> Unit,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    colors = if (state.commandDirection == dir) {
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
        Button(
            onClick = onGenerate,
            enabled = state.resultBitmap != null,
            modifier = Modifier.fillMaxWidth().height(40.dp),
        ) {
            Text(stringResource(R.string.itb_export_generate))
        }
        if (state.commandsText.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = state.commandsText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                )
            }
        }
    }
}

/** 简短标签，便于在 6 个 chip 里展示 */
private fun ExportApi.CommandDirection.shortLabel(): String = when (this) {
    ExportApi.CommandDirection.ES -> "ES · 东南"
    ExportApi.CommandDirection.WS -> "WS · 西南"
    ExportApi.CommandDirection.EN -> "EN · 东北"
    ExportApi.CommandDirection.WN -> "WN · 西北"
    ExportApi.CommandDirection.EU -> "EU · 东上"
    ExportApi.CommandDirection.NU -> "NU · 北上"
}
