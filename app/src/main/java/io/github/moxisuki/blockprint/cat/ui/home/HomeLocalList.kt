package io.github.moxisuki.blockprint.cat.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta
import io.github.moxisuki.blockprint.cat.ui.format.FormatFilter
import io.github.moxisuki.blockprint.cat.ui.home.components.EmptyHomeState
import io.github.moxisuki.blockprint.cat.ui.home.components.HomeBlueprintCard
import io.github.moxisuki.blockprint.cat.ui.home.components.FormatChipFilter
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import kotlinx.coroutines.delay

private const val PAGE_SIZE = 15

/**
 * Lazy list of local blueprints used by the Local tab body.
 *
 * Implements three compose-side features:
 *   - 120ms search debounce so the filter pipeline doesn't churn on every
 *     keystroke (see [LaunchedEffect] around `filterQuery`)
 *   - `derivedStateOf` for `visibleBlueprints` + `hasMore` so the LazyColumn
 *     only sees a new list when the visible-window actually changed
 *   - Auto-load next page via a sentinel `LaunchedEffect(Unit)` inside the
 *     `load_more` lazy item — the cheap idiom for infinite scroll that
 *     doesn't need a side-effecting `produceState`
 *
 * @param allBlueprints     all on-disk blueprints (unfiltered)
 * @param visibleCount      lazy window size, ticks up by [PAGE_SIZE] on scroll
 * @param filterVisible / filterQuery / filterFormat  toolbar state — owned by
 *                          HomeScreen so they survive tab switches
 * @param onVisibleCountChange  caller needs the bump to write back so the
 *                          outer HomeScreen survives configuration changes
 * @param snackbarHostState forwarded to error toasts
 * @param onDeleteTarget / onRenameTarget / onUpload  callbacks dispatch into
 *                          the outer HomeScreen's dialog state machine
 */
@Composable
internal fun LocalBlueprintList(
    modifier: Modifier = Modifier,
    allBlueprints: List<BlueprintMeta>,
    scanning: Boolean,
    visibleCount: Int,
    onVisibleCountChange: (Int) -> Unit,
    safFolderName: String?,
    onRequestSafFolder: () -> Unit,
    navController: NavController,
    onBlueprintSelected: ((BlueprintMeta) -> Unit)?,
    onDeleteTarget: (BlueprintMeta) -> Unit,
    onRenameTarget: (BlueprintMeta) -> Unit,
    onUpload: (BlueprintMeta) -> Unit,
    bridgeConnected: Boolean,
    canTransfer: Boolean,
    snackbarHostState: SnackbarHostState,
    filterVisible: Boolean,
    filterQuery: String,
    onFilterQueryChange: (String) -> Unit,
    filterFormat: FormatFilter,
    onFilterFormatChange: (FormatFilter) -> Unit,
    onLongPress: (String) -> Unit = {},
    isSelected: (String) -> Boolean = { false },
) {
    // Debounce search query (avoid re-filtering on every keystroke)
    var debouncedQuery by remember { mutableStateOf("") }
    LaunchedEffect(filterQuery) {
        delay(120)
        debouncedQuery = filterQuery
    }

    val filtered = remember(allBlueprints, debouncedQuery, filterFormat) {
        val q = debouncedQuery.trim().lowercase()
        allBlueprints.filter { bp ->
            val matchesQuery = q.isEmpty() ||
                bp.displayName.lowercase().contains(q) ||
                bp.fileName.lowercase().contains(q)
            val matchesFormat = when (filterFormat) {
                FormatFilter.All -> true
                FormatFilter.Litematica -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Litematica
                FormatFilter.Schematic -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Sponge
                FormatFilter.Nbt -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Structure ||
                                     bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.PartialNbt ||
                                     bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.Unknown
                FormatFilter.Json -> bp.format == io.github.moxisuki.blockprint.core.SchematicFormat.BuildingHelper
            }
            matchesQuery && matchesFormat
        }
    }
    val visibleBlueprints by remember(filtered, visibleCount) {
        derivedStateOf { filtered.take(visibleCount) }
    }
    val hasMore by remember(filtered, visibleCount) {
        derivedStateOf { visibleCount < filtered.size }
    }

    if (scanning) {
        Column(
            modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.home_scanning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else if (allBlueprints.isEmpty()) {
        EmptyHomeState(
            onScanFolder = onRequestSafFolder,
            safFolderName = safFolderName,
            modifier = modifier.fillMaxSize(),
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = filterVisible,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(animationSpec = tween(280)) + fadeOut(animationSpec = tween(220)),
            ) {
                BlueprintFilterBar(
                    query = filterQuery,
                    onQueryChange = onFilterQueryChange,
                    selected = filterFormat,
                    onSelectedChange = onFilterFormatChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (visibleBlueprints.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.home_filter_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleBlueprints, key = { it.uuid }, contentType = { "bp" }) { bp ->
                        HomeBlueprintCard(
                            blueprint = bp,
                            onDetail = {
                                if (onBlueprintSelected != null) {
                                    onBlueprintSelected(bp)
                                } else {
                                    navController.navigate(NavRoutes.detailRoute(bp.uuid))
                                }
                            },
                            onDelete = { onDeleteTarget(bp) },
                            onRename = { onRenameTarget(bp) },
                            onUpload = { onUpload(bp) },
                            connected = bridgeConnected,
                            canTransfer = canTransfer,
                            selected = isSelected(bp.uuid),
                            onLongClick = { onLongPress(bp.uuid) },
                        )
                    }
                    if (hasMore) {
                        item(key = "load_more") {
                            LaunchedEffect(Unit) { onVisibleCountChange(visibleCount + PAGE_SIZE) }
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    item(key = "bottom_spacer") { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }
}

/**
 * Search field + format chip row that appears beneath the action bar when
 * the user taps the filter icon. `query` and `selected` are external state
 * owned by HomeScreen so they survive tab switches.
 */
@Composable
internal fun BlueprintFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selected: FormatFilter,
    onSelectedChange: (FormatFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    stringResource(R.string.home_filter_search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = if (query.isNotEmpty()) {{
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }} else null,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FormatChipFilter(
                label = stringResource(R.string.home_filter_format_all),
                selected = selected == FormatFilter.All,
            ) { onSelectedChange(FormatFilter.All) }
            FormatChipFilter(
                label = stringResource(R.string.format_filter_litematica),
                selected = selected == FormatFilter.Litematica,
            ) { onSelectedChange(FormatFilter.Litematica) }
            FormatChipFilter(
                label = stringResource(R.string.format_filter_schematic),
                selected = selected == FormatFilter.Schematic,
            ) { onSelectedChange(FormatFilter.Schematic) }
            FormatChipFilter(
                label = stringResource(R.string.format_filter_json),
                selected = selected == FormatFilter.Json,
            ) { onSelectedChange(FormatFilter.Json) }
            FormatChipFilter(
                label = stringResource(R.string.format_filter_nbt),
                selected = selected == FormatFilter.Nbt,
            ) { onSelectedChange(FormatFilter.Nbt) }
        }
    }
}
