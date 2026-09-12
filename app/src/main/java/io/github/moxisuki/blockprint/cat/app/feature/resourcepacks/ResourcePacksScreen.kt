package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ActiveInstall
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.PackProgress
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackEntry
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.FileDownloads
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ResourcePacksScreen(
    state: ResourcePacksState,
    onAction: (ResourcePacksAction) -> Unit,
    onAppBarTitleVisibleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val titleThresholdPx = with(LocalDensity.current) { 44.dp.toPx() }
    val showAppBarTitle by remember(listState, titleThresholdPx) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > titleThresholdPx
        }
    }
    LaunchedEffect(showAppBarTitle) {
        onAppBarTitleVisibleChange(showAppBarTitle)
    }

    val mods = remember(state.installed) { state.installed.filterNot { it.id.isVanilla } }
    val vanilla = remember(state.installed) { state.installed.firstOrNull { it.id == ResourcePackId.Vanilla } }
    val activeInstalls = remember(state.activeInstalls) {
        state.activeInstalls.values.sortedBy { it.packId.value }
    }
    val metrics = remember(state.installed, mods) {
        ResourcePackMetrics(
            installedCount = state.installed.size,
            modCount = mods.size,
            totalSize = state.installed.sumOf { it.totalSize },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {},
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(ResourcePacksAction.ModSearchOpen()) }) {
                Icon(
                    imageVector = MiuixIcons.Store,
                    contentDescription = stringResource(R.string.resourcepacks_mod_add),
                    tint = MiuixTheme.colorScheme.onPrimary,
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
                .appScrollEndHaptic()
                .appMaxContentWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = innerPadding.calculateTopPadding() + 18.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
        ) {
            item(key = "summary") {
                ResourceSummaryPanel(
                    metrics = metrics,
                    onDeleteAll = { onAction(ResourcePacksAction.DeleteAllClicked) },
                )
            }

            state.feedback?.let { feedback ->
                item(key = "feedback") {
                    FeedbackCard(
                        feedback = feedback,
                        onDismiss = { onAction(ResourcePacksAction.FeedbackDismissed) },
                    )
                }
            }

            if (activeInstalls.isNotEmpty()) {
                item(key = "active-title") {
                    SectionTitle(R.string.resourcepacks_progress_preparing)
                }
                items(items = activeInstalls, key = { "active-${it.packId.value}" }) { install ->
                    ActiveInstallBanner(
                        install = install,
                        onCancel = {
                            if (install.packId.isVanilla) {
                                onAction(ResourcePacksAction.VanillaCancel)
                            } else {
                                onAction(ResourcePacksAction.ModCancel(install.packId))
                            }
                        },
                        onRetry = {
                            if (install.packId.isVanilla) {
                                onAction(ResourcePacksAction.VanillaRedownload)
                            } else {
                                onAction(ResourcePacksAction.ModRedownload(install.packId))
                            }
                        },
                    )
                }
            }

            item(key = "vanilla-section") {
                SectionTitle(R.string.resourcepacks_section_vanilla)
            }
            item(key = "vanilla-card") {
                VanillaPackItem(
                    entry = vanilla,
                    isInstalling = state.activeInstalls.containsKey(ResourcePackId.Vanilla),
                    onDownload = { onAction(ResourcePacksAction.VanillaDownload) },
                    onRedownload = { onAction(ResourcePacksAction.VanillaRedownload) },
                    onDelete = { onAction(ResourcePacksAction.VanillaDelete) },
                    onCancel = { onAction(ResourcePacksAction.VanillaCancel) },
                )
            }

            item(key = "mods-section") {
                SectionHeaderWithCount(
                    titleRes = R.string.resourcepacks_section_mod,
                    count = mods.size,
                )
            }
            if (mods.isEmpty()) {
                item(key = "mods-empty") {
                    EmptyModsCard(onAdd = { onAction(ResourcePacksAction.ModSearchOpen()) })
                }
            } else {
                items(items = mods, key = { it.id.value }) { entry ->
                    ModPackItem(
                        entry = entry,
                        onRedownload = { onAction(ResourcePacksAction.ModRedownload(entry.id)) },
                        onDelete = { onAction(ResourcePacksAction.ModDelete(entry.id)) },
                    )
                }
            }

            item(key = "tail-spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    ModSearchSheet(state = state.modSearch, onAction = onAction)
    PendingDeleteConfirmDialog(
        pendingDelete = state.pendingDelete,
        onConfirm = { onAction(ResourcePacksAction.PendingDeleteConfirm) },
        onDismiss = { onAction(ResourcePacksAction.PendingDeleteDismiss) },
    )
    DeleteAllConfirmDialog(
        visible = state.isDeleteAllConfirmVisible,
        onConfirm = { onAction(ResourcePacksAction.DeleteAllConfirm) },
        onDismiss = { onAction(ResourcePacksAction.DeleteAllDismissed) },
    )
}

@Composable
private fun ResourceSummaryPanel(metrics: ResourcePackMetrics, onDeleteAll: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            HeroResourceMark()
            Column(
                modifier = Modifier.weight(1f).padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = stringResource(R.string.resourcepacks_title),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.resourcepacks_subtitle),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDeleteAll) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = stringResource(R.string.resourcepacks_delete_all),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeroMetric(
                value = metrics.installedCount.toString(),
                label = stringResource(R.string.resourcepacks_metric_installed),
                modifier = Modifier.weight(1f),
            )
            HeroMetric(
                value = metrics.modCount.toString(),
                label = stringResource(R.string.resourcepacks_metric_mods),
                modifier = Modifier.weight(1f),
            )
            if (metrics.totalSize > 0L) {
                HeroMetric(
                    value = formatBytes(metrics.totalSize),
                    label = stringResource(R.string.resourcepacks_metric_size),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroResourceMark() {
    val primary = MiuixTheme.colorScheme.primary
    val secondary = MiuixTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(primary.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(9.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(Modifier.size(9.dp, 19.dp).clip(RoundedCornerShape(3.dp)).background(primary.copy(alpha = 0.42f)))
            Box(Modifier.size(9.dp, 28.dp).clip(RoundedCornerShape(3.dp)).background(primary.copy(alpha = 0.72f)))
            Box(Modifier.size(9.dp, 14.dp).clip(RoundedCornerShape(3.dp)).background(secondary.copy(alpha = 0.68f)))
        }
        Icon(
            imageVector = MiuixIcons.GridView,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun HeroMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.48f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionTitle(resourceId: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(resourceId),
        color = MiuixTheme.colorScheme.onSurface,
        style = MiuixTheme.textStyles.body1,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun SectionHeaderWithCount(titleRes: Int, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(titleRes, modifier = Modifier.weight(1f).padding(top = 0.dp, bottom = 0.dp))
        StatusChip(text = count.toString(), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun StatusChip(text: String, color: Color = MiuixTheme.colorScheme.primary) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            color = color,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun FeedbackCard(feedback: ResourcePacksFeedback, onDismiss: () -> Unit) {
    val isError = feedback is ResourcePacksFeedback.Error
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (feedback) {
                    is ResourcePacksFeedback.Info -> feedback.message
                    is ResourcePacksFeedback.Error -> feedback.message
                },
                modifier = Modifier.weight(1f),
                color = if (isError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body2,
            )
            IconButton(onClick = onDismiss) {
                Icon(MiuixIcons.Close, contentDescription = stringResource(R.string.action_dismiss))
            }
        }
    }
}

@Composable
private fun VanillaPackItem(
    entry: ResourcePackEntry?,
    isInstalling: Boolean,
    onDownload: () -> Unit,
    onRedownload: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    ResourcePackCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VanillaIcon(isInstalled = entry != null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.resourcepacks_vanilla_name),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (entry != null) {
                        stringResource(
                            R.string.resourcepacks_vanilla_installed,
                            entry.version,
                            entry.fileCount,
                            formatBytes(entry.totalSize),
                        )
                    } else {
                        stringResource(R.string.resourcepacks_vanilla_not_installed)
                    },
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = MiuixIcons.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        if (isInstalling) {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCancel,
                text = stringResource(android.R.string.cancel),
            )
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = if (entry != null) onRedownload else onDownload,
            ) {
                Text(if (entry != null) stringResource(R.string.render_redownload) else stringResource(R.string.render_check_update))
            }
        }
    }
}

@Composable
private fun ModPackItem(entry: ResourcePackEntry, onRedownload: () -> Unit, onDelete: () -> Unit) {
    ResourcePackCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModIcon(entry.id.modSlug)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = entry.displayName,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = modDetails(entry),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = MiuixIcons.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onRedownload,
                text = stringResource(R.string.render_redownload),
            )
        }
    }
}

@Composable
private fun ResourcePackCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

@Composable
private fun EmptyModsCard(onAdd: () -> Unit) {
    ResourcePackCard {
        Text(
            text = stringResource(R.string.resourcepacks_mod_none),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        TextButton(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            onClick = onAdd,
            text = stringResource(R.string.resourcepacks_mod_add),
        )
    }
}

@Composable
private fun VanillaIcon(isInstalled: Boolean) {
    val iconColor = if (isInstalled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(iconColor.copy(alpha = if (isInstalled) 0.14f else 0.08f))
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.24f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.Layers,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun ModIcon(slug: String) {
    val initial = slug.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.secondary.copy(alpha = 0.14f))
            .border(1.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.28f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.FileDownloads,
            contentDescription = initial,
            tint = MiuixTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ActiveInstallBanner(
    install: ActiveInstall,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
) {
    val progress by install.progress.collectAsStateWithLifecycle(initialValue = PackProgress.Idle)
    val snapshot = taskProgressSnapshot(progress)
    val animatedOverall by animateFloatAsState(
        targetValue = snapshot.overallFraction ?: 0f,
        label = "resourceInstallProgress",
    )
    val isFailure = progress is PackProgress.Failed

    ResourcePackCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = install.displayName,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = snapshot.statusText,
                    color = if (isFailure) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ProgressLine(
                    label = stringResource(R.string.resourcepacks_task_progress),
                    progress = snapshot.overallFraction?.let { animatedOverall },
                    detail = snapshot.progressDetail,
                )
                if (isFailure) {
                    TextButton(onClick = onRetry, text = stringResource(R.string.action_retry))
                }
            }
            if (!isFailure && progress !is PackProgress.Done && progress !is PackProgress.Cancelled) {
                IconButton(onClick = onCancel) {
                    Icon(MiuixIcons.Close, contentDescription = stringResource(android.R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(label: String, progress: Float?, detail: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
            detail?.let {
                Text(
                    text = it,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun taskProgressSnapshot(progress: PackProgress): ResourceTaskProgressSnapshot =
    when (progress) {
        PackProgress.Idle -> ResourceTaskProgressSnapshot(
            statusText = stringResource(R.string.resourcepacks_progress_preparing),
            overallFraction = null,
        )
        is PackProgress.Preparing -> ResourceTaskProgressSnapshot(
            statusText = progress.label,
            overallFraction = null,
        )
        is PackProgress.FetchingManifest -> ResourceTaskProgressSnapshot(
            statusText = progress.label,
            overallFraction = null,
        )
        is PackProgress.Downloading -> {
            val fraction = progress.fraction.coerceIn(0f, 1f)
            ResourceTaskProgressSnapshot(
                statusText = stringResource(
                    R.string.resourcepacks_progress_installing,
                    progress.fileName,
                    (fraction * 100).toInt(),
                ),
                overallFraction = fraction * DOWNLOAD_WEIGHT,
                progressDetail = if (progress.bytesRead >= 0L && progress.totalBytes > 0L) {
                    "${formatBytes(progress.bytesRead)} / ${formatBytes(progress.totalBytes)}"
                } else {
                    "${(fraction * 100).toInt()}%"
                },
            )
        }
        is PackProgress.Installing -> ResourceTaskProgressSnapshot(
            statusText = stringResource(
                R.string.resourcepacks_progress_extracting_files,
                progress.installedFiles,
                progress.label,
            ),
            overallFraction = DOWNLOAD_WEIGHT + (progress.fraction?.coerceIn(0f, 1f) ?: 0f) * INSTALL_WEIGHT,
            progressDetail = progress.totalFiles?.let { total -> "${progress.installedFiles} / $total" }
                ?: progress.installedFiles.takeIf { it > 0 }?.toString(),
        )
        is PackProgress.Extracting -> ResourceTaskProgressSnapshot(
            statusText = stringResource(
                R.string.resourcepacks_progress_extracting_files,
                progress.extracted,
                progress.currentPath,
            ),
            overallFraction = DOWNLOAD_WEIGHT,
            progressDetail = progress.extracted.toString(),
        )
        is PackProgress.Failed -> ResourceTaskProgressSnapshot(
            statusText = stringResource(R.string.resourcepacks_failed, progress.message),
            overallFraction = null,
        )
        PackProgress.Cancelled -> ResourceTaskProgressSnapshot(
            statusText = stringResource(R.string.resourcepacks_progress_cancelled),
            overallFraction = null,
        )
        is PackProgress.Done -> ResourceTaskProgressSnapshot(
            statusText = stringResource(R.string.resourcepacks_progress_done),
            overallFraction = 1f,
            progressDetail = progress.entry.fileCount.toString(),
        )
    }

@Composable
private fun PendingDeleteConfirmDialog(
    pendingDelete: PendingResourcePackDelete?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayDialog(
        show = pendingDelete != null,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.resourcepacks_confirm_delete),
        summary = pendingDelete?.displayName.orEmpty(),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(modifier = Modifier.weight(1f), onClick = onDismiss, text = stringResource(android.R.string.cancel))
            TextButton(modifier = Modifier.weight(1f), onClick = onConfirm, text = stringResource(android.R.string.ok))
        }
    }
}

@Composable
private fun DeleteAllConfirmDialog(visible: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    OverlayDialog(
        show = visible,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.resourcepacks_delete_all),
        summary = stringResource(R.string.resourcepacks_subtitle),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(modifier = Modifier.weight(1f), onClick = onDismiss, text = stringResource(android.R.string.cancel))
            TextButton(modifier = Modifier.weight(1f), onClick = onConfirm, text = stringResource(android.R.string.ok))
        }
    }
}

@Composable
private fun modDetails(entry: ResourcePackEntry): String =
    if (entry.fileCount == 0) {
        stringResource(R.string.resourcepacks_mod_empty_pack)
    } else {
        stringResource(
            R.string.resourcepacks_mod_details,
            entry.version,
            entry.fileCount,
            entry.namespaces.joinToString(", "),
        )
    }

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes.toDouble() / 1024.0
    var i = 0
    while (value >= 1024.0 && i < units.lastIndex) {
        value /= 1024.0
        i++
    }
    return "%.1f %s".format(value, units[i])
}

@Immutable
private data class ResourcePackMetrics(
    val installedCount: Int,
    val modCount: Int,
    val totalSize: Long,
)

@Immutable
private data class ResourceTaskProgressSnapshot(
    val statusText: String,
    val overallFraction: Float?,
    val progressDetail: String? = null,
)

private const val DOWNLOAD_WEIGHT = 0.55f
private const val INSTALL_WEIGHT = 0.45f
