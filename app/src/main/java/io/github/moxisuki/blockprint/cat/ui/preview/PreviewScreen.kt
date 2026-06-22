package io.github.moxisuki.blockprint.cat.ui.preview

import android.app.Activity
import io.github.moxisuki.blockprint.cat.glb.GlbGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import io.github.sceneview.SceneView
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.colorOf
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.LineNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberView
import io.github.sceneview.environment.Environment
import io.github.sceneview.rememberFillLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.moxisuki.blockprint.cat.R
import androidx.compose.runtime.key
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val TAG = "PreviewScreen"
private const val GRID_SIZE = 64f
private const val GRID_STEP = 1f
private const val ORBIT_SPEED = 0.005f
private const val DRAG_SPEED = 0.15f
private const val ZOOM_STRENGTH = 5f
private const val WALK_ROTATE_SPEED = 0.004f
private const val WALK_MOVE_SPEED = 12f
private const val JOYSTICK_RADIUS_DP = 72
private const val THUMB_RADIUS_DP = 32

enum class CameraMode(val label: String) {
    ORBIT("旋转"), WALK("摇杆"), DRAG("拖拽"),
}

private data class LightPreset(
    val label: String,
    val timeOfDay: Float,
    val sunIntensity: Float,
    val fillIntensity: Float,
    val envName: String = "neutral",  // 对应 assets/environments/<envName>/ 目录
    val celestialAzimuthDeg: Float,
    val celestialElevationDeg: Float,
)
private val LIGHT_PRESETS = arrayOf(
    // 天体位置和 skybox 生成器保持一致:白天/黄昏=太阳,夜晚=月亮
    LightPreset("白天", timeOfDay = 12f, sunIntensity = 140_000f, fillIntensity = 10_000f, envName = "noon",   celestialAzimuthDeg = 135f, celestialElevationDeg = 55f),
    LightPreset("黄昏", timeOfDay = 18f, sunIntensity = 90_000f,  fillIntensity = 8_000f,  envName = "sunset", celestialAzimuthDeg = 270f, celestialElevationDeg = 2f),
    LightPreset("夜晚", timeOfDay = 0f,  sunIntensity = 5_000f,   fillIntensity = 1_200f,  envName = "night",  celestialAzimuthDeg = 110f, celestialElevationDeg = 45f),
    // 影棚模式仍保留中性方向,不追求 sky 天体一致性
    LightPreset("影棚", timeOfDay = 12f, sunIntensity = 30_000f,  fillIntensity = 60_000f, envName = "studio", celestialAzimuthDeg = 135f, celestialElevationDeg = 55f),
)

