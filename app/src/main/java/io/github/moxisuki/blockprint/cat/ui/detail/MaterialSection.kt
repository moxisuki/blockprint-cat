package io.github.moxisuki.blockprint.cat.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.IconIndexResolver
import io.github.moxisuki.blockprint.cat.data.vanilla.LangManager
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber

/**
 * Material list row used by the detail screen's Top-10 stats card.
 * Combines the [MaterialIcon] (with on-disk icon-fallback), the localized
 * material name, the registry key, and the block count.
 */
@Composable
internal fun MaterialRow(name: String, count: Int) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DetailScreenEntryPoint::class.java
        )
    }
    val iconIndexResolver = entryPoint.iconIndexResolver()
    LaunchedEffect(Unit) {
        iconIndexResolver.ensureLoaded()
    }
    val langName = remember(name) { LangManager.displayName(context, name) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MaterialIcon(name = name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                langName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "× ${formatNumber(count)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Material icon. Tries up to 3 icon URL variants via Coil and falls back
 * to a stable colour-tinted 2-letter box for vanilla items, or `?` for
 * unknown. Depends on [DetailScreenEntryPoint] for the IconIndexResolver.
 */
@Composable
internal fun MaterialIcon(name: String) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DetailScreenEntryPoint::class.java
        )
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
    val currentUrl = variants.getOrNull(attempt)

    if (currentUrl != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(currentUrl)
                .crossfade(true)
                .build(),
            contentDescription = name,
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)),
            error = {
                if (attempt < variants.lastIndex) {
                    LaunchedEffect(currentUrl) { attempt++ }
                } else {
                    UnknownIcon()
                }
            },
        )
    } else {
        val displayName = name.removePrefix("minecraft:")
        if (name.startsWith("minecraft:")) {
            val bg = materialColor(name)
            val label = displayName.replace("_", " ").take(2).uppercase()
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        } else {
            UnknownIcon()
        }
    }
}

/** "?" placeholder when neither an icon URL nor a vanilla colour-box is available. */
@Composable
internal fun UnknownIcon() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

/**
 * Hilt entry point that exposes the singleton [IconIndexResolver] to
 * composables without dragging a full ViewModel into the MaterialRow
 * subtree. Registered against the [SingletonComponent] so it lives for
 * the app's lifetime.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface DetailScreenEntryPoint {
    fun iconIndexResolver(): IconIndexResolver
}
