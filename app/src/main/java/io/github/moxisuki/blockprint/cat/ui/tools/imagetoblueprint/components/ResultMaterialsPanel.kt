package io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.vanilla.LangManager
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.BlockCatalog
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber

@Composable
internal fun ResultMaterialsPanel(
    totalBlocks: Int,
    materialCounts: Map<String, Int>,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // 把 blockId 映射到 drawableResId 一次，省去 Row 内重复查找
    val idToResId = remember(context) {
        BlockCatalog.all.associate { it.id to it.drawableResId }
    }

    when {
        errorMessage != null -> {
            Text(
                stringResource(R.string.itb_result_error, errorMessage),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = modifier,
            )
        }
        totalBlocks > 0 && materialCounts.isNotEmpty() -> {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.itb_result_total, formatNumber(totalBlocks)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.itb_result_materials),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                )
                val sorted = materialCounts.entries.sortedByDescending { it.value }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sorted.take(10).forEach { (name, count) ->
                        // engine 给的 key 是 "white_wool" 这种无命名空间形式
                        // lang 文件的键是 "block.minecraft.white_wool"，需加 minecraft: 前缀
                        val displayName = LangManager.displayName(context, "minecraft:$name")
                        val resId = idToResId[name]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (resId != null) {
                                    val bitmap = findPixelArt(resId)
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                            filterQuality = FilterQuality.None,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                }
                                Text(displayName, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                "× ${formatNumber(count)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (sorted.size > 10) {
                        Text(
                            "+ ${sorted.size - 10} more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
        else -> {
            Text(
                stringResource(R.string.itb_no_result),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = modifier,
            )
        }
    }
}
