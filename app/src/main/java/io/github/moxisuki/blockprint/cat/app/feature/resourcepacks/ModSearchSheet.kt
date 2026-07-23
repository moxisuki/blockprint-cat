package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
internal fun ModSearchSheet(
    state: ModSearchState,
    onAction: (ResourcePacksAction) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    when (state) {
        ModSearchState.Closed -> Unit
        ModSearchState.Searching -> OverlayDialog(
            show = true,
            onDismissRequest = { onAction(ResourcePacksAction.ModSearchClose) },
            title = stringResource(R.string.resourcepacks_search_title),
            summary = stringResource(R.string.resourcepacks_search_hint),
        ) {
            TextButton(
                onClick = { onAction(ResourcePacksAction.ModSearchClose) },
                text = stringResource(android.R.string.cancel),
            )
        }
        is ModSearchState.Results -> OverlayDialog(
            show = true,
            onDismissRequest = { onAction(ResourcePacksAction.ModSearchClose) },
            title = stringResource(R.string.resourcepacks_search_title),
            summary = stringResource(R.string.resourcepacks_search_hint),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onAction(ResourcePacksAction.ModSearchQueryChanged(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.resourcepacks_search_hint),
                )
                TextButton(
                    onClick = { onAction(ResourcePacksAction.ModSearchSubmit) },
                    text = stringResource(R.string.action_search),
                )
                if (state.hits.isEmpty() && query.isNotBlank()) {
                    Text(stringResource(R.string.resourcepacks_search_empty))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(state.hits, key = { it.projectId }) { hit ->
                            TextButton(
                                onClick = { onAction(ResourcePacksAction.ModSearchHitSelected(hit)) },
                                modifier = Modifier.fillMaxWidth(),
                                text = "${hit.title} (${hit.slug})",
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { onAction(ResourcePacksAction.ModSearchClose) },
                    text = stringResource(android.R.string.cancel),
                )
            }
        }
        is ModSearchState.Versions -> OverlayDialog(
            show = true,
            onDismissRequest = { onAction(ResourcePacksAction.ModSearchClose) },
            title = stringResource(R.string.resourcepacks_versions_title),
            summary = state.hit.title,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.versions.isEmpty()) {
                    Text(stringResource(R.string.resourcepacks_search_no_version))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(state.versions, key = { it.id }) { v ->
                            TextButton(
                                onClick = { onAction(ResourcePacksAction.ModSearchVersionSelected(v)) },
                                modifier = Modifier.fillMaxWidth(),
                                text = "${v.name} · ${v.gameVersions.take(3).joinToString(", ")} · ${"%.1f".format(v.fileSize / 1048576.0)} MB",
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { onAction(ResourcePacksAction.ModSearchBack) },
                    text = stringResource(android.R.string.cancel),
                )
            }
        }
        is ModSearchState.VersionError -> OverlayDialog(
            show = true,
            onDismissRequest = { onAction(ResourcePacksAction.ModSearchClose) },
            title = stringResource(R.string.resourcepacks_versions_title),
            summary = state.message,
        ) {
            TextButton(
                onClick = { onAction(ResourcePacksAction.ModSearchBack) },
                text = stringResource(android.R.string.cancel),
            )
        }
    }
}
