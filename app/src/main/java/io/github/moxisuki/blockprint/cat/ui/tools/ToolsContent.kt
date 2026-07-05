package io.github.moxisuki.blockprint.cat.ui.tools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ToolsContent(
    tools: List<ToolEntry>,
    onToolClick: (ToolEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hero = tools.firstOrNull { it.kind == ToolKind.Hero }
    val list = tools.filter { it.kind != ToolKind.Hero }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        hero?.let { entry ->
            ToolHero(entry = entry, onClick = { onToolClick(entry) })
            Spacer(Modifier.height(12.dp))
        }
        Surface(shape = RoundedCornerShape(14.dp)) {
            Column {
                list.forEachIndexed { index, entry ->
                    ToolRow(entry = entry, onClick = { onToolClick(entry) })
                    if (index < list.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 60.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        }
    }
}
