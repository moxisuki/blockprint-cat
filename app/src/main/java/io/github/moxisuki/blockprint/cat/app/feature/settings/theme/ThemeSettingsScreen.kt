package io.github.moxisuki.blockprint.cat.app.feature.settings.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.AppDefaultThemeSeedColor
import io.github.moxisuki.blockprint.cat.app.core.design.AppMotion
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeAccentColor
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeColorSource
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeMode
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeState
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ThemeSettingsScreen(
    state: AppThemeState,
    onAppBarTitleVisibleChange: (Boolean) -> Unit,
    onModeSelected: (AppThemeMode) -> Unit,
    onMonetEnabledChange: (Boolean) -> Unit,
    onAccentColorSelected: (AppThemeAccentColor) -> Unit,
    onCustomColorEnabledChange: (Boolean) -> Unit,
    onSeedColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val titleThresholdPx = with(LocalDensity.current) { 44.dp.roundToPx() }
    val showAppBarTitle by remember(listState, titleThresholdPx) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > titleThresholdPx
        }
    }
    LaunchedEffect(showAppBarTitle) {
        onAppBarTitleVisibleChange(showAppBarTitle)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appScrollEndHaptic()
            .appMaxContentWidth(),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 8.dp,
            end = 20.dp,
            bottom = 36.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "title") {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = stringResource(R.string.theme_settings_title),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item(key = "preview") {
            HomeThemePreview(
                modifier = Modifier
                    .padding(top = 36.dp)
                    .width(216.dp),
            )
        }
        item(key = "mode-control") {
            ThemeModeSegmentedControl(
                selectedMode = state.mode,
                onModeSelected = onModeSelected,
                modifier = Modifier.padding(top = 48.dp),
            )
        }
        item(key = "dynamic-color") {
            ThemeOptionCard(
                modifier = Modifier.padding(top = 18.dp),
            ) {
                ThemeSwitchRow(
                    marker = {
                        ThemeIconFrame {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        width = 2.dp,
                                        color = MiuixTheme.colorScheme.onSurface,
                                        shape = RoundedCornerShape(4.dp),
                                    ),
                            )
                        }
                    },
                    title = stringResource(R.string.theme_monet_enable),
                    summary = null,
                    checked = state.colorSource == AppThemeColorSource.Monet,
                    onCheckedChange = onMonetEnabledChange,
                )
                AnimatedVisibility(
                    visible = state.colorSource == AppThemeColorSource.Monet,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(14.dp))
                        AccentColorSpinner(
                            selectedAccent = state.accentColor,
                            onAccentColorSelected = onAccentColorSelected,
                        )
                    }
                }
            }
        }
        item(key = "custom-color") {
            ThemeOptionCard(
                modifier = Modifier.padding(top = 12.dp),
            ) {
                ThemeSwitchRow(
                    marker = {
                        ThemeColorDot(color = state.seedColor)
                    },
                    title = stringResource(R.string.theme_custom_color_enable),
                    summary = stringResource(R.string.theme_palette_selected, state.seedColor.toHexRgb()),
                    checked = state.colorSource == AppThemeColorSource.Custom,
                    onCheckedChange = onCustomColorEnabledChange,
                )
                AnimatedVisibility(
                    visible = state.colorSource == AppThemeColorSource.Custom,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(18.dp))
                        ColorPalette(
                            color = state.seedColor,
                            onColorChanged = onSeedColorChanged,
                            showPreview = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeThemePreview(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .aspectRatio(0.58f)
            .clip(RoundedCornerShape(26.dp))
            .background(MiuixTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = RoundedCornerShape(26.dp),
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        HomePreviewToolbar()
        HomePreviewCategoryStrip()
        HomePreviewBlueprintCard(
            selected = true,
        )
        HomePreviewBlueprintCard(
            selected = false,
        )
    }
}

@Composable
private fun HomePreviewToolbar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .padding(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MiuixTheme.colorScheme.primary),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PreviewTabBlock(
                    selected = true,
                    modifier = Modifier.weight(1f),
                )
                PreviewTabBlock(
                    selected = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == 1) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.surfaceContainer
                        },
                    ),
            )
        }
    }
}

@Composable
private fun PreviewTabBlock(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(
                if (selected) {
                    MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.72f)
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.26f)
                },
            ),
    )
}

@Composable
private fun HomePreviewCategoryStrip() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PreviewCategoryChip(width = 42.dp, selected = true)
        PreviewCategoryChip(width = 54.dp, selected = false)
        PreviewCategoryChip(width = 48.dp, selected = false)
    }
}

