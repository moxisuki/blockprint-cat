package io.github.moxisuki.blockprint.cat.ui.tools

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.moxisuki.blockprint.cat.R

internal enum class ToolKind { Hero, ListItem }

internal data class ToolEntry(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int?,
    val icon: ImageVector,
    val accent: Color,
    val kind: ToolKind,
)

internal object ToolCatalog {
    val entries: List<ToolEntry> = listOf(
        ToolEntry(
            id = "image_to_blueprint",
            titleRes = R.string.tool_image_to_blueprint,
            subtitleRes = R.string.tool_image_to_blueprint_subtitle,
            icon = Icons.Outlined.Image,
            accent = Color(0xFF7E57C2),
            kind = ToolKind.Hero,
        ),
        ToolEntry(
            id = "pixel_art",
            titleRes = R.string.tool_pixel_art,
            subtitleRes = null,
            icon = Icons.Outlined.GridOn,
            accent = Color(0xFF26A69A),
            kind = ToolKind.ListItem,
        ),
        ToolEntry(
            id = "color_palette",
            titleRes = R.string.tool_color_palette,
            subtitleRes = null,
            icon = Icons.Outlined.Palette,
            accent = Color(0xFFEF5350),
            kind = ToolKind.ListItem,
        ),
        ToolEntry(
            id = "qr_scan",
            titleRes = R.string.tool_qr_scan,
            subtitleRes = null,
            icon = Icons.Outlined.QrCodeScanner,
            accent = Color(0xFF42A5F5),
            kind = ToolKind.ListItem,
        ),
        ToolEntry(
            id = "backup_restore",
            titleRes = R.string.tool_backup_restore,
            subtitleRes = null,
            icon = Icons.Outlined.Backup,
            accent = Color(0xFFFFA726),
            kind = ToolKind.ListItem,
        ),
        ToolEntry(
            id = "cache_clear",
            titleRes = R.string.tool_cache_clear,
            subtitleRes = null,
            icon = Icons.Outlined.CleaningServices,
            accent = Color(0xFF78909C),
            kind = ToolKind.ListItem,
        ),
    )
}