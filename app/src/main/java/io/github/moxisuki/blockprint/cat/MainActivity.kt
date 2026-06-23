package io.github.moxisuki.blockprint.cat

import android.os.Bundle
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import io.github.moxisuki.blockprint.cat.ui.animation.AnimSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.ui.util.rememberAppErrorResolver
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.moxisuki.blockprint.cat.data.ThemeManager
import io.github.moxisuki.blockprint.cat.data.community.CommunitySource
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.ui.adaptive.AdaptiveNavRail
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeViewModel
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeUiEvent
import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionState
import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionScreen
import io.github.moxisuki.blockprint.cat.ui.community.CommunityDetailContent
import io.github.moxisuki.blockprint.cat.ui.community.CommunityDetailScreen
import io.github.moxisuki.blockprint.cat.ui.community.CommunityScreen
import io.github.moxisuki.blockprint.cat.ui.community.CommunityViewModel
import io.github.moxisuki.blockprint.cat.ui.community.DownloadEvent
import io.github.moxisuki.blockprint.cat.ui.community.LoginWebViewScreen
import io.github.moxisuki.blockprint.cat.ui.detail.BlueprintDetailContent
import io.github.moxisuki.blockprint.cat.ui.detail.BlueprintDetailScreen
import io.github.moxisuki.blockprint.cat.ui.home.HomeScreen
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.ui.settings.CommunitySettingsScreen
import io.github.moxisuki.blockprint.cat.ui.preview.PreviewScreen
import io.github.moxisuki.blockprint.cat.ui.qr.QrScannerScreen
import io.github.moxisuki.blockprint.cat.ui.render.RenderManagerScreen
import io.github.moxisuki.blockprint.cat.ui.settings.SettingsScreen
import io.github.moxisuki.blockprint.cat.ui.settings.AboutScreen
import io.github.moxisuki.blockprint.cat.ui.settings.TermsScreen
import io.github.moxisuki.blockprint.cat.ui.settings.TermsGate
import io.github.moxisuki.blockprint.cat.data.settings.TermsAcceptance
import io.github.moxisuki.blockprint.cat.ui.theme.BlockPrintCatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var themeManager: ThemeManager
    @Inject lateinit var blueprintManager: BlueprintManager
    @Inject lateinit var termsAcceptance: TermsAcceptance
    @Inject lateinit var communityConfigManager: io.github.moxisuki.blockprint.cat.data.community.CommunityConfigManager

    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val safFolderLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { treeUri ->
            if (treeUri != null) {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(treeUri, flags)
                blueprintManager.setSafFolder(this, treeUri)
            }
        }

        setContent {
            // Resolve the same ViewModel instances BlockPrintCatAppContent
            // uses, so the onRefresh lambda can call bridgeVm.requestList()
            // for the PC tab refresh without re-resolving hilt here.
            val outerBridgeVm: BridgeViewModel = hiltViewModel()
            val outerConnectionState by outerBridgeVm.connectionState.collectAsState()
            val outerIsBridgeConnected = outerConnectionState is ConnectionState.Connected

            BlockPrintCatTheme(themeManager = themeManager) {
                var termsAccepted by remember { mutableStateOf(termsAcceptance.isAccepted()) }
                if (!termsAccepted) {
                    TermsGate(
                        onAccepted = {
                            termsAccepted = true
                            // Bugly 合规要求：用户同意隐私条款后才初始化
                            (application as? BlockPrintCatApp)?.initBuglyIfConsented()
                        },
                        onExit = { finishAffinity() },
                    )
                    return@BlockPrintCatTheme
                }
                BlockPrintCatAppContent(
                    themeManager = themeManager,
                    communityConfigManager = communityConfigManager,
                    onRequestSafFolder = { safFolderLauncher.launch(null) },
                    // Tab-aware refresh:
                    //   tab 0 (Local) → re-hydrate the on-disk blueprint list.
                    //   tab 1 (PC)    → ask the bridge for a fresh server listing.
                    //                   PC refresh MUST NOT touch the local
                    //                   on-disk list — the user is looking at
                    //                   remote blueprints, local rehydration
                    //                   would be wasted IO + could mask a stale
                    //                   local list behind the loading spinner.
                    onRefresh = { tab ->
                        activityScope.launch(Dispatchers.IO) {
                            when (tab) {
                                0 -> blueprintManager.refresh()
                                1 -> if (outerIsBridgeConnected) outerBridgeVm.requestList()
                            }
                        }
                    },
                    onImportSafer = { uri ->
                        activityScope.launch(Dispatchers.IO) {
                            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "untitled.litematic"
                            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                            blueprintManager.ingest(name, bytes)
                        }
                    },
                )
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.HOME, R.string.bottom_nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(NavRoutes.CONNECTION, R.string.bottom_nav_connection, Icons.Filled.Computer, Icons.Outlined.Computer),
    BottomNavItem(NavRoutes.COMMUNITY, R.string.bottom_nav_community, Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(NavRoutes.SETTINGS, R.string.bottom_nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockPrintCatAppContent(
    themeManager: ThemeManager,
    communityConfigManager: io.github.moxisuki.blockprint.cat.data.community.CommunityConfigManager,
    onRequestSafFolder: () -> Unit = {},
    onImportSafer: (Uri) -> Unit = {},
    onRefresh: (tab: Int) -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val view = LocalView.current

    val themeState by themeManager.themeState.collectAsState()
    val colorScheme = themeManager.colorSchemeFor(
        when (themeState.mode) {
            ThemeManager.MODE_DARK -> true
            ThemeManager.MODE_LIGHT -> false
            else -> isSystemInDarkTheme()
        }
    )

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
    val showBottomBar = !isDetail && !isRender && !isPreview && !isCommunityDetail && !isCommunityLogin && !isAbout && !isTerms && !isQrScanner && !isCommunitySettings
    val showBackButton = isDetail || isRender || isPreview || isCommunityDetail || isCommunityLogin || isAbout || isTerms || isQrScanner || isCommunitySettings

    var detailTitle by remember { mutableStateOf("") }
    var isPreviewFullscreen by remember { mutableStateOf(false) }
    val onPreviewFullscreenChange = remember { { full: Boolean -> isPreviewFullscreen = full } }

    // Preview fullscreen: hide system bars via WindowInsetsControllerCompat
    // with BEHAVIOR_DEFAULT (no immersive swipe gestures, so touches still
    // reach the app). Also extend the content edge-to-edge so the preview
    // SurfaceView fills the whole screen including the bar areas.
    val configuration = LocalConfiguration.current
    androidx.compose.runtime.DisposableEffect(isPreviewFullscreen, configuration) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            if (isPreviewFullscreen) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                // Use legacy translucent flags so the bar areas become
                // transparent overlays instead of an opaque windowBackground
                // strip. Without these the system paints the default theme
                // background where the bars were.
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            } else {
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val w = (view.context as? android.app.Activity)?.window
            if (w != null) {
                w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                WindowCompat.getInsetsController(w, view).show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 预览全屏时跳过状态栏修改，避免加载事件重组导致退出沉浸模式
    SideEffect {
        if (isPreviewFullscreen) return@SideEffect
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                themeState.mode != ThemeManager.MODE_DARK
            window.statusBarColor = colorScheme.surface.toArgb()
        }
    }

    val bridgeVm: BridgeViewModel = hiltViewModel()
    val connectionState by bridgeVm.connectionState.collectAsState()
    val isBridgeConnected = connectionState is ConnectionState.Connected
    val isBridgeConnecting = connectionState is ConnectionState.Connecting

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                bridgeVm.reconnectIfNeeded()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val communityEnabled by communityConfigManager.enabled.collectAsState()

    LaunchedEffect(Unit) {
        bridgeVm.events.collect { ev ->
            when (ev) {
                is BridgeUiEvent.DownloadStart -> Unit // Status shown in progress bar
                is BridgeUiEvent.DownloadComplete -> {
                    if (currentDestination?.route == NavRoutes.HOME) {
                        navController.navigate(NavRoutes.detailRoute(ev.targetUuid))
                    }
                }
                is BridgeUiEvent.DownloadFailed -> Unit // Status shown in progress bar
                is BridgeUiEvent.UploadSucceeded -> Unit
                is BridgeUiEvent.UploadFailed -> snackbarHostState.showSnackbar(
                    view.context.getString(R.string.snackbar_upload_failed, ev.fileName, ev.errorCode)
                )
                is BridgeUiEvent.AuthFailed -> snackbarHostState.showSnackbar(ev.message)
                is BridgeUiEvent.Disconnected -> {
                    if (ev.unexpected) snackbarHostState.showSnackbar(view.context.getString(R.string.snackbar_disconnected))
                }
            }
        }
    }

    LaunchedEffect(communityEnabled) {
        if (!communityEnabled && currentDestination?.route == NavRoutes.COMMUNITY) {
            navController.navigate(NavRoutes.HOME) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
        }
    }

    val communityVm: CommunityViewModel = hiltViewModel()
    val communityState by communityVm.state.collectAsState()

    val resolveAppError = rememberAppErrorResolver()

    LaunchedEffect(Unit) {
        communityVm.download.collect { event ->
            when (event) {
                is DownloadEvent.Success -> snackbarHostState.showSnackbar(view.context.getString(R.string.snackbar_community_downloaded, event.schematic.name))
                is DownloadEvent.Failed -> snackbarHostState.showSnackbar(resolveAppError(event.error))
                is DownloadEvent.Progress -> Unit
            }
        }
    }

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

    val isExpanded = LocalConfiguration.current.screenWidthDp >= 840
    var selectedBlueprintUuid by remember { mutableStateOf<String?>(null) }
    var selectedCommunityPair by remember { mutableStateOf<Pair<CommunitySource, String>?>(null) }

    if (isExpanded) {
        val padFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { onImportSafer(it) }
        }
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isPreviewFullscreen) {
                    val padOnCommunity = currentDestination?.route == NavRoutes.COMMUNITY
                    val padActive = communityState.active
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
                                            .background(when {
                                                isBridgeConnected -> Color(0xFF4CAF50)
                                                isBridgeConnecting -> Color(0xFFFFC107)
                                                else -> Color(0xFF9E9E9E)
                                            })
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
                                    HomeScreen(navController = navController, bridgeVm = bridgeVm, snackbarHostState = snackbarHostState, onRequestSafFolder = onRequestSafFolder, onRefresh = onRefresh, onBlueprintSelected = remember { { bp -> selectedBlueprintUuid = bp.uuid } })
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
                                            BlueprintDetailContent(uuid = uuid, navController = navController, snackbarHostState = snackbarHostState)
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
                                Box(Modifier.weight(0.4f)) { CommunityScreen(navController = navController, viewModel = communityVm, onSchematicSelected = { s, id -> selectedCommunityPair = s to id }) }
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
                                            CommunityDetailContent(source = pair.first, id = pair.second, viewModel = communityVm, snackbarHostState = snackbarHostState)
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
                        ) { LoginWebViewScreen(onLoginSuccess = { communityVm.refreshLoginState(); communityVm.refresh(); navController.popBackStack() }) }
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
                        ) { SettingsScreen(navController = navController) }
                        composable(
                            route = NavRoutes.ABOUT,
                            enterTransition = { fadeIn(AnimSpec.padFade) },
                            exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                            popEnterTransition = { fadeIn(AnimSpec.padFade) },
                            popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        ) { AboutScreen(navController = navController) }
                        composable(
                            route = NavRoutes.TERMS,
                            enterTransition = { fadeIn(AnimSpec.padFade) },
                            exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                            popEnterTransition = { fadeIn(AnimSpec.padFade) },
                            popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        ) { TermsScreen(navController = navController) }
                        composable(
                            route = NavRoutes.COMMUNITY_SETTINGS,
                            enterTransition = { fadeIn(AnimSpec.padFade) },
                            exitTransition = { fadeOut(AnimSpec.padFadeOut) },
                            popEnterTransition = { fadeIn(AnimSpec.padFade) },
                            popExitTransition = { fadeOut(AnimSpec.padFadeOut) },
                        ) { CommunitySettingsScreen() }
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
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            ) { data ->
                Snackbar(snackbarData = data, shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface)
            }
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (showMainTopBar) {
                    val onCommunity2 = currentDestination?.route == NavRoutes.COMMUNITY
                    val active2 = communityState.active
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
                        // 同上：必须传入 activity 作用域的 bridgeVm。
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
                            onTitleChange = { detailTitle = it },
                            snackbarHostState = snackbarHostState,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(
    title: String,
    showBackButton: Boolean,
    showCommunityActions: Boolean,
    showLogout: Boolean,
    onCommunity: Boolean,
    onToggleFilter: () -> Unit,
    onToggleHeatSort: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    isHeatSort: Boolean,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(AnimSpec.title) togetherWith fadeOut(AnimSpec.title) using SizeTransform(clip = false) },
                label = "topBarTitle",
            ) { t -> Text(t, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        navigationIcon = {
            AnimatedVisibility(
                visible = showBackButton,
                enter = fadeIn(AnimSpec.title),
                exit = fadeOut(AnimSpec.title),
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back)) }
            }
        },
        actions = {
            if (onCommunity && showCommunityActions) {
                IconButton(onClick = onToggleFilter) { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search)) }
                IconButton(onClick = onToggleHeatSort) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = if (isHeatSort) stringResource(R.string.cd_sort_time) else stringResource(R.string.cd_sort_hot), tint = if (isHeatSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh)) }
                if (showLogout) {
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, contentDescription = stringResource(R.string.cd_logout)) }
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)
