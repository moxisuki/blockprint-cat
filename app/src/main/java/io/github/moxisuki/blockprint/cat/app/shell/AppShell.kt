package io.github.moxisuki.blockprint.cat.app.shell

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import io.github.moxisuki.blockprint.cat.app.core.design.AppMotion
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppRoute
import io.github.moxisuki.blockprint.cat.app.core.navigation.rememberAppNavigator
import io.github.moxisuki.blockprint.cat.app.feature.about.AboutRoute
import io.github.moxisuki.blockprint.cat.app.feature.home.HomeRoute
import io.github.moxisuki.blockprint.cat.app.feature.settings.SettingsRoute
import io.github.moxisuki.blockprint.cat.R

@Composable
fun AppShell() {
    val navigator = rememberAppNavigator()
    val currentTopLevelRoute = navigator.currentTopLevelRoute
    val currentTitleRes = when (navigator.currentRoute) {
        AppRoute.Home -> R.string.nav_title_home
        AppRoute.Settings -> R.string.nav_title_settings
        AppRoute.About -> R.string.nav_title_about
    }
    val entryProvider = remember {
        entryProvider<NavKey> {
            entry(AppRoute.Home) {
                HomeRoute()
            }
            entry(AppRoute.Settings) {
                SettingsRoute(
                    onAboutClick = { navigator.navigate(AppRoute.About) },
                )
            }
            entry(AppRoute.About) {
                AboutRoute()
            }
        }
    }
    AppScaffold(
        title = stringResource(currentTitleRes),
        currentRoute = currentTopLevelRoute,
        navigationItems = AppTopLevelDestinations,
        onNavigationItemClick = navigator::navigateTopLevel,
        canNavigateBack = navigator.canNavigateBack,
        onNavigateBack = { navigator.navigateBack() },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AppTopLevelDestinations.forEach { destination ->
                key(destination.route) {
                    TopLevelNavDisplay(
                        selected = destination.route == currentTopLevelRoute,
                        entries = rememberDecoratedNavEntries(
                            backStack = navigator.backStackFor(destination.route),
                            entryProvider = entryProvider,
                        ),
                        onBack = { navigator.navigateBack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun TopLevelNavDisplay(
    selected: Boolean,
    entries: List<NavEntry<NavKey>>,
    onBack: () -> Unit,
) {
    val transition = updateTransition(
        targetState = selected,
        label = "topLevelTabVisibility",
    )
    val alpha = transition.animateFloat(
        transitionSpec = { AppMotion.topLevelVisibilitySpec() },
        label = "topLevelTabAlpha",
    ) { visible ->
        if (visible) 1f else 0f
    }
    val scale = transition.animateFloat(
        transitionSpec = { AppMotion.topLevelVisibilitySpec() },
        label = "topLevelTabScale",
    ) { visible ->
        if (visible) AppMotion.Scale.TabVisible else AppMotion.Scale.TabHidden
    }

    val content: @Composable () -> Unit = {
        NavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (selected) 1f else 0f)
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
            entries = entries,
            transitionSpec = { AppMotion.contentForwardTransition() },
            popTransitionSpec = { AppMotion.contentBackTransition() },
            transitionEffects = NavDisplayTransitionEffects.None,
            onBack = onBack,
        )
    }

    if (selected) {
        content()
    } else {
        CompositionLocalProvider(
            LocalNavigationEventDispatcherOwner provides rememberInactiveNavigationEventDispatcherOwner(),
        ) {
            content()
        }
    }
}

@Composable
private fun rememberInactiveNavigationEventDispatcherOwner(): NavigationEventDispatcherOwner {
    val owner = remember {
        object : NavigationEventDispatcherOwner {
            override val navigationEventDispatcher = NavigationEventDispatcher()
        }
    }

    DisposableEffect(owner) {
        onDispose {
            owner.navigationEventDispatcher.dispose()
        }
    }

    return owner
}
