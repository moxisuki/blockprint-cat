package io.github.moxisuki.blockprint.cat.app.core.navigation

import androidx.navigation3.runtime.NavKey
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppTopLevelRoute

    @Serializable
    data object Community : AppTopLevelRoute

    @Serializable
    data object Tools : AppTopLevelRoute

    @Serializable
    data object ImageToBlueprint : AppRoute

    @Serializable
    data object TextToBlueprint : AppRoute

    @Serializable
    data object BlockPaint : AppRoute

    @Serializable
    data class CommunityDetail(
        val source: String,
        val blueprintId: String,
        val title: String,
        val author: String,
        val format: BlueprintFormat,
        val description: String,
        val heat: Int? = null,
        val downloads: Int? = null,
        val dimensions: String? = null,
        val sizeText: String? = null,
        val stress: String? = null,
        val updateTime: String = "",
        val coverUrl: String? = null,
        val tags: List<String> = emptyList(),
        val downloadable: Boolean = true,
        val webUrl: String? = null,
        val gameVersion: String? = null,
        val versionNumber: Int = 1,
        val categoryName: String? = null,
        val formatLabel: String? = null,
    ) : AppRoute

    @Serializable
    data object Settings : AppTopLevelRoute

    @Serializable
    data object ThemeSettings : AppRoute

    @Serializable
    data class BlueprintDetail(val blueprintId: String) : AppRoute

    @Serializable
    data class Preview(
        val blueprintId: String,
        val forceRegenerate: Boolean = false,
    ) : AppRoute

    @Serializable
    data object About : AppRoute

    @Serializable
    data object Debug : AppRoute

    @Serializable
    data class ResourcePacks(val initialQuery: String? = null) : AppRoute
}

sealed interface AppTopLevelRoute : AppRoute

internal val AppTopLevelRoutes = listOf<AppTopLevelRoute>(
    AppRoute.Home,
    AppRoute.Tools,
    AppRoute.Community,
    AppRoute.Settings,
)

internal fun AppTopLevelRoute.routeId(): String = when (this) {
    AppRoute.Home -> "home"
    AppRoute.Tools -> "tools"
    AppRoute.Community -> "community"
    AppRoute.Settings -> "settings"
}

internal fun appTopLevelRouteFromId(routeId: String): AppTopLevelRoute =
    AppTopLevelRoutes.firstOrNull { it.routeId() == routeId } ?: AppRoute.Home

fun AppRoute.topLevelRoute(): AppTopLevelRoute = when (this) {
    AppRoute.Home -> AppRoute.Home
    AppRoute.Tools,
    AppRoute.ImageToBlueprint,
    AppRoute.TextToBlueprint,
    AppRoute.BlockPaint,
    -> AppRoute.Tools
    AppRoute.Community,
    is AppRoute.CommunityDetail,
    -> AppRoute.Community
    is AppRoute.BlueprintDetail -> AppRoute.Home
    is AppRoute.Preview -> AppRoute.Home
    AppRoute.Settings,
    AppRoute.ThemeSettings,
    AppRoute.About,
    AppRoute.Debug,
    is AppRoute.ResourcePacks,
    -> AppRoute.Settings
}
