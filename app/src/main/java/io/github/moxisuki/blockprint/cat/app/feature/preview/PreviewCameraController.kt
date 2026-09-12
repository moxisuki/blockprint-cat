package io.github.moxisuki.blockprint.cat.app.feature.preview

import io.github.sceneview.math.Position
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class PreviewCameraMode {
    Orbit,
    Walk,
}

internal class PreviewCameraController {
    private var eyeX = 30f
    private var eyeY = 20f
    private var eyeZ = 36f
    private var targetX = 0f
    private var targetY = 3f
    private var targetZ = 0f
    private var homeEye = floatArrayOf(eyeX, eyeY, eyeZ)
    private var homeTarget = floatArrayOf(targetX, targetY, targetZ)
    private var walkYaw = atan2(eyeX - targetX, eyeZ - targetZ)
    private var walkPitch = atan2(eyeY - targetY, sqrt((eyeX - targetX) * (eyeX - targetX) + (eyeZ - targetZ) * (eyeZ - targetZ)))
    private var walkForward = 0f
    private var walkRight = 0f
    private var walkMode = false
    private var lastFrameNanos = 0L
    private var cameraDirty = true
    private var gridStartXValue = 0f
    private var gridStartZValue = 0f
    private var gridLinesXValue = 1
    private var gridLinesZValue = 1

    val anchorX: Float get() = targetX
    val anchorY: Float get() = targetY
    val anchorZ: Float get() = targetZ
    val gridStartX: Float get() = gridStartXValue
    val gridStartZ: Float get() = gridStartZValue
    val gridLinesX: Int get() = gridLinesXValue
    val gridLinesZ: Int get() = gridLinesZValue

    fun centerOn(node: ModelNode) {
        val bounds = node.boundingBox
        val center = bounds.center
        val extent = bounds.halfExtent
        val radius = sqrt(extent[0] * extent[0] + extent[1] * extent[1] + extent[2] * extent[2])
            .coerceAtLeast(2f)
        targetX = center[0]
        targetY = center[1]
        targetZ = center[2]
        eyeX = targetX + radius * 1.15f
        eyeY = targetY + radius * 0.72f
        eyeZ = targetZ + radius * 1.35f
        homeEye = floatArrayOf(eyeX, eyeY, eyeZ)
        homeTarget = floatArrayOf(targetX, targetY, targetZ)
        groundYValue = center[1] - extent[1]
        val minX = center[0] - extent[0]
        val minZ = center[2] - extent[2]
        val blockCountX = (extent[0] * 2f).roundToInt().coerceAtLeast(1)
        val blockCountZ = (extent[2] * 2f).roundToInt().coerceAtLeast(1)
        val desiredGridX = blockCountX + GRID_BORDER_BLOCKS * 2
        val desiredGridZ = blockCountZ + GRID_BORDER_BLOCKS * 2
        val visibleX = min(desiredGridX, MAX_GRID_BLOCKS)
        val visibleZ = min(desiredGridZ, MAX_GRID_BLOCKS)
        gridStartXValue = minX - GRID_BORDER_BLOCKS + ((desiredGridX - visibleX) / 2f).roundToInt()
        gridStartZValue = minZ - GRID_BORDER_BLOCKS + ((desiredGridZ - visibleZ) / 2f).roundToInt()
        gridLinesXValue = visibleX
        gridLinesZValue = visibleZ
        syncWalkOrientation()
        cameraDirty = true
    }

    private var groundYValue = 0f
    val gridY: Float get() = groundYValue

    fun reset(cameraNode: CameraNode) {
        eyeX = homeEye[0]
        eyeY = homeEye[1]
        eyeZ = homeEye[2]
        targetX = homeTarget[0]
        targetY = homeTarget[1]
        targetZ = homeTarget[2]
        walkForward = 0f
        walkRight = 0f
        syncWalkOrientation()
        apply(cameraNode)
    }

