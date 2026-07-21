package io.github.moxisuki.blockprint.cat.app.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.AppMotion
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppTopLevelRoute
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun AppScaffold(
    title: String,
    currentRoute: AppTopLevelRoute,
    navigationItems: List<AppTopLevelDestination>,
    onNavigationItemClick: (AppTopLevelRoute) -> Unit,
    modifier: Modifier = Modifier,
    canNavigateBack: Boolean = false,
    showBottomBar: Boolean = true,
    onNavigateBack: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val topBarState = AppTopBarState(
        title = title,
        canNavigateBack = canNavigateBack,
    )

    Scaffold(
        modifier = modifier,
        containerColor = MiuixTheme.colorScheme.surface,
        topBar = {
            AnimatedContent(
                targetState = topBarState,
                transitionSpec = {
                    AppMotion.appBarTransition(showingChildPage = targetState.canNavigateBack)
                },
                label = "appTopBar",
            ) { state ->
                if (state.canNavigateBack) {
                    SmallTopAppBar(
                        title = state.title,
                        color = MiuixTheme.colorScheme.surface,
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = MiuixIcons.Back,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        title = state.title,
                        color = MiuixTheme.colorScheme.surface,
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = AppMotion.bottomBarEnterTransition(),
                exit = AppMotion.bottomBarExitTransition(),
                label = "appBottomBar",
            ) {
                FloatingNavigationBar(
                    color = MiuixTheme.colorScheme.surfaceContainer,
                ) {
                    navigationItems.forEach { item ->
                        AppFloatingNavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    onNavigationItemClick(item.route)
                                }
                            },
                            item = item,
                            label = stringResource(item.labelRes),
                        )
                    }
                }
            }
        },
        content = content,
    )
}

@Composable
private fun AppFloatingNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    item: AppTopLevelDestination,
    label: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val targetColor = appFloatingNavigationItemColor(
        selected = selected,
        isPressed = isPressed,
    )
    val tint by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = AppMotion.Duration.TabVisibilityMillis),
        label = "bottomNavItemTint",
    )
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            selected -> 1.06f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bottomNavItemScale",
    )

    Icon(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(10.dp)
            .size(28.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        imageVector = item.icon,
        contentDescription = label,
        tint = tint,
    )
}

@Composable
private fun appFloatingNavigationItemColor(
    selected: Boolean,
    isPressed: Boolean,
): Color {
    val base = MiuixTheme.colorScheme.onSurfaceContainer
    return when {
        isPressed && selected -> base.copy(alpha = 0.56f)
        isPressed -> base.copy(alpha = 0.62f)
        selected -> base
        else -> base.copy(alpha = 0.42f)
    }
}

@Immutable
private data class AppTopBarState(
    val title: String,
    val canNavigateBack: Boolean,
)
