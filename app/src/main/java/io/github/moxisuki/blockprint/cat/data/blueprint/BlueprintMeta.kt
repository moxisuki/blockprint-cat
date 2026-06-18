package io.github.moxisuki.blockprint.cat.data.blueprint

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.core.SchematicFormat

@Immutable
data class BlueprintMeta(
    val uuid: String,
    val fileDocId: String,
    val fileName: String,
    val displayName: String,
    val author: String,
    val regionCount: Int,
    val blockCount: Int,
    val format: SchematicFormat,
) {
    /** 预计算的列表副标题，避免每次 recompose 做字符串拼接 */
    val subtitle: String = "作者: ${author.ifEmpty { "未知" }}  |  区域: $regionCount  |  方块: ${formatNumber(blockCount)}"
}

private fun formatNumber(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}.${(n % 1_000_000) / 100_000}M"
    n >= 1_000 -> "${n / 1_000}.${(n % 1_000) / 100}K"
    else -> n.toString()
}
