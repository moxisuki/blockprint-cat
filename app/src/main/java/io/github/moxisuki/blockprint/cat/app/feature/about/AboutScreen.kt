package io.github.moxisuki.blockprint.cat.app.feature.about

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.AppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import top.yukonga.miuix.kmp.anim.DecelerateEasing
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AboutScreen(
    state: AboutState,
    onAction: (AboutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appScrollEndHaptic(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        item(key = "hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppMark()
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(
                        R.string.about_build_summary,
                        state.appVersionName,
                        state.blockPrintCoreVersion,
                        state.miuixVersion,
                    ),
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.Center,
                )
            }

            if (state.isChineseLocale) {
            HitokotoQuote(
                state = state.hitokoto,
                onRefresh = { onAction(AboutAction.RefreshHitokoto) },
                modifier = Modifier.padding(vertical = 6.dp),
            )
            }
        }

        item(key = "app-info-title") {
            SectionTitle(
                text = stringResource(R.string.about_section_info),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item(key = "app-info") {
            AboutSectionCard {
                InfoComponent(
                    title = stringResource(R.string.about_label_app_name),
                    summary = stringResource(R.string.app_name),
                )
                SectionDivider()
                InfoComponent(
                    title = stringResource(R.string.about_label_version),
                    summary = stringResource(R.string.about_version, state.appVersionName),
                )
                SectionDivider()
                InfoComponent(
                    title = stringResource(R.string.about_label_version_code),
                    summary = state.appVersionCode.toString(),
                )
                SectionDivider()
                InfoComponent(
                    title = stringResource(R.string.about_label_package),
                    summary = state.applicationId,
                )
                SectionDivider()
                InfoComponent(
                    title = stringResource(R.string.about_engine_label),
                    summary = stringResource(R.string.about_engine_version, state.blockPrintCoreVersion),
                )
            }
        }
        item(key = "open-source-title") {
            SectionTitle(
                text = stringResource(R.string.about_section_opensource),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "open-source") {
            AboutSectionCard {
                InfoComponent(
                    title = stringResource(R.string.about_label_source_code),
                    summary = stringResource(R.string.about_project_repository_value),
                )
                SectionDivider()
                InfoComponent(
                    title = stringResource(R.string.about_label_core_repository),
                    summary = stringResource(R.string.about_core_repository_value),
                )
                SectionDivider()
                InfoComponent(
                    title = stringResource(R.string.about_label_license),
                    summary = stringResource(R.string.about_license_repository),
                )
                SectionDivider()
                InfoComponent(
                    title = stringResource(R.string.about_open_source_note_title),
                    summary = stringResource(R.string.about_open_source_summary),
                )
            }
        }
        item(key = "libraries-title") {
            SectionTitle(
                text = stringResource(R.string.about_section_libraries),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "libraries") {
            AboutSectionCard {
                state.libraries.forEachIndexed { index, library ->
                    LibraryComponent(library = library)
                    if (index != state.libraries.lastIndex) {
                        SectionDivider()
                    }
                }
            }
        }
        item(key = "bottom-space") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HitokotoQuote(
    state: AboutHitokotoState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onRefresh)
            .padding(horizontal = 16.dp),
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = DecelerateEasing(),
                ),
            ) + slideInVertically(
                initialOffsetY = { it / 8 },
                animationSpec = tween(
                    durationMillis = 400,
                    easing = DecelerateEasing(),
                ),
            ) togetherWith fadeOut(
                animationSpec = tween(
                    durationMillis = 260,
                    easing = DecelerateEasing(),
                ),
            )
        },
        label = "hitokoto",
    ) { current ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (current) {
                AboutHitokotoState.Loading -> {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                AboutHitokotoState.Unavailable -> {
                    Text(
                        text = stringResource(R.string.about_hitokoto_unavailable),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                        textAlign = TextAlign.Center,
                    )
                }

                is AboutHitokotoState.Content -> {
                    Text(
                        text = stringResource(R.string.about_hitokoto_quote, current.text),
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                        style = MiuixTheme.textStyles.body1,
                        textAlign = TextAlign.Center,
                    )
                    if (current.source.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.about_hitokoto_source, current.source),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.body2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppMark(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .size(72.dp)
            .clip(shape)
            .background(MiuixTheme.colorScheme.primary)
            .border(
                width = 1.dp,
                color = MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                shape = shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.about_app_mark),
            color = MiuixTheme.colorScheme.onPrimary,
            style = MiuixTheme.textStyles.title2,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    SmallTitle(
        modifier = modifier,
        text = text,
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun AboutSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(content = { content() })
    }
}

@Composable
private fun InfoComponent(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    BasicComponent(
        modifier = modifier,
        title = title,
        summary = summary,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun LibraryComponent(
    library: AboutLibrary,
    modifier: Modifier = Modifier,
) {
    BasicComponent(
        modifier = modifier,
        title = library.name,
        summary = stringResource(
            R.string.about_library_detail,
            library.version,
            library.license.localizedLabel(),
        ),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun AboutLibraryLicense.localizedLabel(): String = when (this) {
    AboutLibraryLicense.Apache20 -> stringResource(R.string.about_license_apache_2)
    AboutLibraryLicense.Repository -> stringResource(R.string.about_license_repository)
    AboutLibraryLicense.VendorSdk -> stringResource(R.string.about_license_vendor_sdk)
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    AppTheme {
        AboutScreen(
            state = AboutState(),
            onAction = {},
        )
    }
}
