package io.github.moxisuki.blockprint.cat.ui.preview

import io.github.sceneview.math.Position
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import kotlin.math.atan2
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val DRAG_SPEED = 0.15f
private const val ZOOM_STRENGTH = 5f
private const val WALK_MOVE_SPEED = 12f

internal class CameraController(
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
    /** 从当前 eye→target 向量重新计算 walkYaw/Pitch,避免切换 Walk 模式时视角突变 */
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

    // ── 由 rotation overlay 调用(公开的 delta 方法)──
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
