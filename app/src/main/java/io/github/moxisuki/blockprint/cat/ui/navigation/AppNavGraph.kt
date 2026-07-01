package io.github.moxisuki.blockprint.cat.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.community.CommunitySource
import io.github.moxisuki.blockprint.cat.ui.adaptive.AdaptiveNavRail
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeViewModel
import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionScreen
import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionState
import io.github.moxisuki.blockprint.cat.ui.community.CommunityDetailScreen
import io.github.moxisuki.blockprint.cat.ui.community.CommunityScreen
import io.github.moxisuki.blockprint.cat.ui.community.CommunityViewModel
import io.github.moxisuki.blockprint.cat.ui.community.LoginWebViewScreen
import io.github.moxisuki.blockprint.cat.ui.component.AppTopBar
import io.github.moxisuki.blockprint.cat.ui.detail.BlueprintDetailScreen
import io.github.moxisuki.blockprint.cat.ui.home.HomeScreen
import io.github.moxisuki.blockprint.cat.ui.preview.PreviewScreen
import io.github.moxisuki.blockprint.cat.ui.qr.QrScannerScreen
import io.github.moxisuki.blockprint.cat.ui.render.RenderManagerScreen
import io.github.moxisuki.blockprint.cat.ui.settings.AboutScreen
import io.github.moxisuki.blockprint.cat.ui.settings.CommunitySettingsScreen
import io.github.moxisuki.blockprint.cat.ui.settings.SettingsScreen
import io.github.moxisuki.blockprint.cat.ui.settings.TermsScreen

/**
 * Layout dispatcher that picks the Pad (≥840dp) or Compact (<840dp) branch.
 *
 * Behaviour is identical to the inline Pad/Compact branches that lived at
 * lines 410–881 of MainActivity.kt before the MainActivity split. Each
 * branch re-derives the destination flags, connection state, and community
 * state from the supplied navigation controller / VMs so this function
 * can be moved out of MainActivity without coupling it to the orchestrator.
 *
 * The orchestrator ([io.github.moxisuki.blockprint.cat.BlockPrintCatAppContent])
 * keeps responsibility for: theme collection, snackbar plumbing,
 * `reconnectIfNeeded` lifecycle wiring, status bar tint side-effect,
 * and bridge events → snackbar mapping. None of those are redone here.
 */
@Composable
internal fun AppNavGraph(
    navController: NavHostController,
    bridgeVm: BridgeViewModel,
    communityVm: CommunityViewModel,
    snackbarHostState: SnackbarHostState,
    onImportSafer: (Uri) -> Unit,
    onRefresh: (Int) -> Unit,
    onRequestSafFolder: () -> Unit,
    isPreviewFullscreen: Boolean,
    onPreviewFullscreenChange: (Boolean) -> Unit,
    detailTitle: String,
    onDetailTitleChange: (String) -> Unit,
    communityEnabled: Boolean,
) {
    val isExpanded = LocalConfiguration.current.screenWidthDp >= 840
    if (isExpanded) {
        PadLayout(
            navController = navController,
            bridgeVm = bridgeVm,
            communityVm = communityVm,
            snackbarHostState = snackbarHostState,
            onImportSafer = onImportSafer,
            onRefresh = onRefresh,
            isPreviewFullscreen = isPreviewFullscreen,
            onPreviewFullscreenChange = onPreviewFullscreenChange,
            detailTitle = detailTitle,
            onDetailTitleChange = onDetailTitleChange,
            communityEnabled = communityEnabled,
        )
    } else {
        CompactLayout(
            navController = navController,
            bridgeVm = bridgeVm,
            communityVm = communityVm,
            snackbarHostState = snackbarHostState,
            onImportSafer = onImportSafer,
            onRefresh = onRefresh,
            onRequestSafFolder = onRequestSafFolder,
            isPreviewFullscreen = isPreviewFullscreen,
            onPreviewFullscreenChange = onPreviewFullscreenChange,
            detailTitle = detailTitle,
            onDetailTitleChange = onDetailTitleChange,
            communityEnabled = communityEnabled,
        )
    }
}

