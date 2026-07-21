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
import io.github.moxisuki.blockprint.cat.app.feature.about.AboutRoute
import io.github.moxisuki.blockprint.cat.app.feature.detail.BlueprintDetailRoute
import io.github.moxisuki.blockprint.cat.app.feature.debug.DebugRoute
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeRoute
import io.github.moxisuki.blockprint.cat.app.feature.settings.SettingsRoute
import io.github.moxisuki.blockprint.cat.app.feature.settings.theme.ThemeSettingsRoute

@Composable
fun AppShell() {
    val navigator = rememberAppNavigator()
    val currentTopLevelRoute = navigator.currentTopLevelRoute
    val showBottomBar = navigator.currentRoute is AppTopLevelRoute
    var showThemeAppBarTitle by remember { mutableStateOf(false) }
    var showAboutAppBarTitle by remember { mutableStateOf(false) }
    LaunchedEffect(navigator.currentRoute) {
        if (navigator.currentRoute != AppRoute.ThemeSettings) {
            showThemeAppBarTitle = false
        }
        if (navigator.currentRoute != AppRoute.About) {
            showAboutAppBarTitle = false
        }
    }
    val currentTitle = when (navigator.currentRoute) {
        AppRoute.Home -> stringResource(R.string.nav_title_home)
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
        }
    }
    val saveableStateHolder = rememberSaveableStateHolder()
    AppScaffold(
        title = currentTitle,
        currentRoute = currentTopLevelRoute,
        navigationItems = AppTopLevelDestinations,
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
                        forward = topLevelRouteIndex(targetState) >= topLevelRouteIndex(initialState),
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

private fun topLevelRouteIndex(route: AppTopLevelRoute): Int =
    AppTopLevelDestinations.indexOfFirst { it.route == route }.coerceAtLeast(0)
