package io.github.moxisuki.blockprint.cat.app.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.AppMotion
import io.github.moxisuki.blockprint.cat.app.core.navigation.AppTopLevelRoute
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
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
            FloatingNavigationBar(
                color = MiuixTheme.colorScheme.surfaceContainer,
            ) {
                navigationItems.forEach { item ->
                    FloatingNavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = { onNavigationItemClick(item.route) },
                        icon = item.icon,
                        label = stringResource(item.labelRes),
                    )
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
