package io.github.moxisuki.blockprint.cat.app.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailActions
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailHeader
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailInfoSection
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialItem
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMaterialsSection
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailMissing
import io.github.moxisuki.blockprint.cat.app.feature.detail.components.BlueprintDetailStats
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintFormat
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BlueprintDetailScreen(
    state: BlueprintDetailState,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.cdl_loading),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
        return
    }

    val blueprint = state.blueprint
    if (blueprint == null) {
        BlueprintDetailMissing(
            modifier = modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
                .padding(16.dp),
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appScrollEndHaptic(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            BlueprintDetailHeader(blueprint = blueprint)
        }
        item(key = "actions") {
            BlueprintDetailActions()
        }
        item(key = "stats") {
            BlueprintDetailStats(blueprint = blueprint)
        }
        item(key = "info") {
            BlueprintDetailInfoSection(
                blueprint = blueprint,
                source = if (blueprint.id.startsWith("pc-")) {
                    stringResource(R.string.home_tab_pc)
                } else {
                    stringResource(R.string.home_tab_local)
                },
            )
        }
        item(key = "materials") {
            BlueprintDetailMaterialsSection(materials = state.materials)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlueprintDetailScreenPreview() {
    PreviewAppTheme {
        BlueprintDetailScreen(
            state = BlueprintDetailState(
                isLoading = false,
                blueprint = HomeBlueprintItem(
                    id = "preview",
                    name = "Cherry Courtyard",
                    fileName = "cherry_courtyard.litematic",
                    format = HomeBlueprintFormat.Litematica,
                    category = "Survival",
                    author = "moxisuki",
                    blockCount = "48,320",
                    regionCount = 3,
                    size = "2.4 MB",
                    updatedAt = "07-18 22:40",
                ),
                materials = listOf(
                    BlueprintDetailMaterialItem("minecraft:cherry_planks", 12840),
                    BlueprintDetailMaterialItem("minecraft:cherry_log", 6320),
                    BlueprintDetailMaterialItem("minecraft:stone_bricks", 4880),
                    BlueprintDetailMaterialItem("minecraft:lantern", 240),
                ),
            ),
        )
    }
}
