package io.github.moxisuki.blockprint.cat.ui.preview

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.math.Direction
import io.github.sceneview.math.colorOf
import io.github.sceneview.node.LightNode

private const val LIGHTING_TAG = "PreviewLighting"

internal data class LightPreset(
    val label: String,
    val timeOfDay: Float,
    val sunIntensity: Float,
    val fillIntensity: Float,
    val envName: String = "neutral",  // 对应 assets/environments/<envName>/ 目录
    val celestialAzimuthDeg: Float,
    val celestialElevationDeg: Float,
)

internal val LIGHT_PRESETS = arrayOf(
    // 天体位置和 skybox 生成器保持一致:白天/黄昏=太阳,夜晚=月亮
    LightPreset("白天", timeOfDay = 12f, sunIntensity = 140_000f, fillIntensity = 10_000f, envName = "noon",   celestialAzimuthDeg = 135f, celestialElevationDeg = 55f),
    LightPreset("黄昏", timeOfDay = 18f, sunIntensity = 90_000f,  fillIntensity = 8_000f,  envName = "sunset", celestialAzimuthDeg = 270f, celestialElevationDeg = 2f),
    LightPreset("夜晚", timeOfDay = 0f,  sunIntensity = 5_000f,   fillIntensity = 1_200f,  envName = "night",  celestialAzimuthDeg = 110f, celestialElevationDeg = 45f),
    // 影棚模式仍保留中性方向,不追求 sky 天体一致性
    LightPreset("影棚", timeOfDay = 12f, sunIntensity = 30_000f,  fillIntensity = 60_000f, envName = "studio", celestialAzimuthDeg = 135f, celestialElevationDeg = 55f),
)

/**
 * 一次完成预设的完整光状态更新：
 * - fill light intensity
 * - main light direction / color / intensity
 *
 * 让预设切换的副作用集中到唯一调用点，避免分散更新导致中间帧状态不一致。
 */
internal fun applyPreviewLightPreset(
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
internal fun rememberNoDiscSunLight(engine: com.google.android.filament.Engine): LightNode {
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

internal fun loadCachedEnvironments(environmentLoader: EnvironmentLoader): Map<String, Environment> {
    fun loadEnv(name: String): Environment {
        return try {
            environmentLoader.createKTX1Environment(
                iblAssetFile = "environments/$name/${name}_ibl.ktx",
                skyboxAssetFile = "environments/$name/${name}_skybox.ktx",
            )
        } catch (e: Exception) {
            Log.w(LIGHTING_TAG, "env=$name KTX 缺失,使用 fallback neutral: ${e.message}")
            environmentLoader.createKTX1Environment(
                iblAssetFile = "environments/neutral/neutral_ibl.ktx",
                skyboxAssetFile = "environments/neutral/neutral_skybox.ktx",
            )
        }.also { env ->
            Log.i(LIGHTING_TAG, "env=$name loaded: skybox=${env.skybox != null}, ibl=${env.indirectLight != null}")
        }
    }

    return mapOf(
        "noon" to loadEnv("noon"),
        "sunset" to loadEnv("sunset"),
        "night" to loadEnv("night"),
        "studio" to loadEnv("studio"),
    )
}
