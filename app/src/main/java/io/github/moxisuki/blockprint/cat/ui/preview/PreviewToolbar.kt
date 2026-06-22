package io.github.moxisuki.blockprint.cat.ui.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun ToolIcon(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick).padding(4.dp)) { Icon(icon, desc, Modifier.size(22.dp), tint = tint) }
}

@Composable
internal fun LayerIconBtn(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) Color.White
               else Color.White.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(20.dp), tint = tint)
    }
}
