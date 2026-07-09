package io.github.moxisuki.blockprint.cat.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintMeta
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber

/**
 * Single blueprint card used in the Local tab's [androidx.compose.foundation.lazy.LazyColumn].
 *
 * Renders the file icon + display name + format chip + author/region/block-count
 * meta line + the action icon row (upload / rename / delete). The card body
 * itself is clickable (calls [onDetail]); the inner IconButtons override that
 * with their own click targets so taps don't fall through to the card.
 *
 * `connected`/`canTransfer` gate the upload button: when the bridge is
 * disconnected or a transfer is already in flight, the upload icon is shown
 * disabled so the user knows the action won't run.
 *
 * Multi-select state:
 *  - When [onLongClick] is non-null the card uses `combinedClickable` so a long
 *    press triggers multi-select while a short tap still opens detail.
 *  - When [selected] is true the card swaps to a primary-tinted background,
 *    draws a thin primary border, and overlays a circular check badge in the
 *    top-left corner.
 *
 * Depends on [FormatChip] from `HomeFilterChips.kt` (same package, no import).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeBlueprintCard(
    blueprint: BlueprintMeta,
    onDetail: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onUpload: () -> Unit,
    connected: Boolean = false,
    canTransfer: Boolean = true,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .let { base ->
            if (selected) {
                base.border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp),
                )
            } else {
                base
            }
        }
        .let { base ->
            if (onLongClick != null) {
                base.combinedClickable(
                    onClick = onDetail,
                    onLongClick = onLongClick,
                )
            } else {
                base.clickable { onDetail() }
            }
        }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                // 第一行：标题 + 格式 chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            blueprint.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FormatChip(format = blueprint.format)
                }

                Spacer(Modifier.height(10.dp))

                // 第二行：作者 / 区域 / 方块数
                Text(
                    stringResource(R.string.bp_card_author, blueprint.author.ifEmpty { stringResource(R.string.bp_card_author_unknown) }) +
                        "  ·  " + stringResource(R.string.bp_card_region, blueprint.regionCount) +
                        "  ·  " + stringResource(R.string.bp_card_blocks, formatNumber(blueprint.blockCount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(6.dp))

                // 第三行：操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onUpload, enabled = canTransfer, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.CloudUpload,
                            stringResource(R.string.action_sync),
                            modifier = Modifier.size(20.dp),
                            tint = if (canTransfer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        )
                    }
                    IconButton(onClick = onRename, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            stringResource(R.string.action_rename),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            stringResource(R.string.action_delete),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}