    fun setMode(mode: PreviewCameraMode) {
        val nextWalkMode = mode == PreviewCameraMode.Walk
        if (nextWalkMode && !walkMode) syncWalkOrientation()
        if (!nextWalkMode) {
            walkForward = 0f
            walkRight = 0f
        }
        if (walkMode != nextWalkMode) cameraDirty = true
        walkMode = nextWalkMode
    }

    fun setWalkInput(forward: Float, right: Float) {
        walkForward = forward.coerceIn(-1f, 1f)
        walkRight = right.coerceIn(-1f, 1f)
    }

    fun rotateWalk(horizontal: Float, vertical: Float) {
        walkYaw += horizontal
        walkPitch = (walkPitch + vertical).coerceIn(-1.45f, 1.45f)
        cameraDirty = true
    }

    fun orbit(horizontal: Float, vertical: Float) {
        val offsetX = eyeX - targetX
        val offsetY = eyeY - targetY
        val offsetZ = eyeZ - targetZ
        val distance = sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ)
            .coerceAtLeast(0.1f)
        val azimuth = atan2(offsetX, offsetZ) + horizontal
        val elevation = (asin((offsetY / distance).coerceIn(-1f, 1f)) + vertical)
            .coerceIn(-1.45f, 1.45f)
        eyeX = targetX + distance * cos(elevation) * sin(azimuth)
        eyeY = targetY + distance * sin(elevation)
        eyeZ = targetZ + distance * cos(elevation) * cos(azimuth)
        cameraDirty = true
    }

    fun onFrame(frameTimeNanos: Long, cameraNode: CameraNode) {
        val deltaSeconds = if (lastFrameNanos == 0L) {
            0f
        } else {
            ((frameTimeNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        }
        lastFrameNanos = frameTimeNanos
        if (walkMode && (walkForward != 0f || walkRight != 0f)) {
            val speed = (12f * deltaSeconds).coerceAtMost(1.2f)
            val forwardX = cos(walkPitch) * sin(walkYaw)
            val forwardY = sin(walkPitch)
            val forwardZ = cos(walkPitch) * cos(walkYaw)
            // The joystick's positive X is screen-right. Match the camera's
            // right vector so the scene moves in the same direction as the thumb.
            val rightX = -cos(walkYaw)
            val rightZ = sin(walkYaw)
            eyeX += (forwardX * walkForward + rightX * walkRight) * speed
            eyeY += forwardY * walkForward * speed
            eyeZ += (forwardZ * walkForward + rightZ * walkRight) * speed
            cameraDirty = true
        }
        if (cameraDirty) apply(cameraNode)
    }

    fun zoom(factor: Float) {
        val dx = eyeX - targetX
        val dy = eyeY - targetY
        val dz = eyeZ - targetZ
        val distance = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.5f)
        val next = (distance / factor.coerceIn(0.6f, 1.4f)).coerceIn(1.2f, 1000f)
        val ratio = next / distance
        eyeX = targetX + dx * ratio
        eyeY = targetY + dy * ratio
        eyeZ = targetZ + dz * ratio
        cameraDirty = true
    }

    fun apply(cameraNode: CameraNode) {
        if (walkMode) {
            cameraNode.lookAt(
                eye = Position(eyeX, eyeY, eyeZ),
                center = Position(
                    eyeX + cos(walkPitch) * sin(walkYaw) * 10f,
                    eyeY + sin(walkPitch) * 10f,
                    eyeZ + cos(walkPitch) * cos(walkYaw) * 10f,
                ),
                up = Position(0f, 1f, 0f),
            )
            cameraDirty = false
            return
        }
        cameraNode.lookAt(
            eye = Position(eyeX, eyeY, eyeZ),
            center = Position(targetX, targetY, targetZ),
            up = Position(0f, 1f, 0f),
        )
        cameraDirty = false
    }

    private fun syncWalkOrientation() {
        val dx = targetX - eyeX
        val dy = targetY - eyeY
        val dz = targetZ - eyeZ
        walkYaw = atan2(dx, dz)
        walkPitch = atan2(dy, sqrt(dx * dx + dz * dz))
    }

    private companion object {
        const val GRID_BORDER_BLOCKS = 1
        const val MAX_GRID_BLOCKS = 258
    }
}
