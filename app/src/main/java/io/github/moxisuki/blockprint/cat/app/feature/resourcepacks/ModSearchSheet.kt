package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ModSearchSheet(
    state: ModSearchState,
    onAction: (ResourcePacksAction) -> Unit,
) {
    val visible = state !is ModSearchState.Closed
    if (!visible) return
    var query by remember { mutableStateOf("") }
    val title = when (state) {
        is ModSearchState.Results, ModSearchState.Searching -> stringResource(R.string.resourcepacks_search_title)
        is ModSearchState.Versions, is ModSearchState.VersionError -> stringResource(R.string.resourcepacks_versions_title)
        ModSearchState.Closed -> ""
    }
    OverlayBottomSheet(
        show = visible,
        title = title,
        onDismissRequest = { onAction(ResourcePacksAction.ModSearchClose) },
    ) {
        SearchBar(
            inputField = {
                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onAction(ResourcePacksAction.ModSearchQueryChanged(it))
                    },
                    label = stringResource(R.string.resourcepacks_search_hint),
                )
            },
            onExpandedChange = {},
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onAction(ResourcePacksAction.ModSearchSubmit) },
                ) {
                    Text(stringResource(R.string.action_search))
                }
                SearchBody(state = state, onAction = onAction, onBack = { onAction(ResourcePacksAction.ModSearchBack) })
            }
        }
    }
}

@Composable
private fun SearchBody(
    state: ModSearchState,
    onAction: (ResourcePacksAction) -> Unit,
    onBack: () -> Unit,
) {
    Spacer_8dp()
    when (state) {
        ModSearchState.Closed -> Unit
        ModSearchState.Searching -> Text(
            text = stringResource(R.string.action_search) + "…",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 16.dp),
        )
        is ModSearchState.Results -> ResultsList(state = state, onAction = onAction)
        is ModSearchState.Versions -> VersionsList(state = state, onAction = onAction, onBack = onBack)
        is ModSearchState.VersionError -> Column {
            Text(
                text = state.message,
                color = MiuixTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
            )
            BackButton(onBack)
        }
    }
}

@Composable
private fun ResultsList(state: ModSearchState.Results, onAction: (ResourcePacksAction) -> Unit) {
    if (state.hits.isEmpty()) {
        Text(
            text = stringResource(R.string.resourcepacks_search_empty),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(top = 16.dp),
        )
        return
    }
    Text(
        text = "${state.hits.size} ${stringResource(R.string.action_search)}",
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 16.dp),
    )
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
        items(state.hits, key = { it.projectId }) { hit ->
            HitRow(
                title = hit.title,
                subtitle = hit.slug,
                onClick = { onAction(ResourcePacksAction.ModSearchHitSelected(hit)) },
            )
        }
    }
}

@Composable
private fun VersionsList(
    state: ModSearchState.Versions,
    onAction: (ResourcePacksAction) -> Unit,
    onBack: () -> Unit,
) {
    Text(
        text = state.hit.title,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(top = 16.dp),
    )
    if (state.versions.isEmpty()) {
        Text(
            text = stringResource(R.string.resourcepacks_search_no_version),
            color = MiuixTheme.colorScheme.error,
        )
        BackButton(onBack)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
        items(state.versions, key = { it.id }) { v ->
            HitRow(
                title = v.name,
                subtitle = "${v.gameVersions.take(3).joinToString(", ")} · ${"%.1f".format(v.fileSize / 1048576.0)} MB",
                onClick = { onAction(ResourcePacksAction.ModSearchVersionSelected(v)) },
            )
        }
    }
    BackButton(onBack)
}

@Composable
private fun HitRow(title: String, subtitle: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f)),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = androidx.compose.ui.Alignment.Start) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    TextButton(
        modifier = Modifier.padding(top = 16.dp),
        onClick = onBack,
        text = stringResource(android.R.string.cancel),
    )
}

// Avoid unused-import false-positives on Spacer helper:
@Composable
private fun Spacer_8dp() { androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.padding(top = 8.dp)) }
