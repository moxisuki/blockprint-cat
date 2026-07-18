package io.github.moxisuki.blockprint.cat.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

class AppNavigator internal constructor(
    private val topLevelBackStacks: Map<AppTopLevelRoute, MutableList<NavKey>>,
    private val selectedTopLevelRouteId: MutableState<String>,
) {
    val selectedTopLevelRoute: AppTopLevelRoute
        get() = appTopLevelRouteFromId(selectedTopLevelRouteId.value)

    val backStack: List<NavKey>
        get() = currentBackStack()

    val currentRoute: AppRoute
        get() = currentBackStack().last() as AppRoute

    val currentTopLevelRoute: AppTopLevelRoute
        get() = currentRoute.topLevelRoute()

    val canNavigateBack: Boolean
        get() = currentBackStack().size > 1

    fun backStackFor(route: AppTopLevelRoute): List<NavKey> =
        topLevelBackStacks.getValue(route)

    fun navigate(route: AppRoute) {
        if (route is AppTopLevelRoute) {
            navigateTopLevel(route)
            return
        }

        val topLevelRoute = route.topLevelRoute()
        selectedTopLevelRouteId.value = topLevelRoute.routeId()
        topLevelBackStacks.getValue(topLevelRoute).add(route)
    }

    fun navigateTopLevel(route: AppTopLevelRoute) {
        selectedTopLevelRouteId.value = route.routeId()
    }

    fun navigateBack(): Boolean {
        val backStack = currentBackStack()
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    private fun currentBackStack(): MutableList<NavKey> =
        topLevelBackStacks.getValue(selectedTopLevelRoute)
}

@Composable
fun rememberAppNavigator(
    startDestination: AppTopLevelRoute = AppRoute.Home,
): AppNavigator {
    val homeBackStack = rememberNavBackStack(AppRoute.Home)
    val settingsBackStack = rememberNavBackStack(AppRoute.Settings)
    val selectedTopLevelRouteId = rememberSaveable {
        androidx.compose.runtime.mutableStateOf(startDestination.routeId())
    }
    val topLevelBackStacks = remember(homeBackStack, settingsBackStack) {
        mapOf<AppTopLevelRoute, MutableList<NavKey>>(
            AppRoute.Home to homeBackStack,
            AppRoute.Settings to settingsBackStack,
        )
    }

    return remember(topLevelBackStacks, selectedTopLevelRouteId) {
        AppNavigator(
            topLevelBackStacks = topLevelBackStacks,
            selectedTopLevelRouteId = selectedTopLevelRouteId,
        )
    }
}
