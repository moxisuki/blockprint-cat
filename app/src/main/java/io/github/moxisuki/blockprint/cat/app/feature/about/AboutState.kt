package io.github.moxisuki.blockprint.cat.app.feature.about

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.BuildConfig

@Immutable
data class AboutState(
    val isReady: Boolean = true,
    val appVersionName: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE,
    val applicationId: String = BuildConfig.APPLICATION_ID,
    val blockPrintCoreVersion: String = BuildConfig.BLOCKPRINT_CORE_VERSION,
    val miuixVersion: String = BuildConfig.MIUIX_VERSION,
    val hitokoto: AboutHitokotoState = AboutHitokotoState.Unavailable,
    val isChineseLocale: Boolean = true,
    val libraries: List<AboutLibrary> = defaultAboutLibraries(),
)

@Immutable
sealed interface AboutHitokotoState {
    data object Loading : AboutHitokotoState

    data object Unavailable : AboutHitokotoState

    @Immutable
    data class Content(
        val text: String,
        val source: String,
    ) : AboutHitokotoState
}

@Immutable
data class AboutLibrary(
    val name: String,
    val version: String,
    val license: AboutLibraryLicense,
)

enum class AboutLibraryLicense {
    Apache20,
    Repository,
    VendorSdk,
}

private fun defaultAboutLibraries(): List<AboutLibrary> = listOf(
    AboutLibrary("BlockPrint Core", BuildConfig.BLOCKPRINT_CORE_VERSION, AboutLibraryLicense.Repository),
    AboutLibrary("Miuix", BuildConfig.MIUIX_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("Jetpack Compose BOM", BuildConfig.COMPOSE_BOM_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("AndroidX Core KTX", BuildConfig.ANDROIDX_CORE_KTX_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("AndroidX AppCompat", BuildConfig.APP_COMPAT_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("AndroidX Lifecycle", BuildConfig.LIFECYCLE_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("AndroidX Navigation Compose", BuildConfig.NAVIGATION_COMPOSE_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("AndroidX Navigation3", BuildConfig.NAVIGATION3_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("AndroidX NavigationEvent", BuildConfig.NAVIGATION_EVENT_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("AndroidX Startup", BuildConfig.STARTUP_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("AndroidX ProfileInstaller", BuildConfig.PROFILE_INSTALLER_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("Kotlin", BuildConfig.KOTLIN_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("Hilt", BuildConfig.HILT_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("Hilt Navigation Compose", BuildConfig.HILT_NAVIGATION_COMPOSE_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("Room", BuildConfig.ROOM_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("Coil", BuildConfig.COIL_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("OkHttp", BuildConfig.OKHTTP_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("SceneView", BuildConfig.SCENEVIEW_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("CameraX", BuildConfig.CAMERAX_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("ZXing", BuildConfig.ZXING_VERSION, AboutLibraryLicense.Apache20),
    AboutLibrary("Tencent Bugly", BuildConfig.BUGLY_VERSION, AboutLibraryLicense.VendorSdk),
)