@Composable
private fun PreviewCategoryChip(width: androidx.compose.ui.unit.Dp, selected: Boolean) {
    Box(
        modifier = Modifier
            .width(width)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun HomePreviewBlueprintCard(
    selected: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        insideMargin = PaddingValues(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                PreviewLineBlock(
                    widthFraction = if (selected) 0.78f else 0.64f,
                    height = 9.dp,
                    color = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.30f),
                )
                Spacer(modifier = Modifier.height(6.dp))
                PreviewLineBlock(
                    widthFraction = if (selected) 0.58f else 0.70f,
                    height = 7.dp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.22f),
                )
            }
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            PreviewMetricBadge(width = 38.dp, primary = true)
            PreviewMetricBadge(width = 30.dp, primary = false)
            PreviewMetricBadge(width = 34.dp, primary = false)
        }
    }
}

@Composable
private fun PreviewLineBlock(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(color),
    )
}

@Composable
private fun PreviewMetricBadge(width: androidx.compose.ui.unit.Dp, primary: Boolean) {
    Box(
        modifier = Modifier
            .width(width)
            .height(20.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(
                if (primary) {
                    MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
            )
            .padding(horizontal = 7.dp, vertical = 7.dp),
    )
}

@Composable
private fun ThemeModeSegmentedControl(
    selectedMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = remember { AppThemeMode.entries.toList() }
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = AppMotion.topLevelOffsetSpec(),
        label = "themeModeIndicatorOffset",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
            .padding(4.dp),
    ) {
        val itemWidth = maxWidth / modes.size
        Box(
            modifier = Modifier
                .offset(x = itemWidth * animatedIndex)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(MiuixTheme.colorScheme.surface),
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        modes.forEach { mode ->
            val selected = mode == selectedMode
            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onModeSelected(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = mode.label(),
                    color = if (selected) {
                        MiuixTheme.colorScheme.onSurface
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        insideMargin = PaddingValues(horizontal = 26.dp, vertical = 22.dp),
    ) {
        content()
    }
}

@Composable
private fun AccentColorSpinner(
    selectedAccent: AppThemeAccentColor,
    onAccentColorSelected: (AppThemeAccentColor) -> Unit,
) {
    val accents = remember { AppThemeAccentColor.entries.toList() }
    OverlaySpinnerPreference(
        modifier = Modifier.fillMaxWidth(),
        items = accents.map { accent ->
            DropdownItem(text = accent.label())
        },
        selectedIndex = accents.indexOf(selectedAccent).coerceAtLeast(0),
        title = stringResource(R.string.theme_accent_color),
        showValue = true,
        startAction = {
            ThemeColorDot(
                color = selectedAccent.color ?: MiuixTheme.colorScheme.primary,
            )
        },
        insideMargin = PaddingValues(0.dp),
        onSelectedIndexChange = { index ->
            onAccentColorSelected(accents[index])
        },
    )
}

@Composable
private fun ThemeSwitchRow(
    marker: @Composable () -> Unit,
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        marker()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = summary,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun ThemeIconFrame(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun ThemeColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline.copy(alpha = 0.45f),
                shape = CircleShape,
            ),
    )
}

@Composable
private fun AppThemeMode.label(): String = when (this) {
    AppThemeMode.System -> stringResource(R.string.theme_mode_system)
    AppThemeMode.Light -> stringResource(R.string.theme_mode_light)
    AppThemeMode.Dark -> stringResource(R.string.theme_mode_dark)
}

@Composable
private fun AppThemeAccentColor.label(): String = when (this) {
    AppThemeAccentColor.Default -> stringResource(R.string.theme_accent_default)
    AppThemeAccentColor.Red -> stringResource(R.string.theme_accent_red)
    AppThemeAccentColor.Pink -> stringResource(R.string.theme_accent_pink)
    AppThemeAccentColor.Purple -> stringResource(R.string.theme_accent_purple)
    AppThemeAccentColor.DeepPurple -> stringResource(R.string.theme_accent_deep_purple)
    AppThemeAccentColor.Indigo -> stringResource(R.string.theme_accent_indigo)
    AppThemeAccentColor.Blue -> stringResource(R.string.theme_accent_blue)
    AppThemeAccentColor.Cyan -> stringResource(R.string.theme_accent_cyan)
}

private fun Color.toHexRgb(): String =
    "#${(toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')}"

@Preview(showBackground = true)
@Composable
private fun ThemeSettingsScreenPreview() {
    PreviewAppTheme {
        ThemeSettingsScreen(
            state = AppThemeState(
                mode = AppThemeMode.System,
                colorSource = AppThemeColorSource.Custom,
                seedColor = AppDefaultThemeSeedColor,
            ),
            onAppBarTitleVisibleChange = {},
            onModeSelected = {},
            onMonetEnabledChange = {},
            onAccentColorSelected = {},
            onCustomColorEnabledChange = {},
            onSeedColorChanged = {},
        )
    }
}
