package io.github.moxisuki.blockprint.cat.app.feature.debug

import android.os.Build
import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.BuildConfig

@Immutable
data class DebugState(
    val appVersion: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE,
    val applicationId: String = BuildConfig.APPLICATION_ID,
    val blockPrintCoreVersion: String = BuildConfig.BLOCKPRINT_CORE_VERSION,
    val miuixVersion: String = BuildConfig.MIUIX_VERSION,
    val kotlinVersion: String = BuildConfig.KOTLIN_VERSION,
    val composeBomVersion: String = BuildConfig.COMPOSE_BOM_VERSION,
    val buildType: String = BuildConfig.BUILD_TYPE,
    val abi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: Build.CPU_ABI,
    val deviceModel: String = Build.MODEL,
    val deviceManufacturer: String = Build.MANUFACTURER,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val densityDpi: Int = 0,
    val totalRamMb: Int = 0,
    val heapLimitMb: Int = 0,
    val heapMaxMb: Int = 0,
    val appStorageMb: Int = 0,
    val isAppStorageLoading: Boolean = false,
)
