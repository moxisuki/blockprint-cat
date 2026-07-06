package io.github.moxisuki.blockprint.cat.ui.tools

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.moxisuki.blockprint.cat.R

enum class ToolKind { Hero, ListItem }

data class ToolEntry(
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
            id = "text_to_blueprint",
            titleRes = R.string.tool_text_to_blueprint,
            subtitleRes = R.string.tool_text_to_blueprint_subtitle,
            icon = Icons.Outlined.TextFields,
            accent = Color(0xFF42A5F5),
            kind = ToolKind.ListItem,
        ),
    )
}