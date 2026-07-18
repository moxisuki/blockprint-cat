package io.github.moxisuki.blockprint.cat.app.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.AppDefaultThemeSeedColor
import io.github.moxisuki.blockprint.cat.app.core.design.AppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeColorSource
import io.github.moxisuki.blockprint.cat.app.core.design.AppThemeMode
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import io.github.moxisuki.blockprint.cat.app.core.locale.AppLanguage
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ColorPalette
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlaySpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    selectedThemeMode: AppThemeMode,
    selectedThemeColorSource: AppThemeColorSource,
    themeSeedColor: Color,
    selectedLanguage: AppLanguage,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themeModes = listOf(
        AppThemeMode.System,
        AppThemeMode.Light,
        AppThemeMode.Dark,
    )
    val themeModeLabels = listOf(
        stringResource(R.string.theme_mode_system),
        stringResource(R.string.theme_mode_light),
        stringResource(R.string.theme_mode_dark),
    )
    val colorSources = listOf(
        AppThemeColorSource.Default,
        AppThemeColorSource.Monet,
        AppThemeColorSource.Custom,
    )
    val colorSourceLabels = listOf(
        stringResource(R.string.theme_color_source_default),
        stringResource(R.string.theme_color_source_monet),
        stringResource(R.string.theme_color_source_custom),
    )
    val languages = listOf(
        AppLanguage.System,
        AppLanguage.Chinese,
        AppLanguage.English,
    )
    val languageLabels = listOf(
        stringResource(R.string.settings_language_subtitle_system),
        stringResource(R.string.settings_language_subtitle_zh),
        stringResource(R.string.settings_language_subtitle_en),
    )
    val seedColorHex = themeSeedColor.toHexRgb()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appScrollEndHaptic(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        item(key = "appearance-title") {
            SectionTitle(
                text = stringResource(R.string.settings_section_appearance),
            )
        }
        item(key = "theme-mode") {
            OverlaySpinnerPreference(
                items = themeModeLabels.map { label -> DropdownItem(text = label) },
                selectedIndex = themeModes.indexOf(selectedThemeMode).coerceAtLeast(0),
                title = stringResource(R.string.theme_display_mode),
                summary = stringResource(R.string.theme_display_mode_summary),
                onSelectedIndexChange = { index ->
                    onAction(SettingsAction.ThemeModeSelected(themeModes[index]))
                },
            )
        }
        item(key = "theme-color-source") {
            OverlaySpinnerPreference(
                modifier = Modifier.padding(top = 8.dp),
                items = colorSourceLabels.map { label -> DropdownItem(text = label) },
                selectedIndex = colorSources.indexOf(selectedThemeColorSource).coerceAtLeast(0),
                title = stringResource(R.string.theme_color_source),
                summary = stringResource(R.string.theme_color_source_summary),
                onSelectedIndexChange = { index ->
                    onAction(SettingsAction.ThemeColorSourceSelected(colorSources[index]))
                },
            )
        }
        if (selectedThemeColorSource == AppThemeColorSource.Custom) {
            item(key = "theme-palette-entry") {
                ArrowPreference(
                    modifier = Modifier.padding(top = 8.dp),
                    title = stringResource(R.string.theme_palette_title),
                    summary = stringResource(R.string.theme_palette_selected, seedColorHex),
                    startAction = {
                        ThemeColorSwatch(color = themeSeedColor)
                    },
                    onClick = {
                        onAction(
                            SettingsAction.ThemePaletteExpansionChanged(
                                expanded = !state.isThemePaletteExpanded,
                            ),
                        )
                    },
                )
            }
            item(key = "theme-palette") {
                AnimatedVisibility(
                    visible = state.isThemePaletteExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ThemePalettePanel(
                        color = themeSeedColor,
                        onColorChanged = { color ->
                            onAction(SettingsAction.ThemeSeedColorSelected(color))
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        item(key = "language-title") {
            SectionTitle(
                text = stringResource(R.string.settings_language_title),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "language") {
            OverlaySpinnerPreference(
                items = languageLabels.map { label -> DropdownItem(text = label) },
                selectedIndex = languages.indexOf(selectedLanguage).coerceAtLeast(0),
                title = stringResource(R.string.settings_language_title),
                onSelectedIndexChange = { index ->
                    onAction(SettingsAction.LanguageSelected(languages[index]))
                },
            )
        }
        item(key = "about-title") {
            SectionTitle(
                text = stringResource(R.string.settings_section_about),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "about-entry") {
            ArrowPreference(
                title = stringResource(R.string.settings_about_title),
                summary = stringResource(R.string.settings_about_subtitle_info),
                onClick = { onAction(SettingsAction.AboutClicked) },
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
    )
}

@Composable
private fun ThemePalettePanel(
    color: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(20.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.theme_palette_summary),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
            Spacer(modifier = Modifier.height(14.dp))
            ColorPalette(
                color = color,
                onColorChanged = onColorChanged,
                showPreview = true,
            )
        }
    }
}

@Composable
private fun ThemeColorSwatch(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(shape)
            .background(color)
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.outline,
                shape = shape,
            ),
    )
}

private fun Color.toHexRgb(): String =
    "#${(toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')}"

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    AppTheme {
        SettingsScreen(
            state = SettingsState(isThemePaletteExpanded = true),
            selectedThemeMode = AppThemeMode.System,
            selectedThemeColorSource = AppThemeColorSource.Custom,
            themeSeedColor = AppDefaultThemeSeedColor,
            selectedLanguage = AppLanguage.System,
            onAction = {},
        )
    }
}
