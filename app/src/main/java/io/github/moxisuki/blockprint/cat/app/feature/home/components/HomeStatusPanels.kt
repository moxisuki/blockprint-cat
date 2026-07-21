package io.github.moxisuki.blockprint.cat.app.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeBlueprintSource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SafDirectoryRequiredPanel(
    onPickSafDirectory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_saf_required_title),
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_saf_required_summary),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onPickSafDirectory,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = stringResource(R.string.home_pick_folder),
                    color = MiuixTheme.colorScheme.onPrimary,
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}

@Composable
internal fun EmptyBlueprintPanel(
    source: HomeBlueprintSource,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(20.dp),
    ) {
        Text(
            text = when (source) {
                HomeBlueprintSource.Local -> stringResource(R.string.home_empty_title)
                HomeBlueprintSource.Pc -> stringResource(R.string.home_pc_empty_title)
            },
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.title3,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when (source) {
                HomeBlueprintSource.Local -> stringResource(R.string.home_empty_with_folder)
                HomeBlueprintSource.Pc -> stringResource(R.string.home_pc_empty_hint)
            },
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        if (source == HomeBlueprintSource.Local) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EmptyActionButton(
                    text = stringResource(R.string.home_empty_action_pc),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                EmptyActionButton(
                    text = stringResource(R.string.home_empty_action_import),
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f),
                )
                EmptyActionButton(
                    text = stringResource(R.string.home_empty_action_community),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun EmptyActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSecondaryVariant,
            style = MiuixTheme.textStyles.button,
            maxLines = 1,
        )
    }
}

@Composable
internal fun CategoryEmptyPanel(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(20.dp),
    ) {
        Text(
            text = stringResource(R.string.home_category_empty_title),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.title3,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_category_empty_hint),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
internal fun SearchEmptyPanel(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(20.dp),
    ) {
        Text(
            text = stringResource(R.string.home_search_empty_title),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.title3,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_search_empty_hint),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
    }
}