@Composable
fun PreviewScreen(
    uuid: String,
    navController: androidx.navigation.NavController,
    onFullscreenChange: ((Boolean) -> Unit)? = null,
) {
    var glbEntry by remember { mutableStateOf<GlbEntry?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val generator = remember { io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager.generator }
    val context = LocalView.current.context
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
        val cached = io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager.peekGlb(uuid)
        if (cached != null) { glbEntry = GlbEntry(cached.bytes, cached.minY, cached.centerX, cached.centerZ); return@LaunchedEffect }
        try {
            val entry = withContext(Dispatchers.IO) {
                val lit = io.github.moxisuki.blockprint.cat.ui.render.RenderResourceManager.takeLitematic(uuid)
                    ?: blueprintManager.loadDetail(uuid)?.raw
                    ?: throw IllegalStateException("蓝图不存在或已被删除")
                if (lit.blockCount() == 0) throw IllegalStateException("该蓝图不包含任何方块")
                val bytes = generator?.generate(lit, cacheKey = uuid, floorHeight = GlbGenerator.LAYER_FLOOR_HEIGHT)
                    ?: throw IllegalStateException("渲染引擎未初始化")
                val reg = lit.regions.getOrNull(0)
                GlbEntry(bytes, minY = reg?.let { it.position.y - it.height / 2 }?.toFloat() ?: 0f,
                    centerX = reg?.position?.x?.toFloat() ?: 0f, centerZ = reg?.position?.z?.toFloat() ?: 0f)
            }
            glbEntry = entry
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
        glbEntry != null -> PreviewSceneContent(entry = glbEntry!!, onFullscreenChange = onFullscreenChange)
        else -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.preview_loading), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class GlbEntry(val bytes: ByteArray, val minY: Float, val centerX: Float, val centerZ: Float)

/**
 * 一次完成预设的完整光状态更新：
 * - fill light intensity
 * - main light direction / color / intensity
 *
 * 让预设切换的副作用集中到唯一调用点，避免分散更新导致中间帧状态不一致。
 */
private fun applyPreviewLightPreset(
    mainLight: LightNode,
    fillLight: LightNode,
    preset: LightPreset,
) {
    fillLight.intensity = preset.fillIntensity
    updateMCNoDiscSunLight(
        lightNode = mainLight,
        timeOfDay = preset.timeOfDay,
        sunIntensity = preset.sunIntensity,
        celestialAzimuthDeg = preset.celestialAzimuthDeg,
        celestialElevationDeg = preset.celestialElevationDeg,
    )
}

@Composable
private fun PreviewSceneContent(
    entry: GlbEntry,
    onFullscreenChange: ((Boolean) -> Unit)? = null,
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
    val glbBytes = entry.bytes

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
    var floorCount by remember { mutableIntStateOf(0) }
    var modelRoot by remember { mutableStateOf<Node?>(null) }
    val snackbar = remember { SnackbarHostState() }

    // 预加载 4 套环境并在预览页面生命周期内复用，避免切换预设时重复创建 KTX 环境。
    val cachedEnvironments = remember(environmentLoader) {
        fun loadEnv(name: String): Environment {
            return try {
                environmentLoader.createKTX1Environment(
                    iblAssetFile = "environments/$name/${name}_ibl.ktx",
                    skyboxAssetFile = "environments/$name/${name}_skybox.ktx",
                )
            } catch (e: Exception) {
                Log.w(TAG, "env=$name KTX 缺失,使用 fallback neutral: ${e.message}")
                environmentLoader.createKTX1Environment(
                    iblAssetFile = "environments/neutral/neutral_ibl.ktx",
                    skyboxAssetFile = "environments/neutral/neutral_skybox.ktx",
                )
            }.also { env ->
                Log.i(TAG, "env=$name loaded: skybox=${env.skybox != null}, ibl=${env.indirectLight != null}")
            }
        }

        mapOf(
            "noon" to loadEnv("noon"),
            "sunset" to loadEnv("sunset"),
            "night" to loadEnv("night"),
            "studio" to loadEnv("studio"),
        )
    }

    DisposableEffect(environmentLoader, cachedEnvironments) {
        onDispose {
            cachedEnvironments.values.distinct().forEach { env ->
                runCatching { environmentLoader.destroyEnvironment(env) }
            }
        }
    }

    val targetEnv = LIGHT_PRESETS[lightPreset].envName
    val environment = cachedEnvironments[targetEnv] ?: cachedEnvironments.getValue("noon")

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

    val iconOn = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val iconOff = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val toolbarIconOn = androidx.compose.ui.graphics.Color.White
    val toolbarIconOff = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.4f)

    Box(modifier = Modifier
        .fillMaxSize()
        .background(androidx.compose.ui.graphics.Color.Black)  // 全屏时覆盖 window 白底
    ) {
        val context = LocalView.current.context
        var modelError by remember { mutableStateOf(false) }
        var modelErrorMessage by remember { mutableStateOf<String>(context.getString(R.string.preview_render_failed)) }
        val modelInst = remember(glbBytes) {
            modelError = false
            try {
                if (glbBytes.size < 2000) { modelErrorMessage = context.getString(R.string.preview_resource_missing); modelError = true; return@remember null }
                val buffer = java.nio.ByteBuffer.allocateDirect(glbBytes.size).apply { put(glbBytes); flip() }
                modelLoader.createModelInstance(buffer)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "模型加载失败: ${e.message}", e)
                modelErrorMessage = if (e.message?.contains("Empty vertex") == true) context.getString(R.string.preview_resource_missing)
                else context.getString(R.string.preview_render_failed_with_msg, e.message ?: "")
                modelError = true; null
            }
        }
        LaunchedEffect(modelError) { if (modelError) snackbar.showSnackbar(modelErrorMessage) }

        var lastFrameNanos by remember { mutableStateOf(0L) }

        key(glbBytes) {
        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            environmentLoader = environmentLoader,
            environment = environment,
            surfaceType = io.github.sceneview.SurfaceType.Surface,
            isOpaque = true,
            cameraNode = cameraNode,
            view = filamentView,
            cameraManipulator = null,
            mainLightNode = sunLight,
            fillLightNode = fillLight,
            autoCenterContent = false,
            onFrame = { frameTimeNanos ->
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
        } // key(glbBytes)

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
                                    if (p1c != null) prev1 = p1c.position // 用新 p1 当前位置，避免跳变
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
                                    prev1 = p1c.position // 持续更新，避免单指离开后 delta 跳变
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

        // 摇杆（Walk 模式）
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

// ── 摇杆 ──

@Composable
private fun WalkJoystick(
    onMove: (forward: Float, right: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseR = with(LocalDensity.current) { JOYSTICK_RADIUS_DP.dp.toPx() }
    val thumbR = with(LocalDensity.current) { THUMB_RADIUS_DP.dp.toPx() }
    var tx by remember { mutableFloatStateOf(0f) }; var ty by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val sf = MaterialTheme.colorScheme.surface; val onSf = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .size((JOYSTICK_RADIUS_DP * 2).dp)
            .clip(CircleShape).background(sf.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false; tx = 0f; ty = 0f; onMove(0f, 0f) },
                    onDragCancel = { dragging = false; tx = 0f; ty = 0f; onMove(0f, 0f) },
                    onDrag = { change, _ ->
                        // 不 consume：让 SceneView 也能收到触摸（多指兼容）
                        val rx = change.position.x - baseR; val ry = change.position.y - baseR
                        val d = sqrt(rx * rx + ry * ry); val maxD = baseR - thumbR
                        val s = if (d > 0.01f) d.coerceAtMost(maxD) / d else 0f
                        tx = rx * s; ty = ry * s
                        onMove(-(ty / maxD), tx / maxD)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(thumbR.dp).offset { IntOffset(tx.roundToInt(), ty.roundToInt()) }
            .clip(CircleShape).background(onSf.copy(alpha = if (dragging) 0.55f else 0.35f)))
    }
}

// ── 工具栏 ──

@Composable
private fun ToolIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, tint: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(Modifier.clickable(onClick = onClick).padding(4.dp)) { Icon(icon, desc, Modifier.size(22.dp), tint = tint) }
}

@Composable
private fun LayerIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) androidx.compose.ui.graphics.Color.White
               else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(20.dp), tint = tint)
    }
}

