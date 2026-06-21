package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R

@Composable
fun TransferProgressBar(
    transfers: List<TransferItem>,
    modifier: Modifier = Modifier,
) {
    // Hold onto items after they're removed from `transfers` until their exit animation finishes.
    val visible = remember { mutableStateListOf<TransferItem>() }
    val currentlyVisibleIds = transfers.map { it.id }.toSet()
    LaunchedEffect(transfers) {
        // Add new items that aren't yet in our list
        transfers.forEach { newItem ->
            if (visible.none { it.id == newItem.id }) visible.add(newItem)
        }
        // Animate out removed items, then drop them from our list
        val toRemove = visible.filter { it.id !in currentlyVisibleIds }
        if (toRemove.isNotEmpty()) {
            kotlinx.coroutines.delay(400)
            visible.removeAll(toRemove.toSet())
        } else {
            // Sync (no removals) — replace list with latest snapshots so progress updates show
            visible.clear()
            visible.addAll(transfers)
        }
    }

    if (visible.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
    ) {
        for (item in visible) {
            androidx.compose.animation.AnimatedVisibility(
                visible = item.id in currentlyVisibleIds,
                enter = androidx.compose.animation.expandVertically(animationSpec = androidx.compose.animation.core.tween(200))
                    + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(160)),
                exit = androidx.compose.animation.shrinkVertically(animationSpec = androidx.compose.animation.core.tween(360))
                    + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(280)),
            ) {
                TransferRow(item)
            }
        }
    }
}

@Composable
private fun TransferRow(item: TransferItem) {
    val isDownload = item.type == TransferType.DOWNLOAD
    val isDone = item.phase == TransferPhase.DONE
    val isFailed = item.phase == TransferPhase.FAILED

    val containerColor = when {
        isDone -> MaterialTheme.colorScheme.primaryContainer
        isFailed -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        isDone -> MaterialTheme.colorScheme.onPrimaryContainer
        isFailed -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val progressColor = when {
        isDone -> MaterialTheme.colorScheme.primary
        isFailed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when {
        isDone -> if (isDownload) stringResource(R.string.transfer_status_downloaded)
                  else stringResource(R.string.transfer_status_uploaded)
        isFailed -> stringResource(R.string.transfer_status_failed)
        else -> if (isDownload) stringResource(R.string.transfer_status_downloading)
                else stringResource(R.string.transfer_status_uploading)
    }

    val trailingIcon = when {
        isDone -> Icons.Default.CheckCircle
        isFailed -> Icons.Default.ErrorOutline
        isDownload -> Icons.Default.CloudDownload
        else -> Icons.Default.CloudUpload
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            trailingIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isDone || isFailed) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else if (isDownload && item.totalBytes > 0) {
                LinearProgressIndicator(
                    progress = { item.fraction ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = progressColor,
                    trackColor = contentColor.copy(alpha = 0.15f),
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = progressColor,
                    trackColor = contentColor.copy(alpha = 0.15f),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = when {
                isDone -> "100%"
                isFailed -> "!"
                isDownload && item.totalBytes > 0 -> item.fraction?.let { "${(it * 100).toInt()}%" } ?: "…"
                else -> "…"
            },
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}
