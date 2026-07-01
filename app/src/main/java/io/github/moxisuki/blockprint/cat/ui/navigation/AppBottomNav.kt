package io.github.moxisuki.blockprint.cat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.moxisuki.blockprint.cat.R

/**
 * Bottom-nav descriptor used by the compact (phone) layout branch in
 * [io.github.moxisuki.blockprint.cat.ui.navigation.AppNavGraph].
 *
 * Behaviour is identical to the original `private data class BottomNavItem`
 * + `private val bottomNavItems` that lived inside MainActivity.kt before
 * the MainActivity split; only the visibility changed to `internal`.
 */
internal data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

internal val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.HOME, R.string.bottom_nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(NavRoutes.CONNECTION, R.string.bottom_nav_connection, Icons.Filled.Computer, Icons.Outlined.Computer),
    BottomNavItem(NavRoutes.COMMUNITY, R.string.bottom_nav_community, Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(NavRoutes.SETTINGS, R.string.bottom_nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)