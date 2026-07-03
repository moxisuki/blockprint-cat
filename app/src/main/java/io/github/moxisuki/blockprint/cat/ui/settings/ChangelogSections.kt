package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.ExternalUrls

/** 版本段（如 `[1.0.0] · 2026-07-02`）—— 蓝色突出 */
@Composable
internal fun ChangelogEntryTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** 单个分类标题（如 新增 / 修复 / 重构）*/
@Composable
internal fun ChangelogSectionTitle(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

/** 段间分隔线 */
@Composable
internal fun ChangelogSectionDivider() {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    Spacer(Modifier.height(16.dp))
}

/** 单条 bullet —— 按 parts 切 bold / 普通 */
@Composable
internal fun ChangelogBulletRow(parts: List<ChangelogPart>) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("• ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = parts.toAnnotatedString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = TextUnit(22f, TextUnitType.Sp),
        )
    }
}

/** 顶部跳转行 —— 打开 GitHub 上的 CHANGELOG.md 渲染页 */
@Composable
internal fun ChangelogOpenOnGitHubRow() {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(ExternalUrls.CHANGELOG_URL) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.changelog_open_on_github),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun List<ChangelogPart>.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    for (p in this@toAnnotatedString) when (p) {
        is ChangelogPart.Text -> append(p.text)
        is ChangelogPart.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(p.text) }
    }
}
