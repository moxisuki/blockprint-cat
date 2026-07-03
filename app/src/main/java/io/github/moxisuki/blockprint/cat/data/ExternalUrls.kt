package io.github.moxisuki.blockprint.cat.data

/**
 * 集中外部 URL 常量。新的对外跳转链接统一加到这里，避免在多个 Composable
 * 里硬编码字符串。
 */
internal object ExternalUrls {
    /**
     * 项目根 `CHANGELOG.md` 的 GitHub 渲染页。
     * 关于页 → 更新日志 → ChangelogScreen 顶部的「在 GitHub 上查看」会打开这个 URL。
     */
    const val CHANGELOG_URL: String =
        "https://github.com/moxisuki/blockprint-cat/blob/master/CHANGELOG.md"
}
