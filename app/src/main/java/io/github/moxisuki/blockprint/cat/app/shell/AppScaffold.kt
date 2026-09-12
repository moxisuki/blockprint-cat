package io.github.moxisuki.blockprint.cat.app.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.AppMotion
import io.github.moxisuki.blockprint.cat.app.core.design.AppWindowWidthSize
import io.github.moxisuki.blockprint.cat.app.core.design.appMaxContentWidth
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppTopLevelRoute
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.basic.VerticalDivider
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val AppRailWidth = 80.dp
private val AppRailItemWidth = 64.dp
private val AppRailPillSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessHigh,
)

@Composable
internal fun AppScaffold(
    title: String,
    currentRoute: AppTopLevelRoute,
    navigationItems: List<AppTopLevelDestination>,
    onNavigationItemClick: (AppTopLevelRoute) -> Unit,
    modifier: Modifier = Modifier,
    windowWidthSize: AppWindowWidthSize = AppWindowWidthSize.Compact,
    canNavigateBack: Boolean = false,
    showTopBar: Boolean = true,
    showBottomBar: Boolean = true,
    onNavigateBack: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val useNavigationRail = windowWidthSize.isWide
    if (useNavigationRail) {
        Row(modifier = modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showTopBar,
                enter = AppMotion.railEnterTransition(),
                exit = AppMotion.railExitTransition(),
                label = "appNavigationRail",
            ) {
                AppNavigationRail(
                    currentRoute = currentRoute,
                    navigationItems = navigationItems,
                    onNavigationItemClick = onNavigationItemClick,
                )
            }
            AppScaffoldContent(
                title = title,
                canNavigateBack = canNavigateBack,
                showTopBar = showTopBar,
                showBottomBar = false,
                navigationItems = navigationItems,
                currentRoute = currentRoute,
                onNavigationItemClick = onNavigationItemClick,
                onNavigateBack = onNavigateBack,
                modifier = Modifier.weight(1f),
                content = content,
            )
        }
    } else {
        AppScaffoldContent(
            title = title,
            canNavigateBack = canNavigateBack,
            showTopBar = showTopBar,
            showBottomBar = showBottomBar,
            navigationItems = navigationItems,
            currentRoute = currentRoute,
            onNavigationItemClick = onNavigationItemClick,
            onNavigateBack = onNavigateBack,
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
private fun AppScaffoldContent(
    title: String,
    canNavigateBack: Boolean,
    showTopBar: Boolean,
    showBottomBar: Boolean,
    navigationItems: List<AppTopLevelDestination>,
    currentRoute: AppTopLevelRoute,
    onNavigationItemClick: (AppTopLevelRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
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
            if (showTopBar) {
                AnimatedContent(
                    targetState = topBarState,
                    modifier = Modifier.appMaxContentWidth(),
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
                        val label = stringResource(item.labelRes)
                        FloatingNavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    onNavigationItemClick(item.route)
                                }
                            },
                            icon = item.icon,
                            label = label,
                        )
                    }
                }
            }
        },
        content = content,
    )
}

@Immutable
private data class AppTopBarState(
    val title: String,
    val canNavigateBack: Boolean,
)

@Composable
private fun AppNavigationRail(
    currentRoute: AppTopLevelRoute,
    navigationItems: List<AppTopLevelDestination>,
    onNavigationItemClick: (AppTopLevelRoute) -> Unit,
) {
    Row {
        var railCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(AppRailWidth)
                .background(MiuixTheme.colorScheme.surface)
                .safeDrawingPadding()
                .padding(top = 26.dp)
                .onGloballyPositioned { railCoordinates = it },
        ) {
            val selectedIndex = navigationItems
                .indexOfFirst { it.route == currentRoute }
                .coerceAtLeast(0)
            val itemTops = remember { mutableStateMapOf<Int, Float>() }
            val itemHeights = remember { mutableStateMapOf<Int, Int>() }
            val targetTop = itemTops[selectedIndex]
            val targetHeight = itemHeights[selectedIndex]
            if (targetTop != null && targetHeight != null) {
                val pillTop by animateFloatAsState(
                    targetValue = targetTop,
                    animationSpec = AppRailPillSpring,
                    label = "railPillTop",
                )
                val pillHeight by animateFloatAsState(
                    targetValue = targetHeight.toFloat(),
                    animationSpec = AppRailPillSpring,
                    label = "railPillHeight",
                )
                Box(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(
                                Constraints.fixed(
                                    AppRailItemWidth.roundToPx(),
                                    pillHeight.roundToInt().coerceAtLeast(1),
                                ),
                            )
                            layout(placeable.width, placeable.height) {
                                placeable.placeRelative(
                                    (constraints.maxWidth - placeable.width) / 2,
                                    pillTop.roundToInt(),
                                )
                            }
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(MiuixTheme.colorScheme.primary),
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                navigationItems.forEachIndexed { index, item ->
                    AppNavigationRailItem(
                        selected = index == selectedIndex,
                        onClick = {
                            if (index != selectedIndex) {
                                onNavigationItemClick(item.route)
                            }
                        },
                        icon = item.icon,
                        label = stringResource(item.labelRes),
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            val rail = railCoordinates ?: return@onGloballyPositioned
                            itemTops[index] = rail.localBoundingBoxOf(coordinates).top
                            itemHeights[index] = coordinates.size.height
                        },
                    )
                }
            }
        }
        VerticalDivider()
    }
}

@Composable
private fun AppNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(AppMotion.Duration.TabExitMillis),
        label = "railItemPressScale",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MiuixTheme.colorScheme.onPrimary
        } else {
            MiuixTheme.colorScheme.onSurfaceContainer
        },
        animationSpec = tween(AppMotion.Duration.TabVisibilityMillis),
        label = "railItemContent",
    )
    Column(
        modifier = modifier
            .padding(vertical = 5.dp)
            .width(AppRailItemWidth)
            .scale(pressScale)
            .clip(RoundedCornerShape(16.dp))
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
