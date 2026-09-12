package io.github.moxisuki.blockprint.cat.app.feature.preview

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.preview.PreviewModel
import io.github.moxisuki.blockprint.cat.app.core.preview.PreviewStage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import io.github.sceneview.RenderQuality
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Position
import io.github.sceneview.node.LineNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberFillLightNode
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberView
import io.github.sceneview.model.ModelInstance
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.RichTooltipBox
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.rememberTooltipState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.RotateLeft
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PreviewScreen(
    state: PreviewState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface),
    ) {
        when {
            state.errorMessage != null -> PreviewError(
                message = state.errorMessage,
                onBack = onBack,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            state.model == null -> PreviewLoading(state = state, modifier = Modifier.fillMaxSize())
            else -> PreviewCanvas(
                model = state.model,
                onBack = onBack,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PreviewLoading(state: PreviewState, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = state.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 260),
        label = "previewGenerationProgress",
    )
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = MiuixIcons.GridView,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.preview_loading),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.title4,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stageLabel(state.stage),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.preview_generating_percent, (animatedProgress * 100).roundToInt()),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
        )
    }
}

@Composable
private fun PreviewError(
    message: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = MiuixIcons.Info,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.error,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.preview_failed),
            color = MiuixTheme.colorScheme.onSurface,
            style = MiuixTheme.textStyles.title4,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onBack) {
                Icon(MiuixIcons.Back, stringResource(R.string.preview_back))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.preview_back))
            }
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Icon(MiuixIcons.Refresh, stringResource(R.string.preview_retry), tint = MiuixTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.preview_retry), color = MiuixTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun PreviewCanvas(
    model: PreviewModel,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberCameraNode(engine)
    val view = rememberView(engine)
    val mainLight = rememberMainLightNode(engine)
    val fillLight = rememberFillLightNode(engine)
    val camera = remember { PreviewCameraController() }
    var modelInstance by remember(model.file) { mutableStateOf<ModelInstance?>(null) }
    var modelCentered by remember(model.file) { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var cameraMode by remember { mutableStateOf(PreviewCameraMode.Orbit) }
    var renderError by remember(model.file) { mutableStateOf(false) }
    val modelAlpha by animateFloatAsState(
        targetValue = if (modelInstance != null) 1f else 0f,
        animationSpec = tween(180),
        label = "previewModelAlpha",
    )
    val fileUri = remember(model.file) { Uri.fromFile(model.file).toString() }
    val gridMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(android.graphics.Color.argb(75, 255, 255, 255))
    }
    val gridLines = remember(
        model.file,
        modelCentered,
        camera.gridStartX,
        camera.gridStartZ,
        camera.gridLinesX,
        camera.gridLinesZ,
        camera.gridY,
        gridMaterial,
    ) {
        if (modelCentered) {
            buildPreviewGridLines(
                startX = camera.gridStartX,
                startZ = camera.gridStartZ,
                linesX = camera.gridLinesX,
                linesZ = camera.gridLinesZ,
                y = camera.gridY,
            )
        } else {
            emptyList()
        }
    }

    LaunchedEffect(model.file) {
        modelInstance = null
        modelCentered = false
        renderError = false
        modelLoader.loadModelInstanceAsync(fileUri) { loaded ->
            modelInstance = loaded
            renderError = loaded == null
        }
    }

    LaunchedEffect(cameraMode) {
        camera.setMode(cameraMode)
    }

    Box(modifier = modifier.background(MiuixTheme.colorScheme.surface)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(modelAlpha)
                .pointerInput(model.file, cameraMode) {
                    awaitPreviewGestures(
                        camera = camera,
                        mode = cameraMode,
                    )
                },
        ) {
            SceneView(
                modifier = Modifier.fillMaxSize(),
                surfaceType = SurfaceType.TextureSurface,
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                view = view,
                renderQuality = RenderQuality.Performance,
                isOpaque = true,
                cameraNode = cameraNode,
                mainLightNode = mainLight,
                fillLightNode = fillLight,
                cameraManipulator = null,
                autoCenterContent = false,
                onFrame = { frameTimeNanos ->
                    camera.onFrame(frameTimeNanos, cameraNode)
                },
            ) {
                modelInstance?.let { instance ->
                    ModelNode(
                        modelInstance = instance,
                        centerOrigin = Position(0f, 0f, 0f),
                        apply = {
                            if (!modelCentered) {
                                camera.centerOn(this)
                                camera.apply(cameraNode)
                                modelCentered = true
                            }
                        },
                    )
                }
                if (showGrid) {
                    gridLines.forEachIndexed { index, line ->
                        androidx.compose.runtime.key(index) {
                            LineNode(
                                start = line.start,
                                end = line.end,
                                materialInstance = gridMaterial,
                            )
                        }
                    }
                }
            }
        }

        PreviewTopBar(
            title = model.title,
            fromCache = model.fromCache,
            missingNamespaces = model.missingNamespaces,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (renderError) {
            PreviewRenderError(onRetry = onRetry, modifier = Modifier.align(Alignment.Center))
        }

        if (cameraMode == PreviewCameraMode.Walk) {
            PreviewJoystick(
                onMove = camera::setWalkInput,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 18.dp, bottom = 18.dp),
            )
        }

        PreviewControls(
            showGrid = showGrid,
            cameraMode = cameraMode,
            onReset = { camera.reset(cameraNode) },
            onToggleGrid = { showGrid = !showGrid },
            onToggleCameraMode = {
                cameraMode = if (cameraMode == PreviewCameraMode.Orbit) {
                    PreviewCameraMode.Walk
                } else {
                    PreviewCameraMode.Orbit
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

@Composable
private fun PreviewTopBar(
    title: String,
    fromCache: Boolean,
    missingNamespaces: Set<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f), RoundedCornerShape(18.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(MiuixIcons.Back, stringResource(R.string.preview_back), tint = MiuixTheme.colorScheme.onSurface)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(
                text = title.ifBlank { stringResource(R.string.preview_title) },
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (fromCache) stringResource(R.string.preview_from_cache)
                else stringResource(R.string.preview_resource_ready),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                maxLines = 1,
            )
        }
        if (missingNamespaces.isNotEmpty()) {
            MissingAssetsTooltip(
                namespaces = missingNamespaces,
                modifier = Modifier.padding(end = 2.dp),
            )
        }
    }
}

@Composable
private fun MissingAssetsTooltip(
    namespaces: Set<String>,
    modifier: Modifier = Modifier,
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    val namespaceText = remember(namespaces) {
        namespaces.sorted().joinToString(", ")
    }

    RichTooltipBox(
        modifier = modifier,
        state = tooltipState,
        title = stringResource(R.string.preview_missing_assets),
        text = stringResource(R.string.preview_missing_assets_detail, namespaceText),
        actionText = stringResource(R.string.preview_missing_assets_dismiss),
        onActionClick = tooltipState::dismiss,
        positioning = TooltipAnchorPosition.Below,
    ) {
        IconButton(
            onClick = {
                scope.launch { tooltipState.show() }
            },
        ) {
            Icon(
                imageVector = MiuixIcons.Info,
                contentDescription = stringResource(R.string.preview_missing_assets),
                tint = MiuixTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PreviewControls(
    showGrid: Boolean,
    cameraMode: PreviewCameraMode,
    onReset: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleCameraMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(end = 14.dp, bottom = 12.dp)
            .animateContentSize(),
        insideMargin = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onReset) {
                Icon(MiuixIcons.Refresh, stringResource(R.string.preview_reset_camera), tint = MiuixTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onToggleGrid) {
                Icon(
                    MiuixIcons.GridView,
                    stringResource(R.string.preview_toggle_grid),
                    tint = if (showGrid) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            IconButton(onClick = onToggleCameraMode) {
                Icon(
                    if (cameraMode == PreviewCameraMode.Walk) {
                        Icons.Filled.Gamepad
                    } else {
                        MiuixIcons.RotateLeft
                    },
                    contentDescription = stringResource(
                        if (cameraMode == PreviewCameraMode.Orbit) {
                            R.string.preview_mode_walk
                        } else {
                            R.string.preview_mode_orbit
                        },
                    ),
                    tint = if (cameraMode == PreviewCameraMode.Walk) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun PreviewRenderError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.padding(24.dp), insideMargin = PaddingValues(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.preview_model_load_failed),
                color = MiuixTheme.colorScheme.onSurface,
                style = MiuixTheme.textStyles.body1,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColorsPrimary()) {
                Icon(MiuixIcons.Refresh, stringResource(R.string.preview_retry), tint = MiuixTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.preview_retry), color = MiuixTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun stageLabel(stage: PreviewStage): String = when (stage) {
    PreviewStage.Preparing -> stringResource(R.string.preview_stage_preparing)
    PreviewStage.Reading -> stringResource(R.string.preview_stage_reading)
    PreviewStage.Generating -> stringResource(R.string.preview_stage_generating)
    PreviewStage.LoadingModel -> stringResource(R.string.preview_stage_loading_model)
    PreviewStage.Ready -> stringResource(R.string.preview_stage_ready)
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.awaitPreviewGestures(
    camera: PreviewCameraController,
    mode: PreviewCameraMode,
) {
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        val ignoredIds = mutableSetOf<PointerId>()
        if (mode == PreviewCameraMode.Walk && firstDown.position.x < size.width * 0.4f) {
            ignoredIds += firstDown.id
        }

        var firstId: PointerId? = firstDown.id.takeUnless { it in ignoredIds }
        var secondId: PointerId? = null
        var previousFirst = firstDown.position
        var previousSecond = firstDown.position
        var previousDistance = 0f
        var previousMidpoint = firstDown.position

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val active = event.changes.filter { it.pressed }
            if (active.isEmpty()) break

            // In walk mode the joystick pointer is independent from the look
            // pointer. Classify new pointers by their initial screen position.
            event.changes
                .filter { it.pressed && !it.previousPressed }
                .forEach { change ->
                    if (mode == PreviewCameraMode.Walk && change.position.x < size.width * 0.4f) {
                        ignoredIds += change.id
                    }
                }

            val cameraActive = active.filterNot { it.id in ignoredIds }
            if (firstId != null && cameraActive.none { it.id == firstId }) {
                firstId = null
                secondId = null
                previousDistance = 0f
            }
            if (secondId != null && cameraActive.none { it.id == secondId }) {
                secondId = null
                previousDistance = 0f
            }
            if (firstId == null) {
                firstId = cameraActive.firstOrNull()?.id
                firstId?.let { id ->
                    previousFirst = cameraActive.first { it.id == id }.position
                }
            }

            var first = firstId?.let { id -> cameraActive.firstOrNull { it.id == id } }
            var second = secondId?.let { id -> cameraActive.firstOrNull { it.id == id } }

            if (first == null && second != null) {
                firstId = second.id
                first = second
                secondId = null
                second = null
                previousFirst = first.position
                previousDistance = 0f
            }

            if (first != null && second == null && secondId == null && cameraActive.size >= 2) {
                second = cameraActive.firstOrNull { it.id != first.id }
                if (second != null) {
                    secondId = second.id
                    previousFirst = first.position
                    previousSecond = second.position
                    previousDistance = (first.position - second.position).getDistance()
                    previousMidpoint = (first.position + second.position) / 2f
                }
            }

            if (first != null && second != null) {
                val midpoint = (first.position + second.position) / 2f
                val distance = (first.position - second.position).getDistance()
                val midpointDelta = midpoint - previousMidpoint
                if (previousDistance > 0f && distance > 0f) {
                    camera.zoom((distance / previousDistance).coerceIn(0.5f, 2f))
                }
                if (midpointDelta.x != 0f || midpointDelta.y != 0f) {
                    rotateCamera(camera, mode, midpointDelta.x, midpointDelta.y)
                }
                previousDistance = distance
                previousMidpoint = midpoint
                previousFirst = first.position
                previousSecond = second.position
            } else if (first != null) {
                val delta = first.position - previousFirst
                if (delta.x != 0f || delta.y != 0f) {
                    rotateCamera(camera, mode, delta.x, delta.y)
                }
                previousFirst = first.position
                if (secondId != null) {
                    previousSecond = first.position
                    previousDistance = 0f
                    secondId = null
                }
            }

            event.changes.forEach { change ->
                if (change.pressed && change.id !in ignoredIds) change.consume()
            }
        }
    }
}

private fun rotateCamera(
    camera: PreviewCameraController,
    mode: PreviewCameraMode,
    deltaX: Float,
    deltaY: Float,
) {
    if (mode == PreviewCameraMode.Walk) {
        camera.rotateWalk(-deltaX * 0.004f, -deltaY * 0.004f)
    } else {
        camera.orbit(-deltaX * 0.006f, -deltaY * 0.006f)
    }
}

private data class PreviewGridLine(
    val start: Position,
    val end: Position,
)

private fun buildPreviewGridLines(
    startX: Float,
    startZ: Float,
    linesX: Int,
    linesZ: Int,
    y: Float,
): List<PreviewGridLine> {
    val endX = startX + linesX
    val endZ = startZ + linesZ
    return buildList(linesX + linesZ + 2) {
        for (index in 0..linesX) {
            val x = startX + index
            add(
                PreviewGridLine(
                    start = Position(x, y, startZ),
                    end = Position(x, y, endZ),
                ),
            )
        }
        for (index in 0..linesZ) {
            val z = startZ + index
            add(
                PreviewGridLine(
                    start = Position(startX, y, z),
                    end = Position(endX, y, z),
                ),
            )
        }
    }
}
