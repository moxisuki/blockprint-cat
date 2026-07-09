package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.ThemeManager
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.data.render.GlbCacheDao
import io.github.moxisuki.blockprint.cat.data.settings.AppIconManager
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes

@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, SettingsScreenEntryPoint::class.java)
    }
    val themeManager = entryPoint.themeManager()
    val communityConfig = entryPoint.communityConfig()
    val appIconManager = entryPoint.appIconManager()
    val appIconCurrent by appIconManager.current.collectAsState()
    val appIconVariant = appIconManager.variants.firstOrNull { it.id == appIconCurrent } ?: appIconManager.variants.first()

    val themeState by themeManager.themeState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    val communityEnabled by communityConfig.enabled.collectAsState()

    val iconScale = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val titleSlide = with(density) { 16.dp.toPx() }

    LaunchedEffect(Unit) {
        iconScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 360f))
        titleAlpha.animateTo(1f, tween(400, delayMillis = 80))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(appIconVariant.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .scale(iconScale.value)
                    .alpha(iconScale.value),
                tint = androidx.compose.ui.graphics.Color.Unspecified,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "BlockPrint Cat",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .graphicsLayer { translationY = (1f - titleAlpha.value) * titleSlide },
            )
        }

        SectionHeader(stringResource(R.string.settings_section_appearance))
        LanguageSection()
        Spacer(modifier = Modifier.height(8.dp))
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

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(stringResource(R.string.settings_section_features))
        SettingsCard(
            icon = Icons.Default.ViewInAr,
            title = stringResource(R.string.settings_render_title),
            subtitle = stringResource(R.string.settings_render_subtitle),
            onClick = { navController.navigate(NavRoutes.RENDER) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard(
            icon = Icons.Default.People,
            title = stringResource(R.string.settings_community_card_title),
            subtitle = if (communityEnabled) stringResource(R.string.settings_community_subtitle_on) else stringResource(R.string.settings_community_subtitle_off),
            onClick = { navController.navigate(NavRoutes.COMMUNITY_SETTINGS) },
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(stringResource(R.string.settings_section_data))
        SettingsCard(
            icon = Icons.Default.Delete,
            title = stringResource(R.string.settings_cache_title),
            subtitle = stringResource(R.string.settings_cache_subtitle),
            onClick = { showCacheDialog = true },
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsCard(
            icon = Icons.Default.Archive,
            title = stringResource(R.string.settings_backup_title),
            subtitle = stringResource(R.string.settings_backup_subtitle),
            onClick = { showBackupDialog = true },
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(stringResource(R.string.settings_section_about))
        SettingsCard(
            icon = Icons.Default.Info,
            title = stringResource(R.string.settings_about_title),
            subtitle = stringResource(R.string.settings_about_subtitle_info),
            onClick = { navController.navigate(NavRoutes.ABOUT) },
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showThemeDialog) ThemeSelectionDialog(onDismiss = { showThemeDialog = false })
    if (showBackupDialog) BackupDialog(onDismiss = { showBackupDialog = false })
    if (showCacheDialog) CacheManagementDialog(onDismiss = { showCacheDialog = false })
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
internal fun SettingsCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
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
