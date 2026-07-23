package io.github.moxisuki.blockprint.cat.app.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppRoute
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppTopLevelRoute
import io.github.moxisuki.blockprint.cat.app.core.navigation.rememberAppNavigator
import io.github.moxisuki.blockprint.cat.app.core.navigation.routeId
import io.github.moxisuki.blockprint.cat.app.feature.community.CommunityDetailRoute
import io.github.moxisuki.blockprint.cat.app.feature.about.AboutRoute
import io.github.moxisuki.blockprint.cat.app.feature.community.CommunityLoginRoute
import io.github.moxisuki.blockprint.cat.app.feature.community.CommunityRoute
import io.github.moxisuki.blockprint.cat.app.feature.detail.BlueprintDetailRoute
import io.github.moxisuki.blockprint.cat.app.feature.debug.DebugRoute
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeRoute
import io.github.moxisuki.blockprint.cat.app.feature.resourcepacks.ResourcePacksRoute
import io.github.moxisuki.blockprint.cat.app.feature.settings.SettingsRoute
import io.github.moxisuki.blockprint.cat.app.feature.settings.theme.ThemeSettingsRoute

@Composable
fun AppShell() {
    val shellViewModel: AppShellViewModel = hiltViewModel()
    val communityEnabled by shellViewModel.communityEnabled.collectAsStateWithLifecycle()
    val navigator = rememberAppNavigator()
    val currentTopLevelRoute = navigator.currentTopLevelRoute
    val showBottomBar = navigator.currentRoute is AppTopLevelRoute
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
        if (navigator.currentRoute != AppRoute.ResourcePacks) {
            showResourcePacksAppBarTitle = false
        }
    }
    val currentTitle = when (navigator.currentRoute) {
        AppRoute.Home -> stringResource(R.string.nav_title_home)
        AppRoute.Community -> stringResource(R.string.nav_title_community)
        AppRoute.CommunityLogin -> ""
        is AppRoute.CommunityDetail -> stringResource(R.string.nav_title_detail_default)
        is AppRoute.BlueprintDetail -> stringResource(R.string.nav_title_detail_default)
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
        AppRoute.ResourcePacks -> if (showResourcePacksAppBarTitle) {
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
            entry(AppRoute.Community) {
                CommunityRoute(
                    onLoginClick = { navigator.navigate(AppRoute.CommunityLogin) },
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
                            ),
                        )
                    },
                )
            }
            entry(AppRoute.CommunityLogin) {
                CommunityLoginRoute(
                    onLoginSuccess = { navigator.navigateBack() },
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
                )
            }
            entry<AppRoute.BlueprintDetail> { route ->
                BlueprintDetailRoute(
                    blueprintId = route.blueprintId,
                )
            }
            entry(AppRoute.Settings) {
                SettingsRoute(
                    onThemeClick = { navigator.navigate(AppRoute.ThemeSettings) },
                    onAboutClick = { navigator.navigate(AppRoute.About) },
                    onDebugClick = { navigator.navigate(AppRoute.Debug) },
                    onResourcePacksClick = { navigator.navigate(AppRoute.ResourcePacks) },
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
            entry(AppRoute.ResourcePacks) {
                ResourcePacksRoute(
                    onAppBarTitleVisibleChange = { visible ->
                        showResourcePacksAppBarTitle = visible
                    },
                )
            }
        }
    }
    val saveableStateHolder = rememberSaveableStateHolder()
    AppScaffold(
        title = currentTitle,
        currentRoute = currentTopLevelRoute,
        navigationItems = navigationItems,
        onNavigationItemClick = navigator::navigateTopLevel,
        canNavigateBack = navigator.canNavigateBack,
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
