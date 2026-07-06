package io.github.moxisuki.blockprint.cat.ui.tools.texttoblueprint

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextToBlueprintScreen(
    onBack: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    // TTB 不带自己的 TopAppBar：父级 AppNavGraph 已经为非 home 路由渲染 AppTopBar
    // 并带返回箭头（flags.isTextToBlueprint -> showBackButton）。再加一个会重叠。
    // 这里只渲染正文（输入框 + 按钮 + 状态），用 onBack 走外层返回。

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.tool_text_to_blueprint_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            placeholder = { Text(stringResource(R.string.ttb_input_hint)) },
            supportingText = { Text(stringResource(R.string.ttb_input_supporting)) },
        )

        FilledTonalButton(
            onClick = {
                // 占位实现：等后续接入 text → bitmap → ITB pipeline
                status = "已收到 ${text.length} 个字符的输入（占位：NBT / LLM 解析未接）"
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.ttb_generate))
        }

        status?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}
