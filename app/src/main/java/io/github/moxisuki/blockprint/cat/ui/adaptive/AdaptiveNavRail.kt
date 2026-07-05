package io.github.moxisuki.blockprint.cat.ui.adaptive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import io.github.moxisuki.blockprint.cat.R
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.moxisuki.blockprint.cat.ui.navigation.ElevatedNavRailItem
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes

private data class RailItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val railItems = listOf(
    RailItem(NavRoutes.HOME, R.string.bottom_nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    RailItem(NavRoutes.TOOLS, R.string.bottom_nav_tools, Icons.Filled.Construction, Icons.Outlined.Construction),
)

@Composable
fun AdaptiveNavRail(
    navController: NavController,
    communityEnabled: Boolean = true,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationRail(
        modifier = Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        header = { Spacer(Modifier.height(16.dp)) },
    ) {
        Spacer(Modifier.height(12.dp))
        railItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            val iconTint by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = 350f, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "navIconTint",
            )
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.labelRes),
                        tint = iconTint,
                    )
                },
                label = { Text(stringResource(item.labelRes), style = MaterialTheme.typography.labelSmall, color = iconTint) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
            )
        }

        // Connection — elevated circular button (always shown)
        ElevatedNavRailItem(
            selected = currentDestination?.hierarchy?.any { it.route == NavRoutes.CONNECTION } == true,
            onClick = {
                navController.navigate(NavRoutes.CONNECTION) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            },
            icon = Icons.Outlined.Computer,
            selectedIcon = Icons.Filled.Computer,
            label = stringResource(R.string.bottom_nav_connection),
        )

        // Community — flat slot, gated by communityEnabled
        if (communityEnabled) {
            val communitySelected = currentDestination?.hierarchy?.any { it.route == NavRoutes.COMMUNITY } == true
            val communityLabel = stringResource(R.string.bottom_nav_community)
            val iconTint by animateColorAsState(
                targetValue = if (communitySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = spring(stiffness = 350f, dampingRatio = Spring.DampingRatioNoBouncy),
                label = "navIconTintCommunity",
            )
            NavigationRailItem(
                icon = {
                    Icon(
                        imageVector = if (communitySelected) Icons.Filled.People else Icons.Outlined.People,
                        contentDescription = communityLabel,
                        tint = iconTint,
                    )
                },
                label = { Text(communityLabel, style = MaterialTheme.typography.labelSmall, color = iconTint) },
                selected = communitySelected,
                onClick = {
                    navController.navigate(NavRoutes.COMMUNITY) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
            )
        }

        // Settings — always shown
        val settingsSelected = currentDestination?.hierarchy?.any { it.route == NavRoutes.SETTINGS } == true
        val settingsLabel = stringResource(R.string.bottom_nav_settings)
        val settingsIconTint by animateColorAsState(
            targetValue = if (settingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = spring(stiffness = 350f, dampingRatio = Spring.DampingRatioNoBouncy),
            label = "navIconTintSettings",
        )
        NavigationRailItem(
            icon = {
                Icon(
                    imageVector = if (settingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = settingsLabel,
                    tint = settingsIconTint,
                )
            },
            label = { Text(settingsLabel, style = MaterialTheme.typography.labelSmall, color = settingsIconTint) },
            selected = settingsSelected,
            onClick = {
                navController.navigate(NavRoutes.SETTINGS) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            },
        )

        Spacer(Modifier.weight(1f))
    }
}
