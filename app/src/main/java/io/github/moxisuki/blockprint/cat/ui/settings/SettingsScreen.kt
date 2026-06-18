package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import io.github.moxisuki.blockprint.cat.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.data.ThemeManager
import io.github.moxisuki.blockprint.cat.data.ThemePreset
import io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao
import io.github.moxisuki.blockprint.cat.data.settings.AppIconManager
import io.github.moxisuki.blockprint.cat.data.settings.IconVariant
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SettingsScreenEntryPoint::class.java
        )
    }
    val themeManager = entryPoint.themeManager()
    val blueprintManager = entryPoint.blueprintManager()
    val communityConfig = entryPoint.communityConfig()
    val appIconManager = entryPoint.appIconManager()
    val appIconCurrent by appIconManager.current.collectAsState()

    val themeState by themeManager.themeState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    val communityEnabled by communityConfig.enabled.collectAsState()

    val headerAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 去掉 saveState 后每次进入都重建 composable，LaunchedEffect 可靠触发
        headerAnim.snapTo(0f)
        headerAnim.animateTo(1f, tween(900, 300))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 居中大标题区 — 始终占位，animation 作用在内容上避免闪烁
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.mipmap.ic_launcher_v4),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .scale(headerAnim.value)
                    .alpha(headerAnim.value),
                tint = androidx.compose.ui.graphics.Color.Unspecified,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "BlockPrint Cat",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(headerAnim.value),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 语言卡片
        LanguageSection()

        Spacer(modifier = Modifier.height(12.dp))

        // 主题卡片
        SettingsCard(
            icon = Icons.Default.Palette,
            title = stringResource(R.string.settings_theme_title),
            subtitle = when (themeState.mode) {
                ThemeManager.MODE_LIGHT -> stringResource(R.string.settings_theme_subtitle_light)
                ThemeManager.MODE_DARK -> stringResource(R.string.settings_theme_subtitle_dark)
                else -> stringResource(R.string.settings_theme_subtitle_system)
            },
            onClick = { showThemeDialog = true },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 蓝图渲染卡片
        SettingsCard(
            icon = Icons.Default.ViewInAr,
            title = stringResource(R.string.settings_render_title),
            subtitle = stringResource(R.string.settings_render_subtitle),
            onClick = { navController.navigate(NavRoutes.RENDER) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // MCS 社区登录设置卡片
        SettingsCard(
            icon = Icons.Default.People,
            title = stringResource(R.string.settings_community_card_title),
            subtitle = if (communityEnabled) stringResource(R.string.settings_community_subtitle_on)
                       else stringResource(R.string.settings_community_subtitle_off),
            onClick = { navController.navigate(NavRoutes.COMMUNITY_SETTINGS) },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 缓存管理卡片
        SettingsCard(
            icon = Icons.Default.Delete,
            title = stringResource(R.string.settings_cache_title),
            subtitle = stringResource(R.string.settings_cache_subtitle),
            onClick = { showCacheDialog = true },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 备份卡片
        SettingsCard(
            icon = Icons.Default.Archive,
            title = stringResource(R.string.settings_backup_title),
            subtitle = stringResource(R.string.settings_backup_subtitle),
            onClick = { showBackupDialog = true },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 关于卡片
        SettingsCard(
            icon = Icons.Default.Info,
            title = stringResource(R.string.settings_about_title),
            subtitle = stringResource(R.string.settings_about_subtitle),
            onClick = { navController.navigate(NavRoutes.ABOUT) },
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(onDismiss = { showThemeDialog = false })
    }

    if (showBackupDialog) {
        BackupDialog(onDismiss = { showBackupDialog = false })
    }

    if (showCacheDialog) {
        CacheManagementDialog(onDismiss = { showCacheDialog = false })
    }

}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SettingsScreenEntryPoint::class.java
        )
    }
    val themeManager = entryPoint.themeManager()
    val appIconManager = entryPoint.appIconManager()
    val themeState by themeManager.themeState.collectAsState()
    val appIconCurrent by appIconManager.current.collectAsState()

    var selectedMode by remember { mutableIntStateOf(themeState.mode) }
    var presetExplicitlySelected by remember { mutableIntStateOf(themeManager.findPresetIndex()) }
    var pendingIconVariant by remember { mutableStateOf(appIconCurrent) }
    var showColorPicker by remember { mutableStateOf(false) }

    val highlightedIndex = if (presetExplicitlySelected >= 0) presetExplicitlySelected else themeManager.findPresetIndex()

    val modeLabel: @Composable (Int) -> String = { mode ->
        when (mode) {
            0 -> stringResource(R.string.settings_theme_subtitle_system)
            1 -> stringResource(R.string.theme_mode_light)
            else -> stringResource(R.string.theme_mode_dark)
        }
    }

    val currentPreset: ThemePreset? = themeManager.presets.getOrNull(highlightedIndex)
    val currentIconVariant: IconVariant? = appIconManager.variants.firstOrNull { it.id == appIconCurrent }
    val (customPrimaryColor, customSecondaryColor) = themeManager.currentColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.theme_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                HeroSummary(
                    preset = currentPreset,
                    mode = themeState.mode,
                    iconVariant = currentIconVariant,
                    onModeLabel = { modeLabel(it) },
                )

                Spacer(Modifier.height(20.dp))

                SectionCard {
                    Text(
                        text = stringResource(R.string.theme_section_display),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    DisplayModeRow(
                        selected = selectedMode,
                        onSelect = { selectedMode = it },
                    )
                }

                Spacer(Modifier.height(20.dp))

                SectionCard {
                    Text(
                        text = stringResource(R.string.theme_section_color),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        themeManager.presets.forEachIndexed { index, preset ->
                            PresetColorRow(
                                preset = preset,
                                selected = index == highlightedIndex,
                                onClick = { presetExplicitlySelected = index },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionCard {
                    Text(
                        text = stringResource(R.string.theme_section_icon),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.theme_icon_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(appIconManager.variants, key = { it.id }) { variant ->
                            AppIconGalleryItem(
                                variant = variant,
                                selected = variant.id == pendingIconVariant,
                                onClick = { pendingIconVariant = variant.id },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionCard {
                    CustomColorRow(
                        primary = customPrimaryColor,
                        secondary = customSecondaryColor,
                        onClick = { showColorPicker = true },
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(onClick = {
                themeManager.themeMode = selectedMode
                if (presetExplicitlySelected in themeManager.presets.indices) {
                    val preset = themeManager.presets[presetExplicitlySelected]
                    themeManager.customPrimary = themeManager.colorToHex(preset.primary)
                    themeManager.customSecondary = themeManager.colorToHex(preset.secondary)
                }
                if (pendingIconVariant != appIconCurrent) {
                    appIconManager.apply(pendingIconVariant)
                }
                onDismiss()
            }) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    if (showColorPicker) {
        CustomColorDialog(
            onDismiss = { showColorPicker = false },
            onConfirm = { primary, secondary ->
                themeManager.customPrimary = themeManager.colorToHex(primary)
                themeManager.customSecondary = themeManager.colorToHex(secondary)
                presetExplicitlySelected = -1
                showColorPicker = false
            },
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

// ─────────────────────────────────────────────────────────────
// Theme dialog — modernized pieces (replaces the old segmented row / TextButton)
// ─────────────────────────────────────────────────────────────

private fun hexString(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

@Composable
private fun HeroSummary(
    preset: ThemePreset?,
    mode: Int,
    iconVariant: IconVariant?,
    onModeLabel: @Composable (Int) -> String,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(preset?.primary ?: Color.Transparent),
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(preset?.secondary ?: Color.Transparent),
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset?.name ?: stringResource(R.string.theme_custom_color),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = onModeLabel(mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (iconVariant != null) {
                    Image(
                        painter = painterResource(iconVariant.iconRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun DisplayModeRow(
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DisplayModeCard(
            icon = Icons.Default.PhoneAndroid,
            label = stringResource(R.string.theme_mode_system),
            subtitle = stringResource(R.string.settings_theme_subtitle_system),
            selected = selected == 0,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(0) },
        )
        DisplayModeCard(
            icon = Icons.Default.LightMode,
            label = stringResource(R.string.theme_mode_light),
            subtitle = null,
            selected = selected == 1,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(1) },
        )
        DisplayModeCard(
            icon = Icons.Default.DarkMode,
            label = stringResource(R.string.theme_mode_dark),
            subtitle = null,
            selected = selected == 2,
            modifier = Modifier.weight(1f),
            onClick = { onSelect(2) },
        )
    }
}

@Composable
private fun DisplayModeCard(
    icon: ImageVector,
    label: String,
    subtitle: String?,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

    Card(
        modifier = modifier.aspectRatio(1.1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = if (selected) contentColor else subtitleColor,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PresetColorRow(
    preset: ThemePreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(preset.primary, preset.secondary))),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${hexString(preset.primary)} · ${hexString(preset.secondary)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun CustomColorRow(
    primary: Color,
    secondary: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.theme_section_custom),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${hexString(primary)} · ${hexString(secondary)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(primary, secondary))),
            )
        }
    }
}

@Composable
private fun AppIconGalleryItem(
    variant: IconVariant,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(variant.iconRes),
                contentDescription = if (selected) stringResource(R.string.cd_icon_selected) else null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun CustomColorDialog(
    onDismiss: () -> Unit,
    onConfirm: (primary: Color, secondary: Color) -> Unit,
) {
    val customPresets = listOf(
        Pair(Color(0xFFE91E63), Color(0xFFC2185B)),
        Pair(Color(0xFF9C27B0), Color(0xFF7B1FA2)),
        Pair(Color(0xFF3F51B5), Color(0xFF303F9F)),
        Pair(Color(0xFF03A9F4), Color(0xFF0288D1)),
        Pair(Color(0xFF009688), Color(0xFF00796B)),
        Pair(Color(0xFF4CAF50), Color(0xFF388E3C)),
        Pair(Color(0xFFFFC107), Color(0xFFFFA000)),
        Pair(Color(0xFFFF5722), Color(0xFFE64A19)),
        Pair(Color(0xFF795548), Color(0xFF5D4037)),
        Pair(Color(0xFF607D8B), Color(0xFF455A64)),
        Pair(Color(0xFF673AB7), Color(0xFF512DA8)),
        Pair(Color(0xFF0091EA), Color(0xFF0064B0)),
    )

    var selectedPrimary by remember { mutableStateOf(customPresets[0].first) }
    var selectedSecondary by remember { mutableStateOf(customPresets[0].second) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_custom_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.theme_primary),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.height(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(customPresets.size) { i ->
                        val primary = customPresets[i].first
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(primary)
                                .clickable { selectedPrimary = primary; selectedSecondary = customPresets[i].second }
                                .then(
                                    if (selectedPrimary == primary)
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.theme_secondary),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    customPresets.forEach { (_, secondary) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(secondary)
                                .clickable { selectedSecondary = secondary }
                                .then(
                                    if (selectedSecondary == secondary)
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.theme_preview),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(selectedPrimary))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(selectedSecondary))
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surface),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedPrimary, selectedSecondary) }) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun BackupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            SettingsScreenEntryPoint::class.java
        )
    }
    val blueprintManager = entryPoint.blueprintManager()
    val scope = rememberCoroutineScope()
    var backing by remember { mutableStateOf(false) }
    var currentFile by remember { mutableIntStateOf(0) }
    var totalFiles by remember { mutableIntStateOf(0) }
    var totalBytes by remember { mutableStateOf(0L) }
    var compressedBytes by remember { mutableStateOf(0L) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    // 计算文件数和预估大小
    LaunchedEffect(Unit) {
        val files = runCatching {
            blueprintManager.estimateBackupSize()
        }.getOrNull()
        if (files != null) {
            totalFiles = files.first
            totalBytes = files.second
        }
    }

    fun startBackup() {
        backing = true
        resultMessage = null
        scope.launch {
            val res = blueprintManager.backupToZip { current, _ ->
                currentFile = current
            }
            res.onSuccess {
                compressedBytes = it.totalBytes
                resultMessage = "OK"
            }.onFailure { e ->
                resultMessage = e.message ?: ""
            }
            backing = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!backing) onDismiss() },
        title = { Text(stringResource(R.string.backup_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (resultMessage != null) {
                    val isSuccess = resultMessage == "OK"
                    Column {
                        Text(
                            if (isSuccess) stringResource(R.string.backup_done)
                            else stringResource(R.string.backup_failed, resultMessage!!),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (compressedBytes > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.backup_file_count_x, totalFiles),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.backup_size_x, formatBytes(compressedBytes)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.backup_location_x),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                    }
                } else if (backing) {
                    Column {
                        Text(stringResource(R.string.backup_in_progress), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (totalFiles > 0) currentFile.toFloat() / totalFiles else 0f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$currentFile / $totalFiles",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column {
                        Text(
                            stringResource(R.string.backup_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (totalFiles > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stringResource(R.string.backup_count_label), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "$totalFiles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stringResource(R.string.backup_size_label), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    formatBytes(totalBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.backup_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (resultMessage != null) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            } else {
                TextButton(
                    onClick = { startBackup() },
                    enabled = !backing && totalFiles > 0,
                ) { Text(if (backing) stringResource(R.string.backup_in_progress) else stringResource(R.string.settings_backup_title)) }
            }
        },
        dismissButton = {
            if (!backing) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
}

@Composable
private fun CacheManagementDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, SettingsScreenEntryPoint::class.java)
    }
    val blueprintManager = entryPoint.blueprintManager()
    val glbCacheDao = entryPoint.glbCacheDao()
    val scope = rememberCoroutineScope()
    var roomSize by remember { mutableStateOf(-1L) }
    var roomCount by remember { mutableStateOf(0) }
    var glbSize by remember { mutableStateOf(-1L) }
    var glbCount by remember { mutableStateOf(0) }
    var clearingRoom by remember { mutableStateOf(false) }
    var clearingGlb by remember { mutableStateOf(false) }
    var confirmRoom by remember { mutableStateOf(false) }
    var confirmGlb by remember { mutableStateOf(false) }

    // 订阅 GLB 缓存 — 来自 Room glb_cache 表
    val glbCache by glbCacheDao.observeAll().collectAsState(initial = emptyList())
    LaunchedEffect(glbCache) {
        glbCount = glbCache.size
        glbSize = glbCache.sumOf { it.sizeBytes }
    }

    LaunchedEffect(Unit) {
        val dbPath = context.getDatabasePath("litematic.db")
        roomSize = if (dbPath.isFile) dbPath.length() else 0L
        roomCount = blueprintManager.blueprintCount.value
    }

    fun doClearRoom() {
        clearingRoom = true
        scope.launch {
            runCatching { blueprintManager.clearAllBlueprints() }
            val dbPath = context.getDatabasePath("litematic.db")
            roomSize = if (dbPath.isFile) dbPath.length() else 0L
            roomCount = 0
            clearingRoom = false; confirmRoom = false
        }
    }

    fun doClearGlb() {
        clearingGlb = true
        scope.launch {
            runCatching {
                // 1. 清空磁盘上 glb_cache 目录
                val dir = java.io.File(context.filesDir, "glb_cache")
                dir.listFiles()?.forEach { it.delete() }
                // 2. 清空 GlbGenerator 内存缓存 (GlbCache 文件已在步骤 1 删除)
                io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager.generator?.clearAllCache()
                // 3. 清空 Room glb_cache 表 + 通知 RenderResourceManager
                io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager.clearAllGlb()
                glbSize = 0L; glbCount = 0
            }
            clearingGlb = false; confirmGlb = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_cache_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Room 缓存
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (!confirmRoom) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(stringResource(R.string.cache_room_title), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (roomSize >= 0) stringResource(R.string.cache_room_stats, roomCount, formatSize(roomSize)) else stringResource(R.string.cache_calculating),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(
                                    onClick = { confirmRoom = true },
                                    enabled = roomCount > 0 && !clearingRoom,
                                ) { Text(stringResource(R.string.action_clear)) }
                            }
                        } else {
                            Text(
                                stringResource(R.string.cache_room_confirm_msg),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { confirmRoom = false }) { Text(stringResource(R.string.action_cancel)) }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { doClearRoom() }, enabled = !clearingRoom) {
                                    Text(
                                        if (clearingRoom) stringResource(R.string.cache_clearing)
                                        else stringResource(R.string.action_confirm_clear),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // GLB 缓存
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        if (!confirmGlb) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(stringResource(R.string.cache_glb_title), style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (glbSize >= 0) stringResource(R.string.cache_glb_stats, glbCount, formatSize(glbSize)) else stringResource(R.string.cache_calculating),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(
                                    onClick = { confirmGlb = true },
                                    enabled = glbSize > 0 && !clearingGlb,
                                ) { Text(stringResource(R.string.action_clear)) }
                            }
                        } else {
                            Text(
                                stringResource(R.string.cache_glb_confirm_msg),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { confirmGlb = false }) { Text(stringResource(R.string.action_cancel)) }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { doClearGlb() }, enabled = !clearingGlb) {
                                    Text(
                                        if (clearingGlb) stringResource(R.string.cache_clearing)
                                        else stringResource(R.string.action_confirm_clear),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.cache_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsScreenEntryPoint {
    fun themeManager(): ThemeManager
    fun blueprintManager(): BlueprintManager
    fun glbCacheDao(): GlbCacheDao
    fun communityConfig(): io.github.moxisuki.blockprint.cat.data.community.CommunityConfigManager
    fun appIconManager(): AppIconManager
}