// ── 相机控制器 ──

private class CameraController(
    var eyeX: Float, var eyeY: Float, var eyeZ: Float,
    var targetX: Float, var targetY: Float, var targetZ: Float,
) {
    var gridY = 0f; var gridOffX = 0f; var gridOffZ = 0f
    var anchorX = 0f; var anchorY = 0f; var anchorZ = 0f
    private var initEyeX: Float; private var initEyeY: Float; private var initEyeZ: Float
    private var initTargetX: Float; private var initTargetY: Float; private var initTargetZ: Float
    private var walkYaw: Float; private var walkPitch: Float
    private var initWalkYaw: Float; private var initWalkPitch: Float

    @Volatile var walkForward = 0f; @Volatile var walkRight = 0f
    @Volatile var isWalk = false
    init {
        initEyeX = eyeX; initEyeY = eyeY; initEyeZ = eyeZ
        initTargetX = targetX; initTargetY = targetY; initTargetZ = targetZ
        val dx = targetX - eyeX; val dy = targetY - eyeY; val dz = targetZ - eyeZ
        walkYaw = atan2(dx, dz); walkPitch = atan2(dy, sqrt(dx * dx + dz * dz))
        initWalkYaw = walkYaw; initWalkPitch = walkPitch
    }

    fun setWalkInput(f: Float, r: Float) { walkForward = f.coerceIn(-1f, 1f); walkRight = r.coerceIn(-1f, 1f) }
    /** 从当前 eye→target 向量重新计算 walkYaw/Pitch，避免切换 Walk 模式时视角突变 */
    fun syncWalkOrientation() {
        val dx = targetX - eyeX; val dy = targetY - eyeY; val dz = targetZ - eyeZ
        walkYaw = atan2(dx, dz)
        walkPitch = atan2(dy, sqrt(dx * dx + dz * dz))
    }
    fun reset() {
        eyeX = initEyeX; eyeY = initEyeY; eyeZ = initEyeZ
        targetX = initTargetX; targetY = initTargetY; targetZ = initTargetZ
        walkYaw = initWalkYaw; walkPitch = initWalkPitch; walkForward = 0f; walkRight = 0f
    }
    fun setTargetFromNode(node: ModelNode) {
        val b = node.boundingBox; val c = b.center
        eyeX += c[0] - targetX; eyeY += c[1] - targetY; eyeZ += c[2] - targetZ
        targetX = c[0]; targetY = c[1]; targetZ = c[2]
        anchorX = c[0]; anchorY = c[1]; anchorZ = c[2]
        gridY = c[1] - b.halfExtent[1]
        // 网格线对齐方块边缘：奇方块数偏移 0.5
        gridOffX = if ((b.halfExtent[0] * 2).toInt() % 2 == 1) 0.5f else 0f
        gridOffZ = if ((b.halfExtent[2] * 2).toInt() % 2 == 1) 0.5f else 0f
        initEyeX = eyeX; initEyeY = eyeY; initEyeZ = eyeZ
        initTargetX = targetX; initTargetY = targetY; initTargetZ = targetZ
        val dx = targetX - eyeX; val dy = targetY - eyeY; val dz = targetZ - eyeZ
        walkYaw = atan2(dx, dz); walkPitch = atan2(dy, sqrt(dx * dx + dz * dz))
        initWalkYaw = walkYaw; initWalkPitch = walkPitch
    }

    // ── 由 rotation overlay 调用（公开的 delta 方法）──
    fun orbitRaw(dh: Float, dv: Float) = orbit(dh, dv)
    fun dragRaw(dx: Float, dy: Float) = drag(dx, dy)
    fun walkRotateRaw(dyaw: Float, dpitch: Float) = walkRotate(dyaw, dpitch)
    fun zoomRaw(factor: Float) = zoom(factor)

    // ── Walk ──

    private fun walkRotate(dYaw: Float, dPitch: Float) { walkYaw += dYaw; walkPitch = (walkPitch + dPitch).coerceIn(-1.5f, 1.5f) }

    fun applyWalkMove(dt: Float) {
        if (walkForward == 0f && walkRight == 0f) return
        val spd = WALK_MOVE_SPEED * dt
        val fx = cos(walkPitch) * sin(walkYaw); val fy = sin(walkPitch); val fz = cos(walkPitch) * cos(walkYaw)
        val rx = -fz; val rz = fx; val rl = sqrt(rx * rx + rz * rz)
        val nrx = if (rl > 0.001f) rx / rl else 0f; val nrz = if (rl > 0.001f) rz / rl else 0f
        eyeX += (fx * walkForward + nrx * walkRight) * spd
        eyeY += fy * walkForward * spd
        eyeZ += (fz * walkForward + nrz * walkRight) * spd
    }

    fun applyToCamera(cameraNode: CameraNode) {
        val (cx, cy, cz) = if (isWalk) {
            val ld = 10f
            Triple(eyeX + cos(walkPitch) * sin(walkYaw) * ld,
                   eyeY + sin(walkPitch) * ld,
                   eyeZ + cos(walkPitch) * cos(walkYaw) * ld)
        } else {
            Triple(targetX, targetY, targetZ)
        }
        cameraNode.lookAt(eye = Position(eyeX, eyeY, eyeZ), center = Position(cx, cy, cz), up = Position(0f, 1f, 0f))
    }

    // ── Orbit / Drag / Zoom ──

    private fun orbit(dH: Float, dV: Float) {
        val ox = eyeX - targetX; val oy = eyeY - targetY; val oz = eyeZ - targetZ
        val d = sqrt(ox * ox + oy * oy + oz * oz); if (d < 0.01f) return
        val t = atan2(ox, oz) + dH; val p = (asin((oy / d).coerceIn(-1f, 1f)) + dV).coerceIn(-1.5f, 1.5f)
        eyeX = targetX + d * cos(p) * sin(t); eyeY = targetY + d * sin(p); eyeZ = targetZ + d * cos(p) * cos(t)
    }
    private fun drag(dx: Float, dy: Float) {
        val ox = eyeX - targetX; val oz = eyeZ - targetZ; val d = sqrt(ox * ox + oz * oz).coerceAtLeast(0.1f)
        val rx = -oz / d; val rz = ox / d; val s = d * DRAG_SPEED
        val mx = rx * dx * s; val mz = rz * dx * s; val my = dy * s
        targetX -= mx; targetY -= my; targetZ -= mz; eyeX -= mx; eyeY -= my; eyeZ -= mz
    }
    private fun zoom(f: Float) {
        val s = 1f + (f - 1f) * ZOOM_STRENGTH
        val dx = eyeX - targetX; val dy = eyeY - targetY; val dz = eyeZ - targetZ
        val d = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.5f); val nd = (d / s).coerceIn(1.5f, 500f); val r = nd / d
        eyeX = targetX + dx * r; eyeY = targetY + dy * r; eyeZ = targetZ + dz * r
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PreviewEntryPoint {
    fun blueprintManager(): io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
    companion object {
        fun resolve(context: android.content.Context): io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager {
            return dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, PreviewEntryPoint::class.java).blueprintManager()
        }
    }
}

