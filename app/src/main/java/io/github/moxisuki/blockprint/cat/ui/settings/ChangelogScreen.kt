package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.R

/**
 * 关于 → 更新 → 更新日志 的展示页。内容由 `:app:generateChangelog` 在构建时
 * 从项目根 `CHANGELOG.md` 解析生成，运行时直接读取 `changelogEntries()`。
 * 顶部「在 GitHub 上查看」行打开远程 markdown 渲染页。
 */
@Composable
fun ChangelogScreen(navController: NavController) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.changelog_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))
            ChangelogOpenOnGitHubRow()

            Spacer(Modifier.height(12.dp))

            changelogEntries().forEachIndexed { index, entry ->
                if (index > 0) ChangelogSectionDivider()
                ChangelogEntryTitle(entry.title)
                for (section in entry.sections) {
                    ChangelogSectionTitle(section.title)
                    for (bullet in section.bullets) {
                        ChangelogBulletRow(bullet.parts)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
