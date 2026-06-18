package io.github.moxisuki.blockprint.cat.ui.qr

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import io.github.moxisuki.blockprint.cat.data.bridge.QrConnection
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
    viewModel: QrScannerViewModel = hiltViewModel(),
    onResult: (QrConnection) -> Unit,
    onClose: () -> Unit,
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (cameraPermission.status !is PermissionStatus.Granted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        ) {
            Crossfade(
                targetState = cameraPermission.status,
                animationSpec = tween(180),
                label = "permissionState",
            ) { status ->
                when (val s = status) {
                    is PermissionStatus.Granted -> CameraContent(
                        viewModel = viewModel,
                        onResult = onResult,
                    )
                    else -> PermissionRationale(
                        status = s,
                        onRequest = { cameraPermission.launchPermissionRequest() },
                        onOpenSettings = { openAppSettings(context) },
                    )
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraContent(
    viewModel: QrScannerViewModel,
    onResult: (QrConnection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val events by viewModel.events.collectAsState(initial = null)
    val frameFlash by viewModel.frameFlash.collectAsState()

    LaunchedEffect(events) {
        when (val ev = events) {
            is QrScanEvent.Success -> {
                triggerShortVibration(context)
                onResult(ev.connection)
            }
            is QrScanEvent.InvalidPayload -> {
                triggerShortVibration(context)
                kotlinx.coroutines.delay(700)
                viewModel.resumeScanning()
                viewModel.clearFlash()
            }
            null -> Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose { executor.shutdown() }
    }

    val cameraAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(220),
        label = "cameraFadeIn",
    )
    val bottomAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "bottomFadeIn",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer { alpha = cameraAlpha },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = try { cameraProviderFuture.get() } catch (_: Exception) { return@addListener }
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(executor) { proxy: ImageProxy ->
                                viewModel.processImageProxy(proxy)
                            }
                        }
                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, selector, preview, analyzer)
                    } catch (_: Exception) {
                        // 设备无相机 / 相机被占用
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
        )

        ScanFrameOverlay(frameFlash = frameFlash)

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp)
                .graphicsLayer { alpha = bottomAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "将 PC 模组界面二维码对准扫描框",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ScanFrameOverlay(
    frameFlash: FrameFlash = FrameFlash.None,
) {
    val borderColor by animateColorAsState(
        targetValue = when (frameFlash) {
            FrameFlash.Success -> Color(0xFF4CAF50)
            FrameFlash.Error -> MaterialTheme.colorScheme.error
            FrameFlash.None -> Color(0xFF4CAF50)
        },
        animationSpec = tween(250),
        label = "frameBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (frameFlash != FrameFlash.None) 4.dp else 2.dp,
        animationSpec = tween(250),
        label = "frameBorderWidth",
    )

    val infinite = rememberInfiniteTransition(label = "scanLine")
    val scanLineY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanLineY",
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
        ) {
            // 扫描线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopStart)
                    .graphicsLayer { translationY = scanLineY * 240.dp.toPx() }
                    .background(Color(0xFF4CAF50).copy(alpha = 0.7f)),
            )
            CornerBrackets(color = borderColor, size = 20.dp, strokeWidth = borderWidth)
        }
    }
}

@Composable
private fun BoxScope.CornerBrackets(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val sPx = size.toPx()
                val sw = strokeWidth.toPx()
                // 左上
                drawLine(color, Offset(0f, 0f), Offset(sPx, 0f), sw)
                drawLine(color, Offset(0f, 0f), Offset(0f, sPx), sw)
                // 右上
                drawLine(color, Offset(this.size.width - sPx, 0f), Offset(this.size.width, 0f), sw)
                drawLine(color, Offset(this.size.width, 0f), Offset(this.size.width, sPx), sw)
                // 左下
                drawLine(color, Offset(0f, this.size.height - sPx), Offset(0f, this.size.height), sw)
                drawLine(color, Offset(0f, this.size.height), Offset(sPx, this.size.height), sw)
                // 右下
                drawLine(color, Offset(this.size.width - sPx, this.size.height), Offset(this.size.width, this.size.height), sw)
                drawLine(color, Offset(this.size.width, this.size.height - sPx), Offset(this.size.width, this.size.height), sw)
            }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRationale(
    status: PermissionStatus,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val shouldShowRationale = (status as? PermissionStatus.Denied)?.shouldShowRationale == true
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (shouldShowRationale) "扫码需要相机权限" else "相机权限被拒绝",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Cat 用相机扫描 PC 模组界面上的二维码以建立连接",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("授予相机权限") }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = { openAppSettings(context) }) { Text("前往系统设置") }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

private fun triggerShortVibration(context: Context) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    vibrator?.vibrate(
        VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
    )
}
