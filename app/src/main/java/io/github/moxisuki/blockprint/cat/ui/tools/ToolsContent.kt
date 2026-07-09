package io.github.moxisuki.blockprint.cat.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        hero?.let { entry ->
            ToolHero(entry = entry, onClick = { onToolClick(entry) })
        }
        Spacer(Modifier.height(14.dp))
        if (list.size >= 2) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                list.forEach { entry ->
                    ToolRow(
                        entry = entry,
                        onClick = { onToolClick(entry) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            list.forEach { entry ->
                ToolRow(entry = entry, onClick = { onToolClick(entry) })
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
