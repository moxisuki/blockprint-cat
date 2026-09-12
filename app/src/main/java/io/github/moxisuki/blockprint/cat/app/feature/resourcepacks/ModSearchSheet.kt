package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.size
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
import coil.compose.AsyncImage
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ModSearchSheet(
    state: ModSearchState,
    onAction: (ResourcePacksAction) -> Unit,
) {
    if (state is ModSearchState.Closed) return

    var query by remember { mutableStateOf((state as? ModSearchState.Results)?.query.orEmpty()) }
    LaunchedEffect(state) {
        if (state is ModSearchState.Results) query = state.query
    }

    val showingVersions = state is ModSearchState.LoadingVersions ||
        state is ModSearchState.Versions ||
        state is ModSearchState.VersionError
    OverlayBottomSheet(
        show = true,
        title = if (showingVersions) {
            stringResource(R.string.resourcepacks_versions_title)
        } else {
            stringResource(R.string.resourcepacks_search_title)
        },
        startAction = {
            TextButton(
                onClick = {
                    if (showingVersions) {
                        onAction(ResourcePacksAction.ModSearchBack)
                    } else {
                        onAction(ResourcePacksAction.ModSearchClose)
                    }
                },
                text = if (showingVersions) {
                    stringResource(R.string.resourcepacks_search_back)
                } else {
                    stringResource(R.string.resourcepacks_search_cancel)
                },
            )
        },
        onDismissRequest = { onAction(ResourcePacksAction.ModSearchClose) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!showingVersions) {
                SearchInput(
                    query = query,
                    isSearching = state is ModSearchState.Searching,
                    onQueryChange = {
                        query = it
                        onAction(ResourcePacksAction.ModSearchQueryChanged(it))
                    },
                    onSubmit = { onAction(ResourcePacksAction.ModSearchSubmit) },
                )
            }
            SearchBody(
                state = state,
                onAction = onAction,
                onBack = { onAction(ResourcePacksAction.ModSearchBack) },
            )
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            label = stringResource(R.string.resourcepacks_search_hint),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSubmit,
            enabled = !isSearching,
        ) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    size = 18.dp,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.resourcepacks_searching), maxLines = 1)
            } else {
                Icon(
                    imageVector = MiuixIcons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.action_search), maxLines = 1)
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
    when (state) {
        ModSearchState.Closed -> Unit
        ModSearchState.Searching -> Spacer(modifier = Modifier.height(16.dp))
        is ModSearchState.LoadingVersions -> SearchLoadingMessage(
            text = stringResource(R.string.resourcepacks_versions_loading, state.hit.title),
        )
        is ModSearchState.Results -> ResultsList(state = state, onAction = onAction)
        is ModSearchState.Versions -> VersionsList(state = state, onAction = onAction, onBack = onBack)
        is ModSearchState.VersionError -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SheetMessage(text = state.message, isError = true)
            TextButton(onClick = onBack, text = stringResource(R.string.resourcepacks_search_back))
        }
    }
}

@Composable
private fun ResultsList(state: ModSearchState.Results, onAction: (ResourcePacksAction) -> Unit) {
    if (state.query.isBlank()) {
        SheetMessage(text = stringResource(R.string.resourcepacks_search_submit_hint))
        return
    }
    if (state.hits.isEmpty()) {
        SheetMessage(text = stringResource(R.string.resourcepacks_search_empty))
        return
    }
    Text(
        text = stringResource(R.string.resourcepacks_search_result_count, state.hits.size),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
    )
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.hits, key = { it.projectId }) { hit ->
            SearchResultRow(
                title = hit.title,
                subtitle = hit.description.ifBlank { hit.slug },
                iconUrl = hit.iconUrl,
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = state.hit.title,
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
        )
        if (state.versions.isEmpty()) {
            SheetMessage(text = stringResource(R.string.resourcepacks_search_no_version), isError = true)
            TextButton(onClick = onBack, text = stringResource(R.string.resourcepacks_search_back))
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.versions, key = { it.id }) { version ->
                VersionRow(
                    version = version,
                    onClick = { onAction(ResourcePacksAction.ModSearchVersionSelected(version)) },
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    iconUrl: String? = null,
    leadingIcon: ImageVector = MiuixIcons.Search,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.42f)),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconUrl != null) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

@Composable
private fun VersionRow(version: ModVersionInfo, onClick: () -> Unit) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.42f)),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = MiuixIcons.Download,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = version.name,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Clip,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    val gameVersions = version.gameVersions.take(3).ifEmpty { listOf("-") }
                    gameVersions.forEach { gameVersion -> ModInfoChip(text = gameVersion) }
                    ModInfoChip(text = formatModSize(version.fileSize))
                }
                Text(
                    text = version.fileName,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

@Composable
private fun ModInfoChip(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.primary,
        style = MiuixTheme.textStyles.body2,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.11f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SearchLoadingMessage(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            size = 20.dp,
            strokeWidth = 2.5.dp,
        )
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatModSize(bytes: Long): String {
    if (bytes <= 0L) return "-"
    if (bytes < 1024L * 1024L) return "%.0f KB".format(bytes / 1024.0)
    return "%.1f MB".format(bytes / 1048576.0)
}

@Composable
private fun SheetMessage(text: String, isError: Boolean = false) {
    Text(
        text = text,
        color = if (isError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}
