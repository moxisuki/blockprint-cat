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
import io.github.moxisuki.blockprint.cat.data.bridge.RemoteBlueprint
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
import io.github.moxisuki.blockprint.cat.ui.settings.ChangelogScreen
import io.github.moxisuki.blockprint.cat.ui.settings.CommunitySettingsScreen
import io.github.moxisuki.blockprint.cat.ui.settings.SettingsScreen
import io.github.moxisuki.blockprint.cat.ui.settings.TermsScreen
import io.github.moxisuki.blockprint.cat.ui.tools.ToolsScreen
import io.github.moxisuki.blockprint.cat.ui.tools.imagetoblueprint.ImageToBlueprintScreen

/**
 * Derived state shared between Pad and Compact layout branches.
 *
 * Both branches used to re-derive the same 11 route booleans + connection /
 * community state from the navigation controller and ViewModels on every
 * recomposition. Hoisting the derivation:
 *   1. Halves the recomputation cost on every nav / VM emission
 *   2. Eliminates the risk of one branch using a slightly different route
 *      matcher (the previous `isHome` derivation was missing in PadLayout)
 *   3. Makes it easy to add a new route by editing one place
 *
 * `showBottomBar` / `showBackButton` are stored vals (not `get()`) so the
 * data class can be passed as a stable argument into the layout functions.
 * `isBridgeConnected` / `pcSession` / `pcEntries` are derived from
 * [connectionState] in one place so all consumers see the same snapshot.
 */
internal data class NavGraphFlags(
    val route: String?,
    val isHome: Boolean,
    val isDetail: Boolean,
    val isPreview: Boolean,
    val isSettings: Boolean,
    val isRender: Boolean,
    val isCommunity: Boolean,
    val isCommunityDetail: Boolean,
    val isCommunityLogin: Boolean,
    val isAbout: Boolean,
    val isChangelog: Boolean,
    val isTerms: Boolean,
    val isQrScanner: Boolean,
    val isCommunitySettings: Boolean,
    val isTools: Boolean,
    val isImageToBlueprint: Boolean,
    val isTextToBlueprint: Boolean,
    val isConnection: Boolean,
    val connectionState: ConnectionState,
    // Community fields are flattened to stable primitives so this data class
    // stays Stable. The previous single `communityState: CommunityListState`
    // field was Unstable because CommunityListState's `mcs` / `cms`
    // (PerSourceState) carry `schematics: List<...>` which the Kotlin
    // compiler can't prove immutable.
    val communityCurrentSource: io.github.moxisuki.blockprint.cat.data.community.CommunitySource,
    val communityReady: Boolean,
    val communityHeatSort: Boolean,
    val pcEntriesCount: Int,
) {
    val isBridgeConnected: Boolean = connectionState is ConnectionState.Connected
    val isBridgeConnecting: Boolean = connectionState is ConnectionState.Connecting
    val pcSession: io.github.moxisuki.blockprint.cat.data.bridge.SessionInfo? =
        (connectionState as? ConnectionState.Connected)?.session
    val hasPcEntries: Boolean = pcEntriesCount > 0

    val showBottomBar: Boolean =
        !isDetail && !isRender && !isPreview && !isCommunityDetail &&
            !isCommunityLogin && !isAbout && !isChangelog && !isTerms && !isQrScanner && !isCommunitySettings &&
            !isImageToBlueprint

    val showBackButton: Boolean =
        isDetail || isRender || isPreview || isCommunityDetail || isCommunityLogin ||
            isAbout || isChangelog || isTerms || isQrScanner || isCommunitySettings || isImageToBlueprint
}

/**
 * Single-shot collector for everything both layout branches need. Wraps
 * the destination + connection + community derivations in a `remember`
 * keyed on the three reactive sources so the result is computed at most
 * once per emission.
 */
