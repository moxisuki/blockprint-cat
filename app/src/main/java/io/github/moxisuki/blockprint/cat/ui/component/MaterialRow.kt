package io.github.moxisuki.blockprint.cat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import io.github.moxisuki.blockprint.cat.data.IconIndexResolver
import io.github.moxisuki.blockprint.cat.data.vanilla.LangManager
import io.github.moxisuki.blockprint.cat.ui.detail.DetailScreenEntryPoint
import io.github.moxisuki.blockprint.cat.data.vanilla.AssetNamespaceResolver
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import dagger.hilt.android.EntryPointAccessors

/** Material row used by both blueprint detail page and community detail page. */
@Composable
fun MaterialRow(name: String, count: Int) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext as android.app.Application, DetailScreenEntryPoint::class.java)
    }
    val iconIndexResolver = entryPoint.iconIndexResolver()
    val indexReady by iconIndexResolver.ready.collectAsState()
    val variants = remember(name, indexReady) {
        listOfNotNull(
            iconIndexResolver.getIconUrl(name),
            iconIndexResolver.getIconUrl(name, "_block"),
            iconIndexResolver.getIconUrl(name, "_item"),
        )
    }
    var attempt by remember { mutableIntStateOf(0) }
    var localAttempt by remember { mutableIntStateOf(0) }
    val currentUrl = variants.getOrNull(attempt)

    // Local render asset fallback — fuzzy match, strict
    val localPaths = remember(name) {
        val colon = name.indexOf(':')
        val ns = if (colon >= 0) name.substring(0, colon) else "minecraft"
        val item = if (colon >= 0) name.substring(colon + 1) else name
        val root = java.io.File(context.filesDir, "blockprintcat/render_assets/$ns")
        val candidates = mutableListOf<java.io.File>()
        for (sub in listOf("textures/item", "textures/block")) {
            val dir = java.io.File(root, sub)
            if (dir.isDirectory) {
                dir.listFiles()?.filter { it.extension == "png" }?.let { candidates.addAll(it) }
            }
        }
        val target = item.lowercase()
        candidates.mapNotNull { f ->
            val stem = f.nameWithoutExtension.lowercase()
            val score = when {
                stem == target -> 1000
                stem.contains(target) -> 500 - (stem.length - target.length).coerceAtLeast(0)
                target.contains(stem) -> 400 - (target.length - stem.length).coerceAtLeast(0)
                else -> null
            }
            score?.let { f to it }
        }.sortedByDescending { (_, s) -> s }.map { it.first }.take(3)
    }

    val langName = remember(name) { LangManager.displayName(context, name) }

    ListItem(
        headlineContent = {
            Text(langName, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = { Text("× ${formatNumber(count)}") },
        leadingContent = {
            if (currentUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context).data(currentUrl).crossfade(true).build(),
                    contentDescription = name,
                    modifier = Modifier.size(40.dp),
                    error = {
                        if (attempt < variants.lastIndex) LaunchedEffect(currentUrl) { attempt++ }
                        else {
                            val f = localPaths.getOrNull(localAttempt)
                            if (f != null && f.isFile) { localAttempt++ }
                            else UnknownIcon()
                        }
                    },
                )
            } else if (localPaths.any { it.isFile }) {
                val file = localPaths.first { it.isFile }
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context).data(file).crossfade(true).build(),
                    contentDescription = name,
                    modifier = Modifier.size(40.dp),
                    error = { UnknownIcon() },
                )
            } else {
                UnknownIcon()
            }
        },
    )
}

@Composable
private fun UnknownIcon() {
    Box(
        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Text("?", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
