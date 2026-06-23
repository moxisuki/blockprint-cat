package io.github.moxisuki.blockprint.cat.ui.preview

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.glb.GlbGenerator
import io.github.sceneview.SceneView
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Position
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LineNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberFillLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import io.github.sceneview.model.ModelInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "PreviewScreen"
private const val GRID_SIZE = 64f
private const val GRID_STEP = 1f
private const val ORBIT_SPEED = 0.005f
private const val WALK_ROTATE_SPEED = 0.004f

enum class CameraMode(val label: String) {
    ORBIT("旋转"), WALK("摇杆"), DRAG("拖拽"),
}

private data class GlbEntry(val minY: Float, val centerX: Float, val centerZ: Float, val cacheFile: java.io.File, val fromCache: Boolean = false)

@Composable
fun PreviewScreen(
    uuid: String,
    navController: androidx.navigation.NavController,
    onFullscreenChange: ((Boolean) -> Unit)? = null,
) {
    val context = LocalView.current.context
    var glbEntry by remember { mutableStateOf<GlbEntry?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var glbProgress by remember { mutableFloatStateOf(-1f) }
    var glbStageText by remember { mutableStateOf("") }
    // 进度阶段 → 提示文字
    fun stageFor(frac: Float): String = when {
        frac < 0.07f -> context.getString(R.string.preview_stage_region)
        frac < 0.22f -> context.getString(R.string.preview_stage_texture)
        frac < 0.32f -> context.getString(R.string.preview_stage_atlas)
        frac < 0.67f -> context.getString(R.string.preview_stage_pass1)
        frac < 0.95f -> context.getString(R.string.preview_stage_pass2)
        else -> context.getString(R.string.preview_stage_finalize)
    }
    val generator = remember { io.github.moxisuki.blockprint.cat.ui.render.GlbResourceManager.generator }
    val blueprintManager = remember { PreviewEntryPoint.resolve(context) }
    val view = LocalView.current

    // 退出页面时恢复系统栏
    DisposableEffect(Unit) {
        onDispose {
            onFullscreenChange?.invoke(false)
            (view.context as? Activity)?.window?.let { w ->
                WindowCompat.getInsetsController(w, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    LaunchedEffect(uuid) {
        val key = GlbGenerator.Key(blueprintUuid = uuid)

        // Segment 1: in-memory hit (zero I/O)
        val cached = io.github.moxisuki.blockprint.cat.ui.render.GlbResourceManager.peek(uuid)
        val cachedFile = cached?.cacheFile
        if (cached != null && cachedFile != null && cachedFile.isFile && cachedFile.length() > GlbGenerator.MIN_VALID_GLB_BYTES) {
            glbEntry = GlbEntry(cached.minY, cached.centerX, cached.centerZ, cachedFile, fromCache = true)
            return@LaunchedEffect
        }

        // Segment 2: disk hit (peekCacheFile + litematic for region metadata only)
        val onDisk = generator?.peekCacheFile(key)
        if (onDisk != null) {
            val raw = try { blueprintManager.loadDetail(uuid)?.raw } catch (_: Exception) { null }
            val reg = raw?.regions?.getOrNull(0)
            glbEntry = GlbEntry(
                minY = reg?.let { it.position.y - it.height / 2 }?.toFloat() ?: 0f,
                centerX = reg?.position?.x?.toFloat() ?: 0f,
                centerZ = reg?.position?.z?.toFloat() ?: 0f,
                cacheFile = onDisk,
                fromCache = true,
            )
            return@LaunchedEffect
        }

        // Segment 3: cache miss → generate
        glbProgress = 0f
        glbStageText = context.getString(R.string.preview_stage_region)
        try {
            val cacheFile = withContext(Dispatchers.IO) {
                val lit = io.github.moxisuki.blockprint.cat.ui.render.GlbResourceManager.receiveLitematic(uuid)
                    ?: blueprintManager.loadDetail(uuid)?.raw
                    ?: throw IllegalStateException("蓝图不存在或已被删除")
                if (lit.blockCount() == 0) throw IllegalStateException("该蓝图不包含任何方块")
                generator?.getOrGenerateFile(lit, key) { f ->
                    glbProgress = f
                    glbStageText = stageFor(f)
                } ?: throw IllegalStateException("渲染引擎未初始化")
            }
            val reg = blueprintManager.loadDetail(uuid)?.raw?.regions?.getOrNull(0)
            glbEntry = GlbEntry(
                minY = reg?.let { it.position.y - it.height / 2 }?.toFloat() ?: 0f,
                centerX = reg?.position?.x?.toFloat() ?: 0f,
                centerZ = reg?.position?.z?.toFloat() ?: 0f,
                cacheFile = cacheFile,
            )
        } catch (e: Exception) {
            Log.e(TAG, "预览加载失败", e)
            error = "${e.javaClass.simpleName}: ${e.message ?: context.getString(R.string.preview_error_unknown)}"
        }
    }

    when {
        error != null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.ViewInAr, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.preview_failed), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(error!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        glbEntry != null -> PreviewSceneContent(uuid = uuid, entry = glbEntry!!, onFullscreenChange = onFullscreenChange, fromCache = glbEntry!!.fromCache)
        else -> HudStartupOverlay(visible = true)
    }
}

@Composable
private fun PreviewSceneContent(
    uuid: String,
    entry: GlbEntry,
    onFullscreenChange: ((Boolean) -> Unit)? = null,
    fromCache: Boolean = false,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine = engine)
    val materialLoader = rememberMaterialLoader(engine = engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    val filamentView = rememberView(engine)
    val fillLight = rememberFillLightNode(engine)
    // 自定义无圆盘太阳光 (sunAngularRadius=0,关掉 Filament 自带圆盘避免跟 MC 方块太阳重叠)
    val sunLight = rememberNoDiscSunLight(engine)

    val groundY = entry.minY
    val cam = remember { CameraController(
        eyeX = entry.centerX + 30f, eyeY = groundY + 20f, eyeZ = entry.centerZ + 40f,
        targetX = entry.centerX, targetY = groundY + 4f, targetZ = entry.centerZ,
    ).also { it.gridY = groundY } }
    val glbFile = entry.cacheFile

    var fullscreen by remember { mutableStateOf(false) }
    // System-bar visibility is handled in MainActivity via a
    // DisposableEffect(isPreviewFullscreen) so it survives rotation.
    // We only flip the hoisted state here via the onFullscreenChange callback.
    var cameraMode by remember { mutableStateOf(CameraMode.ORBIT) }
    var lightPreset by remember { mutableStateOf(0) }
    var showGrid by remember { mutableStateOf(true) }
    var layerY by remember { mutableIntStateOf(Int.MAX_VALUE) } // Int.MAX_VALUE = 显示全部层
    var layerPanelOpen by remember { mutableStateOf(false) }
    var centered by remember { mutableStateOf(false) }
    // `centered` = ModelNode 已挂到 SceneView 树(用于相机定位/网格/分层)
    // `modelOnScreen` = SceneView 实际渲染出首帧(用于 loading 收尾)
    var modelOnScreen by remember { mutableStateOf(false) }
    var loadingVisible by remember { mutableStateOf(true) }
    val modelAlpha by animateFloatAsState(
        targetValue = if (modelOnScreen) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "modelAlpha",
    )
    LaunchedEffect(modelOnScreen, entry) {
        if (!modelOnScreen) {
            loadingVisible = true
        } else {
            // Wait 250ms after model is on screen before hiding the overlay
            // (gives GPU upload + first frame a moment to stabilize)
            kotlinx.coroutines.delay(250)
            loadingVisible = false
        }
    }
    var floorCount by remember { mutableIntStateOf(0) }
    var modelRoot by remember { mutableStateOf<Node?>(null) }
    val snackbar = remember { SnackbarHostState() }

    // 仅同步加载初始 light preset 那 1 套环境,避免 4 套全加载的 ~300-800ms 阻塞。
    // 用户切换预设时,由下面的 LaunchedEffect 异步加载并加到 loadedEnvs 里。
    val initialEnv = remember(environmentLoader) {
        loadEnvironmentByName(environmentLoader, LIGHT_PRESETS[0].envName)
    }
    val loadedEnvs = remember {
        mutableStateMapOf<String, Environment>(LIGHT_PRESETS[0].envName to initialEnv)
    }

    // 切换预设时按需加载(已在 loadedEnvs 里就直接用,否则异步加载并写入)
    LaunchedEffect(environmentLoader, lightPreset) {
        val target = LIGHT_PRESETS[lightPreset].envName
        if (loadedEnvs[target] == null) {
            val loaded = withContext(Dispatchers.IO) {
                loadEnvironmentByName(environmentLoader, target)
            }
            loadedEnvs[target] = loaded
        }
    }

    DisposableEffect(environmentLoader) {
        onDispose {
            loadedEnvs.values.distinct().forEach { env ->
                runCatching { environmentLoader.destroyEnvironment(env) }
            }
        }
    }

    val targetEnv = LIGHT_PRESETS[lightPreset].envName
    // 切换瞬间如果新 env 还没加载完,fallback 到上一帧可见的 env(不是强制用 noon,避免视觉跳变)
    val environment = loadedEnvs[targetEnv] ?: initialEnv

    LaunchedEffect(cameraMode) {
        if (cameraMode == CameraMode.WALK) cam.syncWalkOrientation()
        cam.isWalk = cameraMode == CameraMode.WALK
    }

    LaunchedEffect(centered) { if (centered) cam.applyToCamera(cameraNode) }
    // 首次 + lightPreset 变化时同步光照
    LaunchedEffect(lightPreset) {
        applyPreviewLightPreset(
            mainLight = sunLight,
            fillLight = fillLight,
            preset = LIGHT_PRESETS[lightPreset],
        )
    }

    LaunchedEffect(layerY, modelRoot) {
        val root = modelRoot?.let { findLayerRoot(it) } ?: return@LaunchedEffect
        if (root.childNodes.isNotEmpty()) {
            root.childNodes.forEachIndexed { index, child ->
                child.isVisible = (layerY == Int.MAX_VALUE) || (index <= layerY)
            }
        }
    }

    val toolbarIconOn = androidx.compose.ui.graphics.Color.White
    val toolbarIconOff = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f)

    Box(modifier = Modifier
        .fillMaxSize()
        .background(androidx.compose.ui.graphics.Color.Black)  // 全屏时覆盖 window 白底
    ) {
        val context = LocalView.current.context
        var modelError by remember { mutableStateOf(false) }
        var modelErrorMessage by remember { mutableStateOf<String>(context.getString(R.string.preview_render_failed)) }
        // loadModelInstanceAsync is the SceneView-native async loader: reads
        // bytes on IO, decodes textures on a worker, creates Filament objects
        // on Main. The onResult callback delivers ModelInstance? (null on
        // parse/IO failure).
        //
        // We pass a file:// URI rather than a bare absolute path. The bare-path
        // form was routing SceneView into its asset-based code path
        // (AssetManager.open() throws FileNotFoundException for files outside
        // the APK), and rememberModelInstance silently swallowed that throw and
        // returned null forever — looking like a hang.
        val filePath = android.net.Uri.fromFile(glbFile).toString()
        android.util.Log.d("PREVIEW", "[$uuid] loadModelInstanceAsync start, path=$filePath exists=${glbFile.exists()} size=${glbFile.length()}")
        var modelInst by remember { mutableStateOf<ModelInstance?>(null) }
        LaunchedEffect(filePath) {
            modelInst = null
            android.util.Log.d("PREVIEW", "[$uuid] loadModelInstanceAsync launched, path=$filePath")
            modelLoader.loadModelInstanceAsync(filePath) { result ->
                android.util.Log.d("PREVIEW", "[$uuid] loadModelInstanceAsync result: ${result?.javaClass?.simpleName ?: "null"}")
                modelInst = result
            }
        }
        LaunchedEffect(modelInst) {
            android.util.Log.d("PREVIEW", "[$uuid] modelOnScreen flip from loadModelInstanceAsync: ${modelInst != null}")
            modelOnScreen = modelInst != null
        }
        // Timeout fallback: if modelOnScreen doesn't flip true within 8s,
        // force the HUD off and surface the file path + state via snackbar
        // so we can diagnose silent loader hangs.
        LaunchedEffect(entry) {
            kotlinx.coroutines.delay(8_000)
            if (!modelOnScreen) {
                android.util.Log.w("PREVIEW", "[$uuid] modelOnScreen stuck false after 8s; forcing loadingVisible=false. file=$filePath exists=${glbFile.exists()} size=${glbFile.length()}")
                loadingVisible = false
                if (modelInst == null) {
                    modelError = true
                    modelErrorMessage = "模型加载超时: ${glbFile.name}"
                    snackbar.showSnackbar(modelErrorMessage)
                }
            }
        }
        // Surface load errors as a snackbar (e.g. missing file). With
        // loadModelInstanceAsync, parse failures arrive as a null result in
        // the callback — we keep the explicit size check as a soft fallback
        // so we don't have to model every failure mode in the loader path.
        LaunchedEffect(glbFile) {
            modelError = !glbFile.isFile
            if (!glbFile.isFile) {
                modelErrorMessage = context.getString(R.string.preview_resource_missing)
            }
        }
        LaunchedEffect(modelError) { if (modelError) snackbar.showSnackbar(modelErrorMessage) }

        var lastFrameNanos by remember { mutableStateOf(0L) }

        Box(modifier = Modifier.fillMaxSize().alpha(modelAlpha)) {
            key(glbFile) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            environmentLoader = environmentLoader,
            environment = environment,
            surfaceType = io.github.sceneview.SurfaceType.TextureSurface,
            isOpaque = true,
            cameraNode = cameraNode,
            view = filamentView,
            cameraManipulator = null,
            mainLightNode = sunLight,
            fillLightNode = fillLight,
            autoCenterContent = false,
            onFrame = { frameTimeNanos ->
                if (modelInst != null && !modelOnScreen) modelOnScreen = true
                val delta = if (lastFrameNanos > 0) ((frameTimeNanos - lastFrameNanos) / 1e9f).coerceIn(0f, 0.1f) else 0f
                lastFrameNanos = frameTimeNanos
                if (cam.isWalk) cam.applyWalkMove(delta)
                cam.applyToCamera(cameraNode)
            },
            onTouchEvent = null,
        ) {
            val lightCfg = LIGHT_PRESETS[lightPreset]
            val gridMat = remember(materialLoader) { materialLoader.createColorInstance(android.graphics.Color.argb(100, 128, 128, 128)) }
            val redMat = remember(materialLoader) { materialLoader.createColorInstance(android.graphics.Color.RED) }
            val greenMat = remember(materialLoader) { materialLoader.createColorInstance(android.graphics.Color.GREEN) }
            val blueMat = remember(materialLoader) { materialLoader.createColorInstance(android.graphics.Color.BLUE) }
            if (modelInst != null) ModelNode(
                modelInstance = modelInst!!,
                apply = {
                    cam.setTargetFromNode(this)
                    centered = true
                    modelRoot = this
                    floorCount = findLayerCount(this)
                },
            )
            // 当前层高亮平面
            val layerPlaneMat = remember(materialLoader) {
                materialLoader.createColorInstance(android.graphics.Color.argb(80, 0, 200, 255))
            }
            if (layerY != Int.MAX_VALUE && centered) {
                val planeY = cam.gridY + layerY + 0.5f
                val lx = cam.anchorX; val lz = cam.anchorZ; val s = 32f
                listOf(
                    Triple(lx-s, planeY, lz-s) to Triple(lx+s, planeY, lz-s),
                    Triple(lx-s, planeY, lz+s) to Triple(lx+s, planeY, lz+s),
                    Triple(lx-s, planeY, lz-s) to Triple(lx-s, planeY, lz+s),
                    Triple(lx+s, planeY, lz-s) to Triple(lx+s, planeY, lz+s),
                ).forEach { (a, b) ->
                    LineNode(Position(a.first, a.second, a.third), Position(b.first, b.second, b.third), materialInstance = layerPlaneMat)
                }
            }
            key(centered) {
                val ax = cam.targetX + cam.gridOffX; val az = cam.targetZ + cam.gridOffZ; val ay = cam.targetY
                val gy = cam.gridY
                LineNode(Position(ax, ay, az), Position(ax + 4f, ay, az), materialInstance = redMat)
                LineNode(Position(ax, ay, az), Position(ax, ay + 4f, az), materialInstance = greenMat)
                LineNode(Position(ax, ay, az), Position(ax, ay, az + 4f), materialInstance = blueMat)
                if (showGrid) {
                    val n = (GRID_SIZE / GRID_STEP).toInt(); val half = GRID_SIZE
                    for (i in -n..n) { val o = ax + i * GRID_STEP; LineNode(Position(o, gy, az - half), Position(o, gy, az + half), materialInstance = gridMat) }
                    for (i in -n..n) { val o = az + i * GRID_STEP; LineNode(Position(ax - half, gy, o), Position(ax + half, gy, o), materialInstance = gridMat) }
                }
            }
        }
        } // key(glbFile)
        } // Box fade-in alpha

        // HUD 风格启动屏 — 一直显示到 SceneView 渲染出首帧 + 250ms
        HudStartupOverlay(visible = loadingVisible && !modelError)

        // 旋转手势层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cameraMode) {
                    if (cameraMode == CameraMode.WALK) {
                        detectDragGestures { change, _ ->
                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            if (dx != 0f || dy != 0f)
                                cam.walkRotateRaw(-dx * WALK_ROTATE_SPEED, -dy * WALK_ROTATE_SPEED)
                        }
                    } else {
                        // 单指旋转/拖拽 + 双指缩放统一处理
                        var p1id: Long = -1L; var p2id: Long = -1L
                        var prev1 = Offset.Zero; var prev2 = Offset.Zero
                        var prevDist = 0f
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            p1id = down.id.value; prev1 = down.position; p2id = -1L; prevDist = 0f
                            do {
                                val ev = awaitPointerEvent()
                                val active = ev.changes.filter { it.pressed }
                                var p1c = active.firstOrNull { it.id.value == p1id }
                                var p2c = active.firstOrNull { it.id.value == p2id }
                                // 第二指
                                if (p2id < 0 && active.size >= 2) {
                                    p2c = active.firstOrNull { it.id.value != p1id }
                                    if (p2c != null && p2c.previousPressed && p1c != null) {
                                        p2id = p2c.id.value; prev2 = p2c.position
                                        prevDist = (p1c.position - prev2).getDistance()
                                    }
                                }
                                // 指针离开先处理
                                if (p1c == null) {
                                    p1id = p2id; p2id = -1L; prevDist = 0f
                                    p1c = p2c; p2c = null
                                    if (p1c != null) prev1 = p1c.position // 用新 p1 当前位置,避免跳变
                                }
                                else if (p2c == null) { p2id = -1L; prevDist = 0f }
                                // 应用手势
                                if (p2c != null && p1c != null) {
                                    val d = (p1c.position - p2c.position).getDistance()
                                    if (prevDist > 0 && d > 0) {
                                        val factor = (d / prevDist).coerceIn(0.5f, 2f)
                                        if (factor != 1f) cam.zoomRaw(factor)
                                    }
                                    prevDist = d
                                    prev1 = p1c.position // 持续更新,避免单指离开后 delta 跳变
                                } else if (p1c != null) {
                                    val dx = p1c.position.x - prev1.x
                                    val dy = p1c.position.y - prev1.y
                                    if (dx != 0f || dy != 0f) {
                                        if (cameraMode == CameraMode.ORBIT)
                                            cam.orbitRaw(-dx * ORBIT_SPEED, -dy * ORBIT_SPEED)
                                        else
                                            cam.dragRaw(-dx * ORBIT_SPEED, dy * ORBIT_SPEED)
                                    }
                                    prev1 = p1c.position
                                }
                                ev.changes.forEach { if (it.pressed) it.consume() }
                            } while (active.isNotEmpty())
                        }
                    }
                },
            )

        // 摇杆(Walk 模式)
        if (cameraMode == CameraMode.WALK) {
            WalkJoystick(
                onMove = { f, r -> cam.setWalkInput(f, r) },
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 24.dp),
            )
        }

        // 工具栏 — 加半透明深色背景保证在全屏时可见
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
        Row(
            horizontalArrangement = Arrangement.End,
        ) {
            ToolIcon(Icons.Default.Refresh, stringResource(R.string.cd_reset), toolbarIconOn) { cam.reset(); cam.applyToCamera(cameraNode) }
            Box(Modifier.clickable { lightPreset = (lightPreset + 1) % LIGHT_PRESETS.size }.padding(4.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LightMode, LIGHT_PRESETS[lightPreset].label, Modifier.size(22.dp), tint = if (lightPreset != 2) toolbarIconOn else toolbarIconOff)
                    Spacer(Modifier.width(4.dp))
                    Text(LIGHT_PRESETS[lightPreset].label, style = MaterialTheme.typography.labelSmall, color = toolbarIconOn, maxLines = 1)
                }
            }
            ToolIcon(Icons.Default.GridOn, stringResource(R.string.cd_grid), if (showGrid) toolbarIconOn else toolbarIconOff) { showGrid = !showGrid }
            ToolIcon(Icons.Default.Layers, stringResource(R.string.cd_layer), if (layerPanelOpen) toolbarIconOn else toolbarIconOff) { layerPanelOpen = !layerPanelOpen }
            Box(Modifier.clickable { cameraMode = CameraMode.entries[(cameraMode.ordinal + 1) % CameraMode.entries.size] }.padding(4.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(when (cameraMode) {
                        CameraMode.ORBIT -> Icons.Default.ViewInAr; CameraMode.DRAG -> Icons.Default.OpenWith; CameraMode.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
                    }, cameraMode.label, Modifier.size(22.dp), tint = toolbarIconOn)
                    Spacer(Modifier.width(4.dp))
                    Text(cameraMode.label, style = MaterialTheme.typography.labelSmall, color = toolbarIconOn, maxLines = 1)
                }
            }
            ToolIcon(if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, stringResource(R.string.cd_fullscreen), toolbarIconOn) {
                fullscreen = !fullscreen
                onFullscreenChange?.invoke(fullscreen)
            }
        }
        } // Box toolbar wrapper
        // 右侧分层控制面板 — 紧凑工具栏风格
        if (layerPanelOpen) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(8.dp)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 标题
                Text(
                    stringResource(R.string.layer_panel_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                Spacer(Modifier.height(6.dp))
                // 当前层显示
                Text(
                    if (layerY == Int.MAX_VALUE) stringResource(R.string.layer_all)
                    else "${layerY + 1} / $floorCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = androidx.compose.ui.graphics.Color.White,
                )
                Spacer(Modifier.height(6.dp))
                // + / - 紧凑按钮
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LayerIconBtn(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "+1",
                        enabled = layerY == Int.MAX_VALUE || layerY < floorCount - 1,
                        onClick = {
                            if (layerY == Int.MAX_VALUE) layerY = 0
                            else if (layerY < floorCount - 1) layerY++
                        },
                    )
                    Spacer(Modifier.width(4.dp))
                    LayerIconBtn(
                        icon = Icons.Default.KeyboardArrowDown,
                        contentDescription = "-1",
                        enabled = layerY != Int.MAX_VALUE,
                        onClick = { if (layerY > 0) layerY-- else layerY = Int.MAX_VALUE },
                    )
                }
                Spacer(Modifier.height(6.dp))
                // 显示全部 toggle
                LayerIconBtn(
                    icon = Icons.Default.Layers,
                    contentDescription = stringResource(R.string.layer_show_all),
                    enabled = layerY != Int.MAX_VALUE,
                    onClick = { layerY = Int.MAX_VALUE },
                )
            }
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Walk the subtree to find the entity with the most direct children — the layer container. */
private fun findLayerRoot(node: Node): Node? {
    var best: Node? = null
    var bestCount = 0
    for (child in node.childNodes) {
        if (child.childNodes.size > bestCount) {
            bestCount = child.childNodes.size
            best = child
        }
        findLayerRoot(child)?.let { deeper -> if (deeper.childNodes.size > bestCount) { bestCount = deeper.childNodes.size; best = deeper } }
    }
    return best
}

/** Walk the subtree to find the maximum child count at any depth — the layer count. */
private fun findLayerCount(node: Node): Int {
    var max = 0
    for (child in node.childNodes) {
        max = maxOf(max, child.childNodes.size)
        max = maxOf(max, findLayerCount(child))
    }
    return max
}
