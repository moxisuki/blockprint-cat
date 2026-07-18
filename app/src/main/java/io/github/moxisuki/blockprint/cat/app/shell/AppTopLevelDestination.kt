package io.github.moxisuki.blockprint.cat.app.shell

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppTopLevelRoute
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppRoute
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Settings

@Immutable
internal data class AppTopLevelDestination(
    val route: AppTopLevelRoute,
    @StringRes val titleRes: Int,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
)

internal val AppTopLevelDestinations = listOf(
    AppTopLevelDestination(
        route = AppRoute.Home,
        titleRes = R.string.nav_title_home,
        labelRes = R.string.bottom_nav_home,
        icon = MiuixIcons.File,
    ),
    AppTopLevelDestination(
        route = AppRoute.Settings,
        titleRes = R.string.nav_title_settings,
        labelRes = R.string.bottom_nav_settings,
        icon = MiuixIcons.Settings,
    ),
)