// ── MC 方块太阳/月亮 ──

/**
 * 跟 SceneView 自带 DynamicSkyNode 的方向/颜色/强度逻辑一致,但**不用 SUN 光源**。
 *
 * 改用 [LightManager.Type.DIRECTIONAL] 后,Filament 不会再画可见的太阳圆盘/halo,
 * 场景里只剩 skybox 本身画出来的太阳/月亮,彻底避免"两个太阳"。
 *
 * 返回的 LightNode 通过 SceneView(mainLightNode = ...) 参数挂到场景,生命周期由
 * DisposableEffect 负责。方向/颜色/强度用 [updateMCNoDiscSunLight] 实时更新。
 */
@Composable
private fun rememberNoDiscSunLight(engine: com.google.android.filament.Engine): LightNode {
    val lightNode = remember(engine) {
        LightNode(
            engine = engine,
            entity = EntityManager.get().create(),
            builder = LightManager.Builder(LightManager.Type.DIRECTIONAL).apply {
                intensity(110_000f)
                color(1f, 1f, 1f)
                castShadows(true)
            }
        )
    }
    DisposableEffect(lightNode) {
        onDispose { lightNode.destroy() }
    }
    return lightNode
}

/**
 * 按 timeOfDay 更新太阳光的方向/颜色/强度。公式直接搬自 DynamicSkyNodeImpl.update()。
 * 跟 [rememberNoDiscSunLight] 配套使用。
 */
