package io.github.moxisuki.blockprint.cat.app.feature.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.feature.detail.BlueprintConversionState
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintFormat
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BlueprintConvertSheet(
    show: Boolean,
    currentFormat: HomeBlueprintFormat,
    state: BlueprintConversionState,
    onTargetSelected: (BlueprintFormat) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val targets = remember(currentFormat) {
        listOf(
            BlueprintFormat.Litematica,
            BlueprintFormat.Schematic,
            BlueprintFormat.Nbt,
        ).filterNot { it.toHomeFormat() == currentFormat }
    }

    OverlayBottomSheet(
        show = show,
        title = stringResource(R.string.detail_convert_dialog_title),
        onDismissRequest = onDismissRequest,
        insideMargin = DpSize(16.dp, 18.dp),
        defaultWindowInsetsPadding = false,
    ) {
        when (state) {
            BlueprintConversionState.Idle -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.detail_convert_dialog_message),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                    if (targets.isEmpty()) {
                        Text(
                            text = stringResource(R.string.detail_convert_no_target),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    } else {
                        targets.forEach { target ->
                            ArrowPreference(
                                title = target.label(),
                                summary = target.summary(),
                                onClick = { onTargetSelected(target) },
                            )
                        }
                    }
                }
            }
            is BlueprintConversionState.Running -> ConversionProgressContent()
            is BlueprintConversionState.Success -> ConversionSuccessContent(
                fileName = state.fileName,
                onDismiss = onDismissRequest,
            )
            is BlueprintConversionState.Failure -> ConversionFailureContent(
                message = state.message,
                onDismiss = onDismissRequest,
            )
        }
    }
}

@Composable
private fun ConversionProgressContent() {
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
            text = stringResource(R.string.detail_convert_running),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

@Composable
private fun ConversionSuccessContent(fileName: String, onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.detail_convert_success, fileName),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.body1,
        )
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(android.R.string.ok),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun ConversionFailureContent(message: String, onDismiss: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.detail_convert_failed, message),
            color = MiuixTheme.colorScheme.error,
            style = MiuixTheme.textStyles.body2,
        )
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(android.R.string.ok),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun BlueprintFormat.label(): String = when (this) {
    BlueprintFormat.Litematica -> stringResource(R.string.detail_convert_target_litematic)
    BlueprintFormat.Schematic -> stringResource(R.string.detail_convert_target_schem)
    BlueprintFormat.Nbt -> stringResource(R.string.detail_convert_target_nbt)
    BlueprintFormat.BuildingHelper -> stringResource(R.string.format_long_building_helper)
    BlueprintFormat.Unknown -> stringResource(R.string.format_long_unknown)
}

@Composable
private fun BlueprintFormat.summary(): String = when (this) {
    BlueprintFormat.Litematica -> stringResource(R.string.format_short_litematica)
    BlueprintFormat.Schematic -> stringResource(R.string.format_short_worldedit)
    BlueprintFormat.Nbt -> stringResource(R.string.format_short_nbt)
    BlueprintFormat.BuildingHelper -> stringResource(R.string.format_short_building_helper)
    BlueprintFormat.Unknown -> stringResource(R.string.format_short_unknown)
}

private fun BlueprintFormat.toHomeFormat(): HomeBlueprintFormat = when (this) {
    BlueprintFormat.Litematica -> HomeBlueprintFormat.Litematica
    BlueprintFormat.Schematic -> HomeBlueprintFormat.Schematic
    BlueprintFormat.Nbt -> HomeBlueprintFormat.Nbt
    BlueprintFormat.BuildingHelper -> HomeBlueprintFormat.BuildingHelper
    BlueprintFormat.Unknown -> HomeBlueprintFormat.Unknown
}
