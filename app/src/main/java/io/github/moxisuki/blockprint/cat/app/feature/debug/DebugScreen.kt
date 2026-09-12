package io.github.moxisuki.blockprint.cat.app.feature.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DebugScreen(
    state: DebugState,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appMaxContentWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        item(key = "app-title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.debug_section_app),
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                )
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = stringResource(R.string.debug_copy),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onCopy() },
                )
            }
        }
        item(key = "app") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                Column {
                    InfoRow(label = stringResource(R.string.about_label_version), value = state.appVersion)
                    SectionDivider()
                    InfoRow(label = stringResource(R.string.debug_label_abi), value = state.abi)
                    SectionDivider()
                    InfoRow(label = stringResource(R.string.debug_label_build_type), value = state.buildType)
                    SectionDivider()
                    InfoRow(label = stringResource(R.string.about_label_package), value = state.applicationId)
                    SectionDivider()
                    InfoRow(label = stringResource(R.string.about_engine_label), value = state.blockPrintCoreVersion)
                    SectionDivider()
                    InfoRow(label = "Miuix", value = state.miuixVersion)
                    SectionDivider()
                    InfoRow(label = "Kotlin", value = state.kotlinVersion)
                    SectionDivider()
                    InfoRow(label = "Compose BOM", value = state.composeBomVersion)
                }
            }
        }

        item(key = "device-title") {
            SectionTitle(
                text = stringResource(R.string.debug_section_device),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "device") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                Column {
                    InfoRow(label = stringResource(R.string.debug_label_model), value = state.deviceModel)
                    SectionDivider()
                    InfoRow(label = stringResource(R.string.debug_label_manufacturer), value = state.deviceManufacturer)
                    SectionDivider()
                    InfoRow(label = stringResource(R.string.debug_label_android), value = "${state.androidVersion} (SDK ${state.sdkInt})")
                    SectionDivider()
                    InfoRow(label = stringResource(R.string.debug_label_density), value = "${state.densityDpi} dpi")
                }
            }
        }

        item(key = "memory-title") {
            SectionTitle(
                text = stringResource(R.string.debug_section_memory),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "memory") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                Column {
                    InfoRow(label = stringResource(R.string.debug_label_total_ram), value = "${state.totalRamMb} MB")
                    SectionDivider()
                    InfoRow(label = stringResource(R.string.debug_label_heap_limit), value = "${state.heapLimitMb} MB")
                    SectionDivider()
                    InfoRow(label = stringResource(R.string.debug_label_heap_max), value = "${state.heapMaxMb} MB")
                    SectionDivider()
                    InfoRow(
                        label = stringResource(R.string.debug_label_app_storage),
                        value = if (state.isAppStorageLoading) {
                            stringResource(R.string.cache_calculating)
                        } else {
                            "${state.appStorageMb} MB"
                        },
                    )
                }
            }
        }

        item(key = "bottom-space") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        text = text,
        color = MiuixTheme.colorScheme.primary,
        style = MiuixTheme.textStyles.body2,
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body1,
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
}

@Preview(showBackground = true)
@Composable
private fun DebugScreenPreview() {
    PreviewAppTheme {
        DebugScreen(state = DebugState(), onCopy = {})
    }
}
