package io.github.moxisuki.blockprint.cat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Construction
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
    BottomNavItem(NavRoutes.TOOLS, R.string.bottom_nav_tools, Icons.Filled.Construction, Icons.Outlined.Construction),
)

/**
 * Connection is rendered as an elevated circular button (see ElevatedNavBarItem)
 * rather than a flat BottomNavItem, so it lives outside the bottomNavItems list.
 */
internal const val CONNECTION_TAB_ROUTE = NavRoutes.CONNECTION