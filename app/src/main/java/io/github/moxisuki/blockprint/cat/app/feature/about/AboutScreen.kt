package io.github.moxisuki.blockprint.cat.app.feature.about

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.AppMotion
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.design.appScrollEndHaptic
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val GITHUB_RELEASES_URL = "https://github.com/moxisuki/blockprint-cat/releases"

@Composable
internal fun AboutScreen(
    state: AboutState,
    onAction: (AboutAction) -> Unit,
    onAppBarTitleVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()
    val heroThresholdPx = with(LocalDensity.current) { 220.dp.roundToPx() }
    val heroRawProgress by remember(listState, heroThresholdPx) {
        derivedStateOf {
            when {
                listState.firstVisibleItemIndex > 0 -> 1f
                heroThresholdPx <= 0 -> 0f
                else -> (listState.firstVisibleItemScrollOffset / heroThresholdPx.toFloat())
                    .coerceIn(0f, 1f)
            }
        }
    }
    val heroProgress by animateFloatAsState(
        targetValue = heroRawProgress,
        animationSpec = AppMotion.topLevelVisibilitySpec(),
        label = "aboutHeroProgress",
    )
    val showAppBarTitle by remember {
        derivedStateOf { heroRawProgress > 0.62f }
    }
    LaunchedEffect(showAppBarTitle) {
        onAppBarTitleVisibleChange(showAppBarTitle)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .appScrollEndHaptic(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // ── Hero ──
        item(key = "hero") {
            AboutHero(
                hitokoto = state.hitokoto,
                showHitokoto = state.isChineseLocale,
                onRefreshHitokoto = { onAction(AboutAction.RefreshHitokoto) },
                scrollProgress = heroProgress,
            )
        }

        item(key = "app-info-title") {
            SectionTitle(
                text = stringResource(R.string.about_section_info),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item(key = "app-info") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                Column {
                    InfoBadgeRow(
                        label = stringResource(R.string.about_label_version),
                        value = stringResource(R.string.about_version, state.appVersionName),
                    )
                    SectionDivider()
                    InfoBadgeRow(
                        label = stringResource(R.string.about_engine_label),
                        value = stringResource(R.string.about_engine_version, state.blockPrintCoreVersion),
                    )
                    SectionDivider()
                    InfoBadgeRow(
                        label = stringResource(R.string.about_label_license),
                        value = stringResource(R.string.about_license_mit),
                    )
                    SectionDivider()
                    ClickableRow(
                        text = stringResource(R.string.about_view_releases),
                        onClick = { uriHandler.openUri(GITHUB_RELEASES_URL) },
                    )
                }
            }
        }

        // ── 外部链接 ──
        item(key = "links-title") {
            SectionTitle(
                text = stringResource(R.string.about_section_links),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "links") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                Column {
                    state.externalLinks.forEachIndexed { index, link ->
                        LinkRow(
                            title = link.title,
                            type = link.type,
                            onClick = { uriHandler.openUri(link.url) },
                        )
                        if (index != state.externalLinks.lastIndex) {
                            SectionDivider()
                        }
                    }
                }
            }
        }

        // ── 开源库 ──
        item(key = "libraries-title") {
            SectionTitle(
                text = stringResource(R.string.about_section_libraries),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item(key = "libraries") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(0.dp),
            ) {
                Column {
                    state.libraries.forEachIndexed { index, library ->
                        LibraryRow(
                            library = library,
                            onClick = { uriHandler.openUri(library.url) },
                        )
                        if (index != state.libraries.lastIndex) {
                            SectionDivider()
                        }
                    }
                }
            }
        }

        item(key = "bottom-space") {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Hitokoto
// ═══════════════════════════════════════════════════════════

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
            .clickable(
                enabled = state !is AboutHitokotoState.Loading,
                onClick = onRefresh,
            )
            .padding(horizontal = 16.dp),
        transitionSpec = {
            (
                fadeIn(
                    initialAlpha = 0.72f,
                    animationSpec = AppMotion.fadeEnterSpec(320),
                ) + slideInVertically(
                    initialOffsetY = { it / 6 },
                    animationSpec = tween(durationMillis = 320),
                ) + scaleIn(
                    initialScale = 0.985f,
                    animationSpec = AppMotion.topLevelEnterSpec(320),
                )
                ) togetherWith (
                fadeOut(
                    targetAlpha = 0.0f,
                    animationSpec = AppMotion.fadeExitSpec(180),
                ) + scaleOut(
                    targetScale = 1.01f,
                    animationSpec = AppMotion.fadeExitSpec(180),
                )
                ) using SizeTransform(clip = false)
        },
        label = "hitokoto",
    ) { current ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (current) {
                AboutHitokotoState.Loading -> HitokotoLoadingDots()
                AboutHitokotoState.Unavailable -> Text(
                    text = stringResource(R.string.about_hitokoto_unavailable),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.Center,
                )
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
private fun HitokotoLoadingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hitokotoLoading")
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha = transition.animateFloat(
                initialValue = 0.32f,
                targetValue = 0.88f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 560,
                        delayMillis = index * 120,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "hitokotoLoadingAlpha$index",
            )
            val scale = transition.animateFloat(
                initialValue = 0.82f,
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 560,
                        delayMillis = index * 120,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "hitokotoLoadingScale$index",
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .graphicsLayer {
                        this.alpha = alpha.value
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .clip(RoundedCornerShape(50))
                    .background(MiuixTheme.colorScheme.primary),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
//  Shared widgets
// ═══════════════════════════════════════════════════════════

@Composable
private fun AboutHero(
    hitokoto: AboutHitokotoState,
    showHitokoto: Boolean,
    onRefreshHitokoto: () -> Unit,
    scrollProgress: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val disappearProgress = ((scrollProgress - 0.18f) / 0.82f).coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 430.dp)
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                alpha = 1f - disappearProgress
                scaleX = 1f - scrollProgress * 0.08f
                scaleY = 1f - scrollProgress * 0.08f
                translationY = with(density) { (-72).dp.toPx() } * scrollProgress
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_default),
            contentDescription = null,
            modifier = Modifier.size(150.dp),
        )
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.app_name),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.title1.copy(fontSize = 42.sp),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (showHitokoto) {
            HitokotoQuote(
                state = hitokoto,
                onRefresh = onRefreshHitokoto,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        text = text,
        color = MiuixTheme.colorScheme.primary,
        style = MiuixTheme.textStyles.body2,
    )
}

@Composable
private fun InfoBadgeRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            modifier = Modifier.weight(1f),
        )
        val badgeShape = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier
                .clip(badgeShape)
                .background(MiuixTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = value,
                color = MiuixTheme.colorScheme.onSecondaryContainer,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

// ── External link rows ──

@Composable
private fun LinkRow(title: String, type: AboutExternalLinkType, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MiuixTheme.textStyles.body1)
        Spacer(modifier = Modifier.width(8.dp))
        val badgeShape = RoundedCornerShape(4.dp)
        Box(
            modifier = Modifier
                .clip(badgeShape)
                .background(MiuixTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
            Text(
                text = type.name,
                color = MiuixTheme.colorScheme.onSecondaryContainer,
                style = MiuixTheme.textStyles.body2,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LibraryRow(library: AboutLibrary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = library.name, style = MiuixTheme.textStyles.body1)
                Spacer(modifier = Modifier.width(8.dp))
                LicenseBadge(license = library.license)
            }
            Text(
                text = stringResource(R.string.about_version, library.version),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LicenseBadge(license: AboutLibraryLicense) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text = license.localizedLabel(),
            color = MiuixTheme.colorScheme.onSecondaryContainer,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
private fun ClickableRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = MiuixTheme.textStyles.body1)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun AboutLibraryLicense.localizedLabel(): String = when (this) {
    AboutLibraryLicense.Apache20 -> stringResource(R.string.about_license_apache_2)
    AboutLibraryLicense.MIT -> stringResource(R.string.about_license_mit)
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
    PreviewAppTheme {
        AboutScreen(
            state = AboutState(),
            onAction = {},
            onAppBarTitleVisibleChange = {},
        )
    }
}
