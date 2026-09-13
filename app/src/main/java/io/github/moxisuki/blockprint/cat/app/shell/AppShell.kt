package io.github.moxisuki.blockprint.cat.app.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.AppMotion
import io.github.moxisuki.blockprint.cat.app.core.design.LocalAppWindowWidthSize
import io.github.moxisuki.blockprint.cat.app.core.design.rememberAppWindowWidthSize
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppRoute
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppTopLevelRoute
import io.github.moxisuki.blockprint.cat.app.core.navigation.rememberAppNavigator
import io.github.moxisuki.blockprint.cat.app.core.navigation.routeId
import io.github.moxisuki.blockprint.cat.app.feature.community.CommunityDetailRoute
import io.github.moxisuki.blockprint.cat.app.feature.about.AboutRoute
import io.github.moxisuki.blockprint.cat.app.feature.community.CommunityRoute
import io.github.moxisuki.blockprint.cat.app.feature.detail.BlueprintDetailRoute
import io.github.moxisuki.blockprint.cat.app.feature.debug.DebugRoute
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeRoute
import io.github.moxisuki.blockprint.cat.app.feature.preview.PreviewRoute
import io.github.moxisuki.blockprint.cat.app.feature.resourcepacks.ResourcePacksRoute
import io.github.moxisuki.blockprint.cat.app.feature.settings.SettingsRoute
import io.github.moxisuki.blockprint.cat.app.feature.settings.theme.ThemeSettingsRoute
import io.github.moxisuki.blockprint.cat.app.feature.tools.BlockPaintRoute
import io.github.moxisuki.blockprint.cat.app.feature.tools.ImageToBlueprintRoute
import io.github.moxisuki.blockprint.cat.app.feature.tools.TextToBlueprintRoute
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolDestination
import io.github.moxisuki.blockprint.cat.app.feature.tools.ToolsRoute