private fun updateMCNoDiscSunLight(
    lightNode: LightNode,
    timeOfDay: Float,
    sunIntensity: Float,
    celestialAzimuthDeg: Float,
    celestialElevationDeg: Float,
    turbidity: Float = 2f,
) {
    // 方向直接来自 skybox 里天体的位置:天上太阳/月亮在哪,光就从哪打下来
    val az = Math.toRadians(celestialAzimuthDeg.toDouble()).toFloat()
    val el = Math.toRadians(celestialElevationDeg.toDouble()).toFloat()
    val dirX = kotlin.math.sin(az) * kotlin.math.cos(el) * 0.6f
    val dirY = kotlin.math.sin(el)
    val dirZ = -kotlin.math.cos(az) * kotlin.math.cos(el) * 0.5f
    val len = kotlin.math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ).coerceAtLeast(1e-6f)
    val elevation = dirY / len

    lightNode.lightDirection = Direction(x = dirX / len, y = dirY / len, z = dirZ / len)

    lightNode.color = if (timeOfDay < 6f || timeOfDay >= 18f) {
        // 夜晚:方向由月亮驱动,颜色冷白蓝
        colorOf(r = 0.78f, g = 0.84f, b = 0.96f)
    } else {
        // 白天/黄昏:颜色仍沿用太阳逻辑,但方向已经来自天体位置
        val horizonFactor = (1f - elevation.coerceIn(0f, 1f)) * (1f - elevation.coerceIn(0f, 1f))
        val turbidityBoost = ((turbidity - 1f) / 9f).coerceIn(0f, 1f)
        val warmR = 1.0f
        val warmG = 0.45f + 0.05f * turbidityBoost
        val warmB = 0.20f - 0.10f * turbidityBoost
        colorOf(
            r = warmR * horizonFactor + 1.00f * (1f - horizonFactor),
            g = warmG * horizonFactor + 0.98f * (1f - horizonFactor),
            b = warmB * horizonFactor + 0.95f * (1f - horizonFactor),
        )
    }

    // 夜晚也保留方向光,但强度远弱于太阳
    lightNode.intensity = if (timeOfDay < 6f || timeOfDay >= 18f) sunIntensity else sunIntensity * elevation.coerceAtLeast(0f)
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

