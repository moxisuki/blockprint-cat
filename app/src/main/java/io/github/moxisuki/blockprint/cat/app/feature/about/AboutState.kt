package io.github.moxisuki.blockprint.cat.app.feature.about

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.BuildConfig

@Immutable
data class AboutState(
    val appVersionName: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE,
    val applicationId: String = BuildConfig.APPLICATION_ID,
    val blockPrintCoreVersion: String = BuildConfig.BLOCKPRINT_CORE_VERSION,
    val changelog: String = BuildConfig.CHANGELOG,
    val hitokoto: AboutHitokotoState = AboutHitokotoState.Unavailable,
    val isChineseLocale: Boolean = true,
    val libraries: List<AboutLibrary> = defaultAboutLibraries(),
    val externalLinks: List<AboutExternalLink> = defaultExternalLinks(),
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
    val url: String,
)

enum class AboutLibraryLicense {
    Apache20,
    MIT,
    Repository,
    VendorSdk,
}

@Immutable
data class AboutExternalLink(
    val title: String,
    val url: String,
    val type: AboutExternalLinkType,
)

enum class AboutExternalLinkType { SITE, API }

private fun defaultAboutLibraries(): List<AboutLibrary> = listOf(
    AboutLibrary("BlockPrint Core", BuildConfig.BLOCKPRINT_CORE_VERSION, AboutLibraryLicense.MIT, "https://github.com/moxisuki/blockprint-core"),
    AboutLibrary("Miuix", BuildConfig.MIUIX_VERSION, AboutLibraryLicense.Apache20, "https://github.com/YuKongA/Miuix"),
    AboutLibrary("Jetpack Compose BOM", BuildConfig.COMPOSE_BOM_VERSION, AboutLibraryLicense.Apache20, "https://developer.android.com/jetpack/compose"),
    AboutLibrary("Kotlin", BuildConfig.KOTLIN_VERSION, AboutLibraryLicense.Apache20, "https://kotlinlang.org/"),
    AboutLibrary("Hilt", BuildConfig.HILT_VERSION, AboutLibraryLicense.Apache20, "https://dagger.dev/hilt/"),
    AboutLibrary("Coil", BuildConfig.COIL_VERSION, AboutLibraryLicense.Apache20, "https://coil-kt.github.io/coil/"),
    AboutLibrary("OkHttp", BuildConfig.OKHTTP_VERSION, AboutLibraryLicense.Apache20, "https://square.github.io/okhttp/"),
    AboutLibrary("SceneView", BuildConfig.SCENEVIEW_VERSION, AboutLibraryLicense.Apache20, "https://github.com/SceneView/sceneview-android"),
)

private fun defaultExternalLinks(): List<AboutExternalLink> = listOf(
    AboutExternalLink("MCS", "https://mcschematic.top", AboutExternalLinkType.SITE),
    AboutExternalLink("CMS", "https://www.creativemechanicserver.com", AboutExternalLinkType.SITE),
    AboutExternalLink("BMCL", "https://bmclapidoc.bangbang93.com/", AboutExternalLinkType.API),
    AboutExternalLink("Modrinth", "https://modrinth.com", AboutExternalLinkType.API),
    AboutExternalLink("Hitokoto", "https://hitokoto.cn/", AboutExternalLinkType.API),
)
