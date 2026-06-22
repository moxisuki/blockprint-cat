package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R

/**
 * 使用条款 — 单一来源。TermsGate（冷启动全屏门）和 TermsScreen（关于页跳转）共用。
 * 存 @StringRes ID，由消费者调用 [termsSections] / [termsIntro] / [termsLastUpdated] 解析。
 */
object TermsContent {
    @StringRes val introRes: Int = R.string.terms_intro
    @StringRes val lastUpdatedRes: Int = R.string.terms_last_updated

    val sectionIds: List<Pair<Int, Int>> = listOf(
        R.string.terms_section_1_title to R.string.terms_section_1_body,
        R.string.terms_section_2_title to R.string.terms_section_2_body,
        R.string.terms_section_3_title to R.string.terms_section_3_body,
        R.string.terms_section_4_title to R.string.terms_section_4_body,
        R.string.terms_section_5_title to R.string.terms_section_5_body,
        R.string.terms_section_6_title to R.string.terms_section_6_body,
        R.string.terms_section_7_title to R.string.terms_section_7_body,
        R.string.terms_section_8_title to R.string.terms_section_8_body,
    )
}

/** 返回已本地化的条款段落列表（可在 LazyColumn 或 Column 内遍历）。 */
@Composable
fun termsSections(): List<TermsSection> = TermsContent.sectionIds.map { (titleRes, bodyRes) ->
    TermsSection(
        title = stringResource(titleRes),
        body = stringResource(bodyRes),
    )
}

data class TermsSection(val title: String, val body: String)

@Composable
fun termsIntro(): String = stringResource(TermsContent.introRes)

@Composable
fun termsLastUpdated(): String = stringResource(TermsContent.lastUpdatedRes)
