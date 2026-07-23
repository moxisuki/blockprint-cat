package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.R
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
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ResourcePacksScreen(
    state: ResourcePacksState,
    onAction: (ResourcePacksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val mods = state.installed.filterNot { it.id.isVanilla }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "",
                largeTitle = stringResource(R.string.resourcepacks_title),
                navigationIcon = {
                    IconButton(onClick = { onAction(ResourcePacksAction.VanillaDelete) /* not used; back wired by AppShell */ }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "back",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(ResourcePacksAction.DeleteAllClicked) }) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = stringResource(R.string.resourcepacks_delete_all),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(ResourcePacksAction.ModSearchOpen) }) {
                Icon(
                    imageVector = MiuixIcons.Add,
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
                .appScrollEndHaptic(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
        ) {
            // ── Active install banners — staggered fade/expand entry. ──
            state.activeInstalls.forEach { (id, install) ->
                item(key = "active-${id.value}") {
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(id) { visible = true }
                    AnimatedVisibility(
                        visible = visible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        ActiveInstallBanner(
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            install = install,
                            onCancel = { onAction(ResourcePacksAction.ModCancel(id)) },
                        )
                    }
                }
            }

            // ── Vanilla ──
            item(key = "vanilla-section-header") {
                Text(
                    text = stringResource(R.string.resourcepacks_section_vanilla),
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
                )
            }
            item(key = "vanilla-card") {
                VanillaHeroCard(
                    entry = state.installed.firstOrNull { it.id == ResourcePackId.Vanilla },
                    onDownload = { onAction(ResourcePacksAction.VanillaDownload) },
                    onRedownload = { onAction(ResourcePacksAction.VanillaRedownload) },
                    onCancel = { onAction(ResourcePacksAction.VanillaCancel) },
                )
            }

            // ── Mods section ──
            item(key = "mods-header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.resourcepacks_section_mod),
                        color = MiuixTheme.colorScheme.onSurface,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (mods.isNotEmpty()) {
                        Text(
                            text = "${mods.size}",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                }
            }
            if (mods.isEmpty()) {
                item(key = "mods-empty") {
                    Text(
                        text = stringResource(R.string.resourcepacks_mod_none),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(items = mods, key = { it.id.value }) { entry ->
                    ModRow(
                        modifier = Modifier.padding(top = 8.dp),
                        entry = entry,
                        onRedownload = { onAction(ResourcePacksAction.ModRedownload(entry.id)) },
                        onDelete = { onAction(ResourcePacksAction.ModDelete(entry.id)) },
                    )
                }
            }

            // A small breathing room before the FAB.
            item(key = "tail-spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    ModSearchSheet(state = state.modSearch, onAction = onAction)

    DeleteAllConfirmDialog(
        visible = state.isDeleteAllConfirmVisible,
        onConfirm = { onAction(ResourcePacksAction.DeleteAllConfirm) },
        onDismiss = { onAction(ResourcePacksAction.DeleteAllDismissed) },
    )
}

@Composable
private fun VanillaHeroCard(
    entry: ResourcePackEntry?,
    onDownload: () -> Unit,
    onRedownload: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(horizontal = 22.dp, vertical = 22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VanillaIcon()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry?.displayName ?: "Minecraft 原版",
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = if (entry != null) {
                    stringResource(
                        R.string.resourcepacks_vanilla_installed,
                        entry.version, entry.fileCount, formatBytes(entry.totalSize),
                    )
                } else {
                    stringResource(R.string.resourcepacks_vanilla_not_installed)
                }
                Text(
                    text = subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = if (entry != null) onRedownload else onDownload,
        ) {
            Text(
                text = if (entry != null) {
                    stringResource(R.string.render_redownload)
                } else {
                    stringResource(R.string.render_check_update)
                },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCancel,
            text = stringResource(android.R.string.cancel),
        )
    }
}

@Composable
private fun VanillaIcon() {
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Base block (dirt-ish surface container).
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer),
        )
        // Top grass band — saturated primary for contrast.
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 14.dp)
                .background(MiuixTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ModRow(
    entry: ResourcePackEntry,
    onRedownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModIcon(slug = entry.id.modSlug)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.displayName,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (entry.fileCount == 0) {
                        stringResource(R.string.resourcepacks_mod_empty_pack)
                    } else {
                        "v${entry.version} · ${entry.fileCount} files · ${entry.namespaces.joinToString(",")}"
                    },
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(modifier = Modifier.weight(1f), onClick = onRedownload, text = stringResource(R.string.render_redownload))
            TextButton(modifier = Modifier.weight(1f), onClick = onDelete, text = stringResource(R.string.action_delete))
        }
    }
}

@Composable
private fun ModIcon(slug: String) {
    val initial = slug.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.4f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ActiveInstallBanner(
    install: ActiveInstall,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by install.progress.collectAsStateWithLifecycle(initialValue = PackProgress.Idle)
    val determinate: Float? = when (val p = progress) {
        is PackProgress.Downloading -> p.fraction
        is PackProgress.Extracting -> 0.99f
        else -> null
    }
    val animatedFraction by animateFloatAsState(
        targetValue = (determinate ?: 0f).coerceIn(0f, 1f),
        label = "installProgress",
    )
    val statusText = when (val p = progress) {
        is PackProgress.Downloading -> stringResource(
            R.string.resourcepacks_progress_installing,
            p.fileName,
            (p.fraction * 100).toInt(),
        )
        is PackProgress.Extracting -> stringResource(R.string.resourcepacks_progress_extracting)
        is PackProgress.FetchingManifest -> p.label
        is PackProgress.Failed -> stringResource(R.string.resourcepacks_failed, p.message)
        else -> ""
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = install.displayName,
                    color = MiuixTheme.colorScheme.onSurface,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (statusText.isNotEmpty()) {
                    Text(
                        text = statusText,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Always show a progress bar; null = indeterminate.
                LinearProgressIndicator(
                    progress = determinate?.let { animatedFraction },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = MiuixIcons.Close,
                    contentDescription = stringResource(android.R.string.cancel),
                )
            }
        }
    }
}

@Composable
private fun DeleteAllConfirmDialog(visible: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    OverlayDialog(
        show = visible,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.resourcepacks_delete_all),
        summary = "",
    ) {
        TextButton(onClick = onConfirm, text = stringResource(android.R.string.ok))
        TextButton(onClick = onDismiss, text = stringResource(android.R.string.cancel))
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes.toDouble()
    var i = 0
    while (value >= 1024.0 && i < units.lastIndex) { value /= 1024.0; i++ }
    return "%.1f %s".format(value, units[i])
}
