package io.github.moxisuki.blockprint.cat.app.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppTopLevelRoute

    @Serializable
    data object Settings : AppTopLevelRoute

    @Serializable
    data object ThemeSettings : AppRoute

    @Serializable
    data class BlueprintDetail(val blueprintId: String) : AppRoute

    @Serializable
    data object About : AppRoute

    @Serializable
    data object Debug : AppRoute
}

sealed interface AppTopLevelRoute : AppRoute

internal val AppTopLevelRoutes = listOf<AppTopLevelRoute>(
    AppRoute.Home,
    AppRoute.Settings,
)

internal fun AppTopLevelRoute.routeId(): String = when (this) {
    AppRoute.Home -> "home"
    AppRoute.Settings -> "settings"
}

internal fun appTopLevelRouteFromId(routeId: String): AppTopLevelRoute =
    AppTopLevelRoutes.firstOrNull { it.routeId() == routeId } ?: AppRoute.Home

fun AppRoute.topLevelRoute(): AppTopLevelRoute = when (this) {
    AppRoute.Home -> AppRoute.Home
    is AppRoute.BlueprintDetail -> AppRoute.Home
    AppRoute.Settings,
    AppRoute.ThemeSettings,
    AppRoute.About,
    AppRoute.Debug,
    -> AppRoute.Settings
}
