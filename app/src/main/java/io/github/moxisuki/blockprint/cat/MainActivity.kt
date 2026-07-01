package io.github.moxisuki.blockprint.cat

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.moxisuki.blockprint.cat.data.ThemeManager
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.data.community.CommunityConfigManager
import io.github.moxisuki.blockprint.cat.data.settings.TermsAcceptance
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeUiEvent
import io.github.moxisuki.blockprint.cat.ui.bridge.BridgeViewModel
import io.github.moxisuki.blockprint.cat.ui.bridge.ConnectionState
import io.github.moxisuki.blockprint.cat.ui.community.CommunityViewModel
import io.github.moxisuki.blockprint.cat.ui.community.DownloadEvent
import io.github.moxisuki.blockprint.cat.ui.navigation.AppNavGraph
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.cat.ui.preview.PreviewFullscreenController
import io.github.moxisuki.blockprint.cat.ui.settings.TermsGate
import io.github.moxisuki.blockprint.cat.ui.theme.BlockPrintCatTheme
import io.github.moxisuki.blockprint.cat.ui.util.rememberAppErrorResolver
import io.github.moxisuki.blockprint.cat.ui.util.toArgb
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
    @Inject lateinit var communityConfigManager: CommunityConfigManager

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

/**
 * Top-level orchestrator. After the MainActivity split it only wires:
 *   1. NavController + SnackbarHostState
 *   2. Theme + status-bar tint side-effect
 *   3. `PreviewFullscreenController` (system-bar hide on preview fullscreen)
 *   4. Bridge + community VMs and event collectors
 *   5. Lifecycle observer that triggers bridge reconnect on RESUME
 *   6. Delegate the actual UI layout to [AppNavGraph]
 *
 * Why so thin: AppNavGraph owns the Pad/Compact branches, AppTopBar, the
 * bottom-nav rail, every screen destination, and the NavHost itself.
 */
@Composable
fun BlockPrintCatAppContent(
    themeManager: ThemeManager,
    communityConfigManager: CommunityConfigManager,
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

    var detailTitle by remember { mutableStateOf("") }
    var isPreviewFullscreen by remember { mutableStateOf(false) }
    val onPreviewFullscreenChange = remember { { full: Boolean -> isPreviewFullscreen = full } }

    PreviewFullscreenController(isFullscreen = isPreviewFullscreen)

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
                is BridgeUiEvent.ConvertSucceeded -> snackbarHostState.showSnackbar(
                    view.context.getString(R.string.snackbar_convert_succeeded, ev.fileName)
                )
                is BridgeUiEvent.ConvertFailed -> snackbarHostState.showSnackbar(
                    view.context.getString(R.string.snackbar_convert_failed, ev.fileName, ev.errorCode)
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

    AppNavGraph(
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
        onDetailTitleChange = { detailTitle = it },
        communityEnabled = communityEnabled,
    )
}