@Composable
private fun rememberNavGraphFlags(
    navController: NavHostController,
    bridgeVm: BridgeViewModel,
    communityVm: CommunityViewModel,
): NavGraphFlags {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val connectionState by bridgeVm.connectionState.collectAsState()
    val communityState by communityVm.state.collectAsState()

    return remember(currentDestination, connectionState, communityState) {
        val route = currentDestination?.route
        val connected = connectionState as? ConnectionState.Connected
        // Flatten CommunityListState into stable primitives here, before
        // constructing NavGraphFlags. If we put `communityState` (Unstable)
        // on the data class, the whole class loses skip-ability.
        val active = communityState.active
        NavGraphFlags(
            route = route,
            isHome = route == NavRoutes.HOME,
            isDetail = route?.startsWith(NavRoutes.DETAIL) == true,
            isPreview = route?.startsWith(NavRoutes.PREVIEW) == true,
            isSettings = route == NavRoutes.SETTINGS,
            isRender = route?.startsWith(NavRoutes.RENDER) == true,
            isCommunity = route == NavRoutes.COMMUNITY,
            isCommunityDetail = route?.startsWith(NavRoutes.COMMUNITY_DETAIL) == true,
            isCommunityLogin = route == NavRoutes.COMMUNITY_LOGIN,
            isTextToBlueprint = route == NavRoutes.TEXT_TO_BLUEPRINT,
            isAbout = route == NavRoutes.ABOUT,
            isChangelog = route == NavRoutes.CHANGELOG,
            isTerms = route == NavRoutes.TERMS,
            isQrScanner = route == NavRoutes.QR_SCANNER,
            isCommunitySettings = route == NavRoutes.COMMUNITY_SETTINGS,
            isTools = route == NavRoutes.TOOLS,
            isImageToBlueprint = route == NavRoutes.IMAGE_TO_BLUEPRINT,
            isConnection = route == NavRoutes.CONNECTION,
            connectionState = connectionState,
            communityCurrentSource = communityState.currentSource,
            communityReady = active.ready,
            communityHeatSort = active.heatSort,
            pcEntriesCount = connected?.entries?.size ?: 0,
        )
    }
}

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
    // Hoist all the cross-branch state into one place; see NavGraphFlags.
    val flags = rememberNavGraphFlags(navController, bridgeVm, communityVm)
    if (isExpanded) {
        PadLayout(
            navController = navController,
            flags = flags,
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
            flags = flags,
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
    flags: NavGraphFlags,
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

    val topBarTitle = when {
        flags.isDetail -> detailTitle
        flags.route == NavRoutes.HOME -> stringResource(R.string.nav_title_home)
        flags.route == NavRoutes.COMMUNITY -> stringResource(R.string.nav_title_community)
        flags.route == NavRoutes.COMMUNITY_SETTINGS -> stringResource(R.string.nav_title_community_settings)
        flags.route == NavRoutes.CONNECTION -> stringResource(R.string.nav_title_connection)
        flags.isCommunityDetail -> stringResource(R.string.nav_title_community_detail)
        flags.isCommunityLogin -> stringResource(R.string.nav_title_community_login)
        flags.isRender -> stringResource(R.string.nav_title_render)
        flags.isPreview -> stringResource(R.string.nav_title_preview)
        flags.isAbout -> stringResource(R.string.nav_title_about)
        flags.isChangelog -> stringResource(R.string.nav_title_changelog)
        flags.isTerms -> stringResource(R.string.nav_title_terms)
        flags.isTools -> stringResource(R.string.nav_title_tools)
        flags.isImageToBlueprint -> stringResource(R.string.itb_title)
        flags.isTextToBlueprint -> stringResource(R.string.tool_text_to_blueprint)
        flags.isQrScanner -> stringResource(R.string.nav_title_qr_scanner)
        else -> ""
    }

    val padOnCommunity = flags.isCommunity
    val padActiveReady = flags.communityReady
    val padActiveHeatSort = flags.communityHeatSort

    val padFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { onImportSafer(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isPreviewFullscreen) {
                if (flags.isHome) {
                    AppTopBar(
                        title = topBarTitle,
                        showBackButton = flags.showBackButton,
                        showCommunityActions = padOnCommunity && padActiveReady,
                        showLogout = flags.communityCurrentSource == CommunitySource.MCS,
                        onCommunity = padOnCommunity,
                        onToggleFilter = { communityVm.toggleFilter() },
                        onToggleHeatSort = { communityVm.toggleHeatSort() },
                        onRefresh = { communityVm.refresh() },
                        onLogout = { communityVm.logout(); communityVm.refreshLoginState() },
                        onBack = { navController.popBackStack() },
                        isHeatSort = padActiveHeatSort,
                        actions = {
                            IconButton(onClick = { navController.navigate(NavRoutes.CONNECTION) }) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape)
                                        .background(
                                            when {
                                                flags.isBridgeConnected -> Color(0xFF4CAF50)
                                                flags.isBridgeConnecting -> Color(0xFFFFC107)
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
                        showBackButton = flags.showBackButton,
                        showCommunityActions = padOnCommunity && padActiveReady,
                        showLogout = flags.communityCurrentSource == CommunitySource.MCS,
                        onCommunity = padOnCommunity,
                        onToggleFilter = { communityVm.toggleFilter() },
                        onToggleHeatSort = { communityVm.toggleHeatSort() },
                        onRefresh = { communityVm.refresh() },
                        onLogout = { communityVm.logout(); communityVm.refreshLoginState() },
                        onBack = { navController.popBackStack() },
                        isHeatSort = padActiveHeatSort,
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
                    composable(NavRoutes.TOOLS) {
                        ToolsScreen(
                            snackbarHostState = snackbarHostState,
                            onNavigateToImageToBlueprint = { navController.navigate(NavRoutes.IMAGE_TO_BLUEPRINT) },
                            onNavigateToTextToBlueprint = { navController.navigate(NavRoutes.TEXT_TO_BLUEPRINT) },
                        )
                    }
                    composable(NavRoutes.IMAGE_TO_BLUEPRINT) {
                        ImageToBlueprintScreen(navController = navController)
                    }
                    composable(NavRoutes.TEXT_TO_BLUEPRINT) {
                        io.github.moxisuki.blockprint.cat.ui.tools.texttoblueprint.TextToBlueprintScreen(
                            onBack = { navController.popBackStack() },
                        )
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
                        route = NavRoutes.CHANGELOG,
                        enterTransition = { fadeIn(AnimSpec.padFade) },
                        exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        popEnterTransition = { fadeIn(AnimSpec.padFade) },
                        popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                    ) {
                        ChangelogScreen(navController = navController)
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
    flags: NavGraphFlags,
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
    val topBarTitle = when {
        flags.isDetail -> detailTitle
        flags.route == NavRoutes.HOME -> stringResource(R.string.nav_title_home)
        flags.route == NavRoutes.COMMUNITY -> stringResource(R.string.nav_title_community)
        flags.route == NavRoutes.COMMUNITY_SETTINGS -> stringResource(R.string.nav_title_community_settings)
        flags.route == NavRoutes.CONNECTION -> stringResource(R.string.nav_title_connection)
        flags.isCommunityDetail -> stringResource(R.string.nav_title_community_detail)
        flags.isCommunityLogin -> stringResource(R.string.nav_title_community_login)
        flags.isRender -> stringResource(R.string.nav_title_render)
        flags.isPreview -> stringResource(R.string.nav_title_preview)
        flags.isAbout -> stringResource(R.string.nav_title_about)
        flags.isChangelog -> stringResource(R.string.nav_title_changelog)
        flags.isTerms -> stringResource(R.string.nav_title_terms)
        flags.isTools -> stringResource(R.string.nav_title_tools)
        flags.isImageToBlueprint -> stringResource(R.string.itb_title)
        flags.isTextToBlueprint -> stringResource(R.string.tool_text_to_blueprint)
        flags.isQrScanner -> stringResource(R.string.nav_title_qr_scanner)
        else -> ""
    }
    val showMainTopBar = !flags.isSettings && !isPreviewFullscreen

    val onCommunity2 = flags.isCommunity
    val active2Ready = flags.communityReady
    val active2HeatSort = flags.communityHeatSort

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showMainTopBar) {
                if (flags.isHome) {
                    AppTopBar(
                        title = topBarTitle,
                        showBackButton = flags.showBackButton,
                        showCommunityActions = onCommunity2 && active2Ready,
                        showLogout = flags.communityCurrentSource == CommunitySource.MCS,
                        onCommunity = onCommunity2,
                        onToggleFilter = { communityVm.toggleFilter() },
                        onToggleHeatSort = { communityVm.toggleHeatSort() },
                        onRefresh = { communityVm.refresh() },
                        onLogout = { communityVm.logout(); communityVm.refreshLoginState() },
                        onBack = { navController.popBackStack() },
                        isHeatSort = active2HeatSort,
                        actions = {
                            IconButton(onClick = { navController.navigate(NavRoutes.CONNECTION) }) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                flags.isBridgeConnected -> Color(0xFF4CAF50)
                                                flags.isBridgeConnecting -> Color(0xFFFFC107)
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
                        showBackButton = flags.showBackButton,
                        showCommunityActions = onCommunity2 && active2Ready,
                        showLogout = flags.communityCurrentSource == CommunitySource.MCS,
                        onCommunity = onCommunity2,
                        onToggleFilter = { communityVm.toggleFilter() },
                        onToggleHeatSort = { communityVm.toggleHeatSort() },
                        onRefresh = { communityVm.refresh() },
                        onLogout = { communityVm.logout(); communityVm.refreshLoginState() },
                        onBack = { navController.popBackStack() },
                        isHeatSort = active2HeatSort,
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
            if (flags.showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    bottomNavItems
                        .filter { it.route != NavRoutes.COMMUNITY || communityEnabled }
                        .forEach { item ->
                            val selected = flags.route?.let { route -> navController.currentBackStackEntry?.destination?.hierarchy?.any { it.route == item.route } == true } ?: false
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
                composable(NavRoutes.TOOLS) {
                    ToolsScreen(
                        snackbarHostState = snackbarHostState,
                        onNavigateToImageToBlueprint = { navController.navigate(NavRoutes.IMAGE_TO_BLUEPRINT) },
                        onNavigateToTextToBlueprint = { navController.navigate(NavRoutes.TEXT_TO_BLUEPRINT) },
                    )
                }
                composable(NavRoutes.IMAGE_TO_BLUEPRINT) {
                    ImageToBlueprintScreen(navController = navController)
                }
                composable(NavRoutes.TEXT_TO_BLUEPRINT) {
                    io.github.moxisuki.blockprint.cat.ui.tools.texttoblueprint.TextToBlueprintScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(route = NavRoutes.ABOUT) { AboutScreen(navController = navController) }
                composable(route = NavRoutes.CHANGELOG) { ChangelogScreen(navController = navController) }
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