@Composable
fun AppShell() {
    val shellViewModel: AppShellViewModel = hiltViewModel()
    val communityEnabled by shellViewModel.communityEnabled.collectAsStateWithLifecycle()
    val navigator = rememberAppNavigator()
    val currentTopLevelRoute = navigator.currentTopLevelRoute
    val isPreview = navigator.currentRoute is AppRoute.Preview
    val showBottomBar = navigator.currentRoute is AppTopLevelRoute && !isPreview
    val navigationItems = remember(communityEnabled) {
        AppTopLevelDestinations.filter { item ->
            communityEnabled || item.route != AppRoute.Community
        }
    }
    var showThemeAppBarTitle by remember { mutableStateOf(false) }
    var showAboutAppBarTitle by remember { mutableStateOf(false) }
    var showResourcePacksAppBarTitle by remember { mutableStateOf(false) }

    LaunchedEffect(communityEnabled, currentTopLevelRoute) {
        if (!communityEnabled && currentTopLevelRoute == AppRoute.Community) {
            navigator.navigateTopLevel(AppRoute.Home)
        }
    }

    LaunchedEffect(navigator.currentRoute) {
        if (navigator.currentRoute != AppRoute.ThemeSettings) {
            showThemeAppBarTitle = false
        }
        if (navigator.currentRoute != AppRoute.About) {
            showAboutAppBarTitle = false
        }
        if (navigator.currentRoute !is AppRoute.ResourcePacks) {
            showResourcePacksAppBarTitle = false
        }
    }
    val currentTitle = when (navigator.currentRoute) {
        AppRoute.Home -> stringResource(R.string.nav_title_home)
        AppRoute.Tools -> stringResource(R.string.nav_title_tools)
        AppRoute.ImageToBlueprint -> stringResource(R.string.itb_title)
        AppRoute.TextToBlueprint -> stringResource(R.string.tool_text_to_blueprint)
        AppRoute.BlockPaint -> stringResource(R.string.tool_block_paint)
        AppRoute.Community -> stringResource(R.string.nav_title_community)
        is AppRoute.CommunityDetail -> stringResource(R.string.nav_title_detail_default)
        is AppRoute.BlueprintDetail -> stringResource(R.string.nav_title_detail_default)
        is AppRoute.Preview -> ""
        AppRoute.Settings -> stringResource(R.string.nav_title_settings)
        AppRoute.ThemeSettings -> if (showThemeAppBarTitle) {
            stringResource(R.string.theme_settings_title)
        } else {
            ""
        }
        AppRoute.About -> if (showAboutAppBarTitle) {
            stringResource(R.string.nav_title_about)
        } else {
            ""
        }
        AppRoute.Debug -> stringResource(R.string.nav_title_debug)
        is AppRoute.ResourcePacks -> if (showResourcePacksAppBarTitle) {
            stringResource(R.string.resourcepacks_title)
        } else {
            ""
        }
    }
    val entryProvider = remember {
        entryProvider<NavKey> {
            entry(AppRoute.Home) {
                HomeRoute(
                    onBlueprintClick = { blueprintId ->
                        navigator.navigate(AppRoute.BlueprintDetail(blueprintId))
                    },
                )
            }
            entry(AppRoute.Tools) {
                ToolsRoute(
                    onToolClick = { destination ->
                        navigator.navigate(
                            when (destination) {
                                ToolDestination.ImageToBlueprint -> AppRoute.ImageToBlueprint
                                ToolDestination.TextToBlueprint -> AppRoute.TextToBlueprint
                                ToolDestination.BlockPaint -> AppRoute.BlockPaint
                            },
                        )
                    },
                )
            }
            entry(AppRoute.ImageToBlueprint) {
                ImageToBlueprintRoute()
            }
            entry(AppRoute.TextToBlueprint) {
                TextToBlueprintRoute()
            }
            entry(AppRoute.BlockPaint) {
                BlockPaintRoute()
            }
            entry(AppRoute.Community) {
                CommunityRoute(
                    onBlueprintClick = { item ->
                        navigator.navigate(
                            AppRoute.CommunityDetail(
                                source = item.source.displayName,
                                blueprintId = item.id,
                                title = item.title,
                                author = item.author,
                                format = item.format,
                                description = item.description,
                                heat = item.heat,
                                downloads = item.downloads,
                                dimensions = item.dimensions,
                                sizeText = item.sizeText,
                                stress = item.stress,
                                updateTime = item.updateTime,
                                coverUrl = item.coverUrl,
                                tags = item.tags,
                                downloadable = item.downloadable,
                                webUrl = item.webUrl,
                                gameVersion = item.gameVersion,
                                versionNumber = item.versionNumber,
                                categoryName = item.categoryName,
                                formatLabel = item.formatLabel,
                            ),
                        )
                    },
                )
            }
            entry<AppRoute.CommunityDetail> { route ->
                CommunityDetailRoute(
                    source = route.source,
                    blueprintId = route.blueprintId,
                    title = route.title,
                    author = route.author,
                    format = route.format,
                    description = route.description,
                    heat = route.heat,
                    downloads = route.downloads,
                    dimensions = route.dimensions,
                    sizeText = route.sizeText,
                    stress = route.stress,
                    updateTime = route.updateTime,
                    coverUrl = route.coverUrl,
                    tags = route.tags,
                    downloadable = route.downloadable,
                    webUrl = route.webUrl,
                    gameVersion = route.gameVersion,
                    versionNumber = route.versionNumber,
                    categoryName = route.categoryName,
                    formatLabel = route.formatLabel,
                    onResourceNamespaceClick = { namespace ->
                        navigator.navigate(
                            AppRoute.ResourcePacks(
                                initialQuery = namespace.takeUnless { it == "minecraft" },
                            ),
                        )
                    },
                )
            }
            entry<AppRoute.BlueprintDetail> { route ->
                BlueprintDetailRoute(
                    blueprintId = route.blueprintId,
                    onPreviewClick = {
                        navigator.navigate(AppRoute.Preview(route.blueprintId))
                    },
                    onRegeneratePreviewClick = {
                        navigator.navigate(
                            AppRoute.Preview(
                                blueprintId = route.blueprintId,
                                forceRegenerate = true,
                            ),
                        )
                    },
                    onResourceNamespaceClick = { namespace ->
                        navigator.navigate(
                            AppRoute.ResourcePacks(
                                initialQuery = namespace.takeUnless { it == "minecraft" },
                            ),
                        )
                    },
                )
            }
            entry<AppRoute.Preview> { route ->
                PreviewRoute(
                    blueprintId = route.blueprintId,
                    forceRegenerate = route.forceRegenerate,
                    onBack = { navigator.navigateBack() },
                )
            }
            entry(AppRoute.Settings) {
                SettingsRoute(
                    onThemeClick = { navigator.navigate(AppRoute.ThemeSettings) },
                    onAboutClick = { navigator.navigate(AppRoute.About) },
                    onDebugClick = { navigator.navigate(AppRoute.Debug) },
                    onResourcePacksClick = { navigator.navigate(AppRoute.ResourcePacks()) },
                )
            }
            entry(AppRoute.ThemeSettings) {
                ThemeSettingsRoute(
                    onAppBarTitleVisibleChange = { visible ->
                        showThemeAppBarTitle = visible
                    },
                )
            }
            entry(AppRoute.About) {
                AboutRoute(
                    onAppBarTitleVisibleChange = { visible ->
                        showAboutAppBarTitle = visible
                    },
                )
            }
            entry(AppRoute.Debug) {
                DebugRoute()
            }
            entry<AppRoute.ResourcePacks> { route ->
                ResourcePacksRoute(
                    initialQuery = route.initialQuery,
                    onAppBarTitleVisibleChange = { visible ->
                        showResourcePacksAppBarTitle = visible
                    },
                )
            }
        }
    }
    val saveableStateHolder = rememberSaveableStateHolder()
    val windowWidthSize = rememberAppWindowWidthSize()
    CompositionLocalProvider(LocalAppWindowWidthSize provides windowWidthSize) {
        AppScaffold(
            title = currentTitle,
            currentRoute = currentTopLevelRoute,
            navigationItems = navigationItems,
            onNavigationItemClick = navigator::navigateTopLevel,
            windowWidthSize = windowWidthSize,
            canNavigateBack = navigator.canNavigateBack,
            showTopBar = !isPreview,
            showBottomBar = showBottomBar,
            onNavigateBack = { navigator.navigateBack() },
        ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection),
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(layoutDirection),
            bottom = 0.dp,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            AnimatedContent(
                targetState = currentTopLevelRoute,
                transitionSpec = {
                    AppMotion.topLevelTabTransition(
                        forward = topLevelRouteIndex(targetState, navigationItems) >=
                            topLevelRouteIndex(initialState, navigationItems),
                    )
                },
                label = "topLevelRouteContent",
            ) { route ->
                key(route) {
                    saveableStateHolder.SaveableStateProvider(route.routeId()) {
                        TopLevelNavDisplay(
                            entries = rememberDecoratedNavEntries(
                                backStack = navigator.backStackFor(route),
                                entryProvider = entryProvider,
                            ),
                            onBack = { navigator.navigateBack() },
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
private fun TopLevelNavDisplay(
    entries: List<NavEntry<NavKey>>,
    onBack: () -> Unit,
) {
    NavDisplay(
        modifier = Modifier.fillMaxSize(),
        entries = entries,
        transitionSpec = { AppMotion.contentForwardTransition() },
        popTransitionSpec = { AppMotion.contentBackTransition() },
        transitionEffects = NavDisplayTransitionEffects.None,
        onBack = onBack,
    )
}

private fun topLevelRouteIndex(
    route: AppTopLevelRoute,
    navigationItems: List<AppTopLevelDestination>,
): Int =
    navigationItems.indexOfFirst { it.route == route }.coerceAtLeast(0)