/**
 * Pad / expanded-width layout (≥840dp wide).
 *
 * Hosts the two-pane master-detail composition:
 *   - HOME: HomeScreen left, BlueprintDetailContent right
 *   - COMMUNITY: CommunityScreen left, CommunityDetailContent right
 *   - other routes: full-width
 *
 * Holds the Pad-only `selectedBlueprintUuid` / `selectedCommunityPair`
 * state so the two panes can stay in sync without round-tripping
 * through the orchestrator.
 */
@Composable
private fun PadLayout(
    navController: NavHostController,
    bridgeVm: BridgeViewModel,
    communityVm: CommunityViewModel,
    snackbarHostState: SnackbarHostState,
    onImportSafer: (Uri) -> Unit,
    onRefresh: (Int) -> Unit,
    isPreviewFullscreen: Boolean,
    onPreviewFullscreenChange: (Boolean) -> Unit,
    detailTitle: String,
    onDetailTitleChange: (String) -> Unit,
    communityEnabled: Boolean,
) {
    var selectedBlueprintUuid by remember { mutableStateOf<String?>(null) }
    var selectedCommunityPair by remember { mutableStateOf<Pair<CommunitySource, String>?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isDetail = currentDestination?.route?.startsWith(NavRoutes.DETAIL) == true
    val isPreview = currentDestination?.route?.startsWith(NavRoutes.PREVIEW) == true
    val isSettings = currentDestination?.route == NavRoutes.SETTINGS
    val isRender = currentDestination?.route?.startsWith(NavRoutes.RENDER) == true
    val isCommunityDetail = currentDestination?.route?.startsWith(NavRoutes.COMMUNITY_DETAIL) == true
    val isCommunityLogin = currentDestination?.route == NavRoutes.COMMUNITY_LOGIN
    val isHome = currentDestination?.route == NavRoutes.HOME
    val isAbout = currentDestination?.route == NavRoutes.ABOUT
    val isTerms = currentDestination?.route == NavRoutes.TERMS
    val isQrScanner = currentDestination?.route == NavRoutes.QR_SCANNER
    val isCommunitySettings = currentDestination?.route == NavRoutes.COMMUNITY_SETTINGS
    val showBackButton = isDetail || isRender || isPreview || isCommunityDetail || isCommunityLogin || isAbout || isTerms || isQrScanner || isCommunitySettings

    val topBarTitle = when {
        isDetail -> detailTitle
        currentDestination?.route == NavRoutes.HOME -> stringResource(R.string.nav_title_home)
        currentDestination?.route == NavRoutes.COMMUNITY -> stringResource(R.string.nav_title_community)
        currentDestination?.route == NavRoutes.COMMUNITY_SETTINGS -> stringResource(R.string.nav_title_community_settings)
        currentDestination?.route == NavRoutes.CONNECTION -> stringResource(R.string.nav_title_connection)
        isCommunityDetail -> stringResource(R.string.nav_title_community_detail)
        isCommunityLogin -> stringResource(R.string.nav_title_community_login)
        isRender -> stringResource(R.string.nav_title_render)
        isPreview -> stringResource(R.string.nav_title_preview)
        isAbout -> stringResource(R.string.nav_title_about)
        isTerms -> stringResource(R.string.nav_title_terms)
        isQrScanner -> stringResource(R.string.nav_title_qr_scanner)
        else -> ""
    }

    val connectionState by bridgeVm.connectionState.collectAsState()
    val isBridgeConnected = connectionState is ConnectionState.Connected
    val isBridgeConnecting = connectionState is ConnectionState.Connecting

    val communityState by communityVm.state.collectAsState()
    val padOnCommunity = currentDestination?.route == NavRoutes.COMMUNITY
    val padActive = communityState.active

    val padFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { onImportSafer(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isPreviewFullscreen) {
                if (currentDestination?.route == NavRoutes.HOME) {
                    AppTopBar(
                        title = topBarTitle,
                        showBackButton = showBackButton,
                        showCommunityActions = padOnCommunity && padActive.ready,
                        showLogout = communityState.currentSource == CommunitySource.MCS,
                        onCommunity = padOnCommunity,
                        onToggleFilter = { communityVm.toggleFilter() },
                        onToggleHeatSort = { communityVm.toggleHeatSort() },
                        onRefresh = { communityVm.refresh() },
                        onLogout = { communityVm.logout(); communityVm.refreshLoginState() },
                        onBack = { navController.popBackStack() },
                        isHeatSort = padActive.heatSort,
                        actions = {
                            IconButton(onClick = { navController.navigate(NavRoutes.CONNECTION) }) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape)
                                        .background(
                                            when {
                                                isBridgeConnected -> Color(0xFF4CAF50)
                                                isBridgeConnecting -> Color(0xFFFFC107)
                                                else -> Color(0xFF9E9E9E)
                                            }
                                        )
                                )
                            }
                        },
                    )
                } else {
                    AppTopBar(
                        title = topBarTitle,
                        showBackButton = showBackButton,
                        showCommunityActions = padOnCommunity && padActive.ready,
                        showLogout = communityState.currentSource == CommunitySource.MCS,
                        onCommunity = padOnCommunity,
                        onToggleFilter = { communityVm.toggleFilter() },
                        onToggleHeatSort = { communityVm.toggleHeatSort() },
                        onRefresh = { communityVm.refresh() },
                        onLogout = { communityVm.logout(); communityVm.refreshLoginState() },
                        onBack = { navController.popBackStack() },
                        isHeatSort = padActive.heatSort,
                    )
                }
            }
            Row(modifier = Modifier.weight(1f)) {
                if (!isPreviewFullscreen) {
                    AdaptiveNavRail(navController = navController, communityEnabled = communityEnabled)
                }
                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.HOME,
                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface),
                ) {
                    composable(NavRoutes.HOME) {
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(0.4f)) {
                                HomeScreen(navController = navController, bridgeVm = bridgeVm, snackbarHostState = snackbarHostState, onRequestSafFolder = {}, onRefresh = onRefresh, onBlueprintSelected = remember { { bp -> selectedBlueprintUuid = bp.uuid } })
                            }
                            HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                            Box(Modifier.weight(0.6f)) {
                                AnimatedContent(
                                    targetState = selectedBlueprintUuid,
                                    transitionSpec = {
                                        if (targetState != null) {
                                            (slideInHorizontally(AnimSpec.padSlide) { it / 4 } + fadeIn(AnimSpec.padFade))
                                                .togetherWith(slideOutHorizontally(AnimSpec.padSlideOut) { -it / 4 } + fadeOut(AnimSpec.padFadeOut))
                                        } else {
                                            (slideInHorizontally(AnimSpec.padSlide) { -it / 4 } + fadeIn(AnimSpec.padFade))
                                                .togetherWith(slideOutHorizontally(AnimSpec.padSlideOut) { it / 4 } + fadeOut(AnimSpec.padFadeOut))
                                        }
                                    },
                                    label = "blueprintDetail",
                                ) { uuid ->
                                    if (uuid != null) {
                                        io.github.moxisuki.blockprint.cat.ui.detail.BlueprintDetailContent(
                                            uuid = uuid,
                                            navController = navController,
                                            snackbarHostState = snackbarHostState,
                                            bridgeViewModel = bridgeVm,
                                        )
                                    } else {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = true,
                                                enter = fadeIn(AnimSpec.fade) + slideInHorizontally(AnimSpec.slide) { it / 8 },
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                    Spacer(Modifier.height(12.dp))
                                                    Text(stringResource(R.string.pad_empty_select_bp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    composable(
                        NavRoutes.COMMUNITY,
                        enterTransition = { fadeIn(AnimSpec.padFade) },
                        exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { fadeIn(AnimSpec.padFade) },
                        popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(0.4f)) {
                                CommunityScreen(
                                    navController = navController,
                                    viewModel = communityVm,
                                    onSchematicSelected = { s, id -> selectedCommunityPair = s to id },
                                )
                            }
                            HorizontalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                            Box(Modifier.weight(0.6f)) {
                                AnimatedContent(
                                    targetState = selectedCommunityPair,
                                    transitionSpec = {
                                        if (targetState != null) {
                                            (slideInHorizontally(AnimSpec.padSlide) { it / 4 } + fadeIn(AnimSpec.padFade))
                                                .togetherWith(slideOutHorizontally(AnimSpec.padSlideOut) { -it / 4 } + fadeOut(AnimSpec.padFadeOut))
                                        } else {
                                            (slideInHorizontally(AnimSpec.padSlide) { -it / 4 } + fadeIn(AnimSpec.padFade))
                                                .togetherWith(slideOutHorizontally(AnimSpec.padSlideOut) { it / 4 } + fadeOut(AnimSpec.padFadeOut))
                                        }
                                    },
                                    label = "communityDetail",
                                ) { pair ->
                                    if (pair != null) {
                                        io.github.moxisuki.blockprint.cat.ui.community.CommunityDetailContent(
                                            source = pair.first,
                                            id = pair.second,
                                            viewModel = communityVm,
                                            snackbarHostState = snackbarHostState,
                                        )
                                    } else {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            androidx.compose.animation.AnimatedVisibility(
                                                visible = true,
                                                enter = fadeIn(AnimSpec.fade) + slideInHorizontally(AnimSpec.slide) { it / 8 },
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                    Spacer(Modifier.height(12.dp))
                                                    Text(stringResource(R.string.pad_empty_select_community), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    composable(
                        route = NavRoutes.COMMUNITY_LOGIN,
                        enterTransition = { slideInHorizontally(AnimSpec.padSlide) { it } + fadeIn(AnimSpec.padFade) },
                        exitTransition = { slideOutHorizontally(AnimSpec.padSlideOut) { it } + fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { slideInHorizontally(AnimSpec.padSlide) { -it / 4 } + fadeIn(AnimSpec.padFade) },
                        popExitTransition = { slideOutHorizontally(AnimSpec.padSlideOut) { it } + fadeOut(AnimSpec.padFadeOut) },
                    ) {
                        LoginWebViewScreen(
                            onLoginSuccess = {
                                communityVm.refreshLoginState()
                                communityVm.refresh()
                                navController.popBackStack()
                            },
                        )
                    }
                    composable(NavRoutes.CONNECTION) {
                        ConnectionScreen(
                            bridgeVm = bridgeVm,
                            onQrClick = { navController.navigate(NavRoutes.QR_SCANNER) },
                        )
                    }
                    composable(
                        route = NavRoutes.QR_SCANNER,
                        enterTransition = {
                            slideInVertically(tween(300)) { it } + fadeIn(tween(260))
                        },
                        exitTransition = {
                            slideOutVertically(tween(280)) { it } + fadeOut(tween(240))
                        },
                    ) {
                        QrScannerScreen(
                            onResult = { conn ->
                                bridgeVm.connect(conn.host, conn.port, conn.token)
                                navController.popBackStack()
                            },
                            onClose = { navController.popBackStack() },
                        )
                    }
                    composable(
                        route = NavRoutes.SETTINGS,
                        enterTransition = { fadeIn(AnimSpec.padFade) },
                        exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { fadeIn(AnimSpec.padFade) },
                        popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                    ) {
                        SettingsScreen(navController = navController)
                    }
                    composable(
                        route = NavRoutes.ABOUT,
                        enterTransition = { fadeIn(AnimSpec.padFade) },
                        exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { fadeIn(AnimSpec.padFade) },
                        popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                    ) {
                        AboutScreen(navController = navController)
                    }
                    composable(
                        route = NavRoutes.TERMS,
                        enterTransition = { fadeIn(AnimSpec.padFade) },
                        exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { fadeIn(AnimSpec.padFade) },
                        popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                    ) {
                        TermsScreen(navController = navController)
                    }
                    composable(
                        route = NavRoutes.COMMUNITY_SETTINGS,
                        enterTransition = { fadeIn(AnimSpec.padFade) },
                        exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { fadeIn(AnimSpec.padFade) },
                        popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                    ) {
                        CommunitySettingsScreen()
                    }
                    composable(
                        route = "${NavRoutes.RENDER}?mod={modSlug}",
                        arguments = listOf(navArgument("modSlug") { type = NavType.StringType; defaultValue = "" }),
                        enterTransition = { fadeIn(AnimSpec.padFade) },
                        exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { fadeIn(AnimSpec.padFade) },
                        popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                    ) { entry ->
                        val modSlug = entry.arguments?.getString("modSlug") ?: ""
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(Modifier.widthIn(max = 680.dp).fillMaxWidth().fillMaxHeight()) {
                                RenderManagerScreen(snackbarHostState = snackbarHostState, initialModSlug = modSlug)
                            }
                        }
                    }
                    composable(
                        route = "${NavRoutes.PREVIEW}/{uuid}",
                        arguments = listOf(navArgument("uuid") { type = NavType.StringType }),
                        enterTransition = { slideInHorizontally(AnimSpec.padSlide) { it } + fadeIn(AnimSpec.padFade) },
                        exitTransition = { slideOutHorizontally(AnimSpec.padSlideOut) { it } + fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { slideInHorizontally(AnimSpec.padSlide) { -it / 4 } + fadeIn(AnimSpec.padFade) },
                        popExitTransition = { slideOutHorizontally(AnimSpec.padSlideOut) { it } + fadeOut(AnimSpec.padFadeOut) },
                    ) { backStackEntry ->
                        val uuid = backStackEntry.arguments?.getString("uuid") ?: ""
                        PreviewScreen(uuid = uuid, navController = navController, onFullscreenChange = onPreviewFullscreenChange)
                    }
                    composable(
                        route = "${NavRoutes.DETAIL}/{uuid}",
                        arguments = listOf(navArgument("uuid") { type = NavType.StringType }),
                        enterTransition = { slideInHorizontally(AnimSpec.padSlide) { it } + fadeIn(AnimSpec.padFade) },
                        exitTransition = { slideOutHorizontally(AnimSpec.padSlideOut) { it } + fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { slideInHorizontally(AnimSpec.padSlide) { -it / 4 } + fadeIn(AnimSpec.padFade) },
                        popExitTransition = { slideOutHorizontally(AnimSpec.padSlideOut) { it } + fadeOut(AnimSpec.padFadeOut) },
                    ) { backStackEntry ->
                        val uuid = backStackEntry.arguments?.getString("uuid") ?: ""
                        BlueprintDetailScreen(
                            uuid = uuid,
                            navController = navController,
                            onTitleChange = onDetailTitleChange,
                            snackbarHostState = snackbarHostState,
                            bridgeViewModel = bridgeVm,
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(12.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }
    }
}

/**
 * Compact / phone-width layout (<840dp wide).
 *
 * Standard Scaffold with bottom NavigationBar, full-width NavHost for
 * each destination. Includes the BLUEPRINT_DETAIL route (handled by Pad
 * via the two-pane composition) and COMMUNITY_DETAIL (Compact pushes a
 * full-screen detail).
 */
@Composable
private fun CompactLayout(
    navController: NavHostController,
    bridgeVm: BridgeViewModel,
    communityVm: CommunityViewModel,
    snackbarHostState: SnackbarHostState,
    onImportSafer: (Uri) -> Unit,
    onRefresh: (Int) -> Unit,
    onRequestSafFolder: () -> Unit,
    isPreviewFullscreen: Boolean,
    onPreviewFullscreenChange: (Boolean) -> Unit,
    detailTitle: String,
    onDetailTitleChange: (String) -> Unit,
    communityEnabled: Boolean,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isDetail = currentDestination?.route?.startsWith(NavRoutes.DETAIL) == true
    val isPreview = currentDestination?.route?.startsWith(NavRoutes.PREVIEW) == true
    val isSettings = currentDestination?.route == NavRoutes.SETTINGS
    val isRender = currentDestination?.route?.startsWith(NavRoutes.RENDER) == true
    val isCommunityDetail = currentDestination?.route?.startsWith(NavRoutes.COMMUNITY_DETAIL) == true
    val isCommunityLogin = currentDestination?.route == NavRoutes.COMMUNITY_LOGIN
    val isAbout = currentDestination?.route == NavRoutes.ABOUT
    val isTerms = currentDestination?.route == NavRoutes.TERMS
    val isQrScanner = currentDestination?.route == NavRoutes.QR_SCANNER
    val isCommunitySettings = currentDestination?.route == NavRoutes.COMMUNITY_SETTINGS
    val showBottomBar = !isDetail && !isRender && !isPreview && !isCommunityDetail && !isCommunityLogin && !isAbout && !isTerms && !isQrScanner && !isCommunitySettings
    val showBackButton = isDetail || isRender || isPreview || isCommunityDetail || isCommunityLogin || isAbout || isTerms || isQrScanner || isCommunitySettings

    val topBarTitle = when {
        isDetail -> detailTitle
        currentDestination?.route == NavRoutes.HOME -> stringResource(R.string.nav_title_home)
        currentDestination?.route == NavRoutes.COMMUNITY -> stringResource(R.string.nav_title_community)
        currentDestination?.route == NavRoutes.COMMUNITY_SETTINGS -> stringResource(R.string.nav_title_community_settings)
        currentDestination?.route == NavRoutes.CONNECTION -> stringResource(R.string.nav_title_connection)
        isCommunityDetail -> stringResource(R.string.nav_title_community_detail)
        isCommunityLogin -> stringResource(R.string.nav_title_community_login)
        isRender -> stringResource(R.string.nav_title_render)
        isPreview -> stringResource(R.string.nav_title_preview)
        isAbout -> stringResource(R.string.nav_title_about)
        isTerms -> stringResource(R.string.nav_title_terms)
        isQrScanner -> stringResource(R.string.nav_title_qr_scanner)
        else -> ""
    }
    val showMainTopBar = !isSettings && !isPreviewFullscreen

    val connectionState by bridgeVm.connectionState.collectAsState()
    val isBridgeConnected = connectionState is ConnectionState.Connected
    val isBridgeConnecting = connectionState is ConnectionState.Connecting

    val communityState by communityVm.state.collectAsState()
    val onCommunity2 = currentDestination?.route == NavRoutes.COMMUNITY
    val active2 = communityState.active

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showMainTopBar) {
                if (currentDestination?.route == NavRoutes.HOME) {
                    AppTopBar(
                        title = topBarTitle,
                        showBackButton = showBackButton,
                        showCommunityActions = onCommunity2 && active2.ready,
                        showLogout = communityState.currentSource == CommunitySource.MCS,
                        onCommunity = onCommunity2,
                        onToggleFilter = { communityVm.toggleFilter() },
                        onToggleHeatSort = { communityVm.toggleHeatSort() },
                        onRefresh = { communityVm.refresh() },
                        onLogout = { communityVm.logout(); communityVm.refreshLoginState() },
                        onBack = { navController.popBackStack() },
                        isHeatSort = active2.heatSort,
                        actions = {
                            IconButton(onClick = { navController.navigate(NavRoutes.CONNECTION) }) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isBridgeConnected -> Color(0xFF4CAF50)
                                                isBridgeConnecting -> Color(0xFFFFC107)
                                                else -> Color(0xFF9E9E9E)
                                            }
                                        )
                                )
                            }
                        },
                    )
                } else {
                    AppTopBar(
                        title = topBarTitle,
                        showBackButton = showBackButton,
                        showCommunityActions = onCommunity2 && active2.ready,
                        showLogout = communityState.currentSource == CommunitySource.MCS,
                        onCommunity = onCommunity2,
                        onToggleFilter = { communityVm.toggleFilter() },
                        onToggleHeatSort = { communityVm.toggleHeatSort() },
                        onRefresh = { communityVm.refresh() },
                        onLogout = { communityVm.logout(); communityVm.refreshLoginState() },
                        onBack = { navController.popBackStack() },
                        isHeatSort = active2.heatSort,
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    snackbarData = data,
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    bottomNavItems
                        .filter { it.route != NavRoutes.COMMUNITY || communityEnabled }
                        .forEach { item ->
                            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                            val label = stringResource(item.labelRes)
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label) },
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id)
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.HOME,
                enterTransition = { fadeIn(AnimSpec.fade) },
                exitTransition = { fadeOut(AnimSpec.fadeExit) },
                popEnterTransition = { fadeIn(AnimSpec.fade) },
                popExitTransition = { fadeOut(AnimSpec.fadeExit) },
            ) {
                composable(NavRoutes.HOME) {
                    HomeScreen(
                        navController = navController,
                        bridgeVm = bridgeVm,
                        snackbarHostState = snackbarHostState,
                        onRequestSafFolder = onRequestSafFolder,
                        onRefresh = onRefresh,
                    )
                }
                composable(NavRoutes.COMMUNITY) {
                    CommunityScreen(
                        navController = navController,
                        viewModel = communityVm,
                    )
                }
                composable(NavRoutes.CONNECTION) {
                    ConnectionScreen(
                        bridgeVm = bridgeVm,
                        onQrClick = { navController.navigate(NavRoutes.QR_SCANNER) },
                    )
                }
                composable(
                    route = NavRoutes.QR_SCANNER,
                    enterTransition = {
                        slideInVertically(tween(300)) { it } + fadeIn(tween(260))
                    },
                    exitTransition = {
                        slideOutVertically(tween(280)) { it } + fadeOut(tween(240))
                    },
                ) {
                    QrScannerScreen(
                        onResult = { conn ->
                            bridgeVm.connect(conn.host, conn.port, conn.token)
                            navController.popBackStack()
                        },
                        onClose = { navController.popBackStack() },
                    )
                }
                composable(
                    route = NavRoutes.COMMUNITY_LOGIN,
                    enterTransition = { slideInHorizontally(AnimSpec.slide) { it } + fadeIn(AnimSpec.fade) },
                    exitTransition = { slideOutHorizontally(AnimSpec.slideExit) { it } + fadeOut(AnimSpec.fadeExit) },
                    popEnterTransition = { slideInHorizontally(AnimSpec.slide) { -it } + fadeIn(AnimSpec.fade) },
                    popExitTransition = { slideOutHorizontally(AnimSpec.slideExit) { it } + fadeOut(AnimSpec.fadeExit) },
                ) {
                    LoginWebViewScreen(
                        onLoginSuccess = {
                            communityVm.refreshLoginState()
                            communityVm.refresh()
                            navController.popBackStack()
                        },
                    )
                }
                composable(
                    route = "${NavRoutes.COMMUNITY_DETAIL}/{source}/{id}",
                    arguments = listOf(
                        navArgument("source") { type = NavType.StringType },
                        navArgument("id") { type = NavType.StringType },
                    ),
                    enterTransition = { slideInHorizontally(AnimSpec.slide) { it } + fadeIn(AnimSpec.fade) },
                    exitTransition = { slideOutHorizontally(AnimSpec.slideExit) { it } + fadeOut(AnimSpec.fadeExit) },
                    popEnterTransition = { slideInHorizontally(AnimSpec.slide) { -it } + fadeIn(AnimSpec.fade) },
                    popExitTransition = { slideOutHorizontally(AnimSpec.slideExit) { it } + fadeOut(AnimSpec.fadeExit) },
                ) { backStackEntry ->
                    val source = backStackEntry.arguments?.getString("source")
                        ?.let { runCatching { CommunitySource.valueOf(it) }.getOrNull() }
                        ?: CommunitySource.MCS
                    val id = backStackEntry.arguments?.getString("id") ?: ""
                    CommunityDetailScreen(
                        source = source,
                        id = id,
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                        viewModel = communityVm,
                    )
                }
                composable(
                    route = "${NavRoutes.RENDER}?mod={modSlug}",
                    arguments = listOf(navArgument("modSlug") { type = NavType.StringType; defaultValue = "" }),
                    enterTransition = { fadeIn(AnimSpec.fade) },
                    exitTransition = { fadeOut(AnimSpec.fadeExit) },
                    popEnterTransition = { fadeIn(AnimSpec.fade) },
                    popExitTransition = { fadeOut(AnimSpec.fadeExit) },
                ) { entry ->
                    val modSlug = entry.arguments?.getString("modSlug") ?: ""
                    RenderManagerScreen(snackbarHostState = snackbarHostState, initialModSlug = modSlug)
                }
                composable(
                    route = NavRoutes.SETTINGS,
                ) {
                    SettingsScreen(navController = navController)
                }
                composable(route = NavRoutes.ABOUT) { AboutScreen(navController = navController) }
                composable(route = NavRoutes.TERMS) { TermsScreen(navController = navController) }
                composable(route = NavRoutes.COMMUNITY_SETTINGS) { CommunitySettingsScreen() }
                composable(
                    route = "${NavRoutes.PREVIEW}/{uuid}",
                    arguments = listOf(navArgument("uuid") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(AnimSpec.slide) { it } + fadeIn(AnimSpec.fade) },
                    exitTransition = { slideOutHorizontally(AnimSpec.slideExit) { it } + fadeOut(AnimSpec.fadeExit) },
                    popEnterTransition = { slideInHorizontally(AnimSpec.slide) { -it } + fadeIn(AnimSpec.fade) },
                    popExitTransition = { slideOutHorizontally(AnimSpec.slideExit) { it } + fadeOut(AnimSpec.fadeExit) },
                ) { backStackEntry ->
                    val uuid = backStackEntry.arguments?.getString("uuid") ?: ""
                    PreviewScreen(uuid = uuid, navController = navController, onFullscreenChange = onPreviewFullscreenChange)
                }
                composable(
                    route = "${NavRoutes.DETAIL}/{uuid}",
                    arguments = listOf(navArgument("uuid") { type = NavType.StringType }),
                    enterTransition = { slideInHorizontally(AnimSpec.slide) { it } + fadeIn(AnimSpec.fade) },
                    exitTransition = { slideOutHorizontally(AnimSpec.slideExit) { it } + fadeOut(AnimSpec.fadeExit) },
                    popEnterTransition = { slideInHorizontally(AnimSpec.slide) { -it / 4 } + fadeIn(AnimSpec.fade) },
                    popExitTransition = { slideOutHorizontally(AnimSpec.slideExit) { it } + fadeOut(AnimSpec.fadeExit) },
                ) { backStackEntry ->
                    val uuid = backStackEntry.arguments?.getString("uuid") ?: ""
                    BlueprintDetailScreen(
                        uuid = uuid,
                        navController = navController,
                        onTitleChange = onDetailTitleChange,
                        snackbarHostState = snackbarHostState,
                        bridgeViewModel = bridgeVm,
                    )
                }
            }
        }
    }
}