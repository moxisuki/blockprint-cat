package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ActiveInstall
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ResourcePackId
import io.github.moxisuki.blockprint.cat.app.feature.resourcepacks.components.ModPackCard
import io.github.moxisuki.blockprint.cat.app.feature.resourcepacks.components.ResourcePackProgressBanner
import io.github.moxisuki.blockprint.cat.app.feature.resourcepacks.components.VanillaPackCard
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
internal fun ResourcePacksScreen(
    state: ResourcePacksState,
    onAction: (ResourcePacksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.resourcepacks_title),
                actions = {
                    TextButton(
                        onClick = { onAction(ResourcePacksAction.DeleteAllClicked) },
                        text = stringResource(R.string.resourcepacks_delete_all),
                    )
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 12.dp,
                bottom = inner.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.activeInstalls.forEach { (id, install) ->
                if (install is ActiveInstall) {
                    item(key = "active-${id.value}") {
                        ResourcePackProgressBanner(
                            install = install,
                            onCancel = { onAction(ResourcePacksAction.ModCancel(id)) },
                        )
                    }
                }
            }
            item(key = "vanilla-section") {
                Text(stringResource(R.string.resourcepacks_section_vanilla))
            }
            item(key = "vanilla-card") {
                val vanilla = state.installed.firstOrNull { it.id == ResourcePackId.Vanilla }
                VanillaPackCard(
                    entry = vanilla,
                    onDownload = { onAction(ResourcePacksAction.VanillaDownload) },
                    onRedownload = { onAction(ResourcePacksAction.VanillaRedownload) },
                    onDelete = { onAction(ResourcePacksAction.VanillaDelete) },
                    onCancel = { onAction(ResourcePacksAction.VanillaCancel) },
                )
            }
            item(key = "mod-section") {
                Text(stringResource(R.string.resourcepacks_section_mod))
            }
            val mods = state.installed.filter { !it.id.isVanilla }
            if (mods.isEmpty()) {
                item(key = "mod-empty") {
                    Text(stringResource(R.string.resourcepacks_mod_none))
                }
            } else {
                items(mods, key = { it.id.value }) { entry ->
                    ModPackCard(
                        entry = entry,
                        onRedownload = { onAction(ResourcePacksAction.ModRedownload(entry.id)) },
                        onDelete = { onAction(ResourcePacksAction.ModDelete(entry.id)) },
                    )
                }
            }
            item(key = "mod-add") {
                TextButton(
                    onClick = { onAction(ResourcePacksAction.ModSearchOpen) },
                    text = stringResource(R.string.resourcepacks_mod_add),
                )
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
