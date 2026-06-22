import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android.plugin)
}

// 读取 local.properties 中的 Bugly AppID（不入 git，避免开源泄露）
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "io.github.moxisuki.blockprint.cat"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.moxisuki.blockprint.cat"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        // AboutScreen 开源信息版本号 — 自动从 libs.versions.toml 读取
        buildConfigField("String", "ANDROIDX_CORE_KTX_VERSION", "\"${libs.versions.coreKtx.get()}\"")
        buildConfigField("String", "COMPOSE_BOM_VERSION", "\"${libs.versions.composeBom.get()}\"")
        buildConfigField("String", "NAVIGATION_COMPOSE_VERSION", "\"${libs.versions.navigationCompose.get()}\"")
        buildConfigField("String", "LIFECYCLE_VERSION", "\"${libs.versions.lifecycleRuntimeKtx.get()}\"")
        buildConfigField("String", "COIL_VERSION", "\"${libs.versions.coil.get()}\"")
        buildConfigField("String", "CAMERAX_VERSION", "\"${libs.versions.camerax.get()}\"")
        buildConfigField("String", "OKHTTP_VERSION", "\"${libs.versions.okhttp.get()}\"")
        buildConfigField("String", "ROOM_VERSION", "\"${libs.versions.room.get()}\"")
        buildConfigField("String", "HILT_VERSION", "\"${libs.versions.hilt.get()}\"")
        buildConfigField("String", "HILT_NAVIGATION_COMPOSE_VERSION", "\"${libs.versions.hiltNavigationCompose.get()}\"")
        buildConfigField("String", "SCENEVIEW_VERSION", "\"${libs.versions.sceneview.get()}\"")
        // Bugly 崩溃上报 — 从 local.properties 读取（不入 git，避免开源泄露）
        val buglyAppId = localProps.getProperty("BUGLY_APP_ID", "")
        buildConfigField("String", "BUGLY_APP_ID", "\"$buglyAppId\"")
        manifestPlaceholders["BUGLY_APP_ID"] = buglyAppId
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.blockprint.core)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.sceneview)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.room.compiler)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.bugly.crashreport)
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
