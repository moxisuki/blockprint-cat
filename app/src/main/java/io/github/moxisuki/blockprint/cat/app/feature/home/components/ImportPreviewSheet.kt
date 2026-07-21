package io.github.moxisuki.blockprint.cat.app.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintFormat
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeImportPreview
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ImportPreviewSheet(
    show: Boolean,
    preview: HomeImportPreview?,
    loading: Boolean,
    importing: Boolean,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayBottomSheet(
        show = show,
        modifier = modifier,
        title = if (preview?.hasError == true) {
            stringResource(R.string.import_preview_failed_title)
        } else {
            stringResource(R.string.import_preview_title)
        },
        onDismissRequest = onDismissRequest,
        insideMargin = androidx.compose.ui.unit.DpSize(16.dp, 18.dp),
        defaultWindowInsetsPadding = false,
    ) {
        when {
            loading || preview == null -> ImportLoadingContent()
            preview.hasError -> ImportErrorContent(
                preview = preview,
                onDismissRequest = onDismissRequest,
            )
            else -> ImportPreviewContent(
                preview = preview,
                importing = importing,
                onConfirm = onConfirm,
                onDismissRequest = onDismissRequest,
            )
        }
    }
}

@Composable
private fun ImportLoadingContent() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            size = 22.dp,
            strokeWidth = 3.dp,
        )
        Text(
            text = stringResource(R.string.import_preview_loading),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

@Composable
private fun ImportErrorContent(
    preview: HomeImportPreview,
    onDismissRequest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = preview.fileName,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = preview.errorMessage.orEmpty(),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismissRequest,
            )
        }
    }
}

@Composable
private fun ImportPreviewContent(
    preview: HomeImportPreview,
    importing: Boolean,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(14.dp),
        ) {
            Text(
                text = preview.displayName,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preview.fileName,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.home_filter_label_format),
                value = preview.format.localizedLabel(),
            )
            InfoRow(
                label = stringResource(R.string.import_preview_author),
                value = preview.author.ifBlank { stringResource(R.string.import_preview_unknown) },
            )
            InfoRow(
                label = stringResource(R.string.import_preview_block_count),
                value = preview.blockCount,
            )
            InfoRow(
                label = stringResource(R.string.import_preview_regions),
                value = preview.regionCount.toString(),
            )
            InfoRow(
                label = stringResource(R.string.detail_meta_size),
                value = preview.size,
            )
        }
        Text(
            text = stringResource(R.string.import_preview_hint),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismissRequest,
                enabled = !importing,
            )
            Button(
                onClick = onConfirm,
                enabled = !importing,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                if (importing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        size = 18.dp,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.import_preview_confirm),
                        color = MiuixTheme.colorScheme.onPrimary,
                        style = MiuixTheme.textStyles.button,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            modifier = Modifier.weight(0.38f),
        )
        Text(
            text = value,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.62f),
        )
    }
}

@Composable
private fun HomeBlueprintFormat.localizedLabel(): String = when (this) {
    HomeBlueprintFormat.Litematica -> stringResource(R.string.format_short_litematica)
    HomeBlueprintFormat.Schematic -> stringResource(R.string.format_short_worldedit)
    HomeBlueprintFormat.Nbt -> stringResource(R.string.format_short_nbt)
    HomeBlueprintFormat.BuildingHelper -> stringResource(R.string.format_short_building_helper)
    HomeBlueprintFormat.Unknown -> stringResource(R.string.format_short_unknown)
}

@Preview(showBackground = true)
@Composable
private fun ImportPreviewSheetPreview() {
    PreviewAppTheme {
        ImportPreviewSheet(
            show = true,
            preview = HomeImportPreview(
                sourceUri = "content://preview",
                fileName = "factory_line.litematic",
                displayName = "Factory Line",
                format = HomeBlueprintFormat.Litematica,
                author = "moxisuki",
                blockCount = "12,480",
                regionCount = 2,
                size = "1.4 MB",
            ),
            loading = false,
            importing = false,
            onConfirm = {},
            onDismissRequest = {},
        )
    }
}
