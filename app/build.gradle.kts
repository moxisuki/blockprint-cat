import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android.plugin)
}

// 读取 local.properties 中的 Bugly AppID（不入 git，避免开源泄露）
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

// Compose Compiler Reports — 开启稳定性推断 + 重组次数统计
// 输出到 app/build/compose_reports/ + app/build/compose_metrics/
// 加 `-Pplugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=...`
// 触发后跑 ./gradlew :app:assembleDebug 即可生成 app_release-classes.txt 等
// 用来定位 unstable class / restartable-but-not-skippable Composable
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                layout.buildDirectory.dir("compose_reports").get().asFile.absolutePath,
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                layout.buildDirectory.dir("compose_metrics").get().asFile.absolutePath,
        )
    }
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
        versionCode = 6
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AboutScreen 开源信息版本号 — 自动从 libs.versions.toml 读取
        buildConfigField("String", "ANDROIDX_CORE_KTX_VERSION", "\"${libs.versions.coreKtx.get()}\"")
        buildConfigField("String", "KOTLIN_VERSION", "\"${libs.versions.kotlin.get()}\"")
        buildConfigField("String", "COMPOSE_BOM_VERSION", "\"${libs.versions.composeBom.get()}\"")
        buildConfigField("String", "NAVIGATION_COMPOSE_VERSION", "\"${libs.versions.navigationCompose.get()}\"")
        buildConfigField("String", "NAVIGATION3_VERSION", "\"${libs.versions.navigation3.get()}\"")
        buildConfigField("String", "NAVIGATION_EVENT_VERSION", "\"${libs.versions.navigationEvent.get()}\"")
        buildConfigField("String", "LIFECYCLE_VERSION", "\"${libs.versions.lifecycleRuntimeKtx.get()}\"")
        buildConfigField("String", "APP_COMPAT_VERSION", "\"${libs.versions.appcompat.get()}\"")
        buildConfigField("String", "COIL_VERSION", "\"${libs.versions.coil.get()}\"")
        buildConfigField("String", "CAMERAX_VERSION", "\"${libs.versions.camerax.get()}\"")
        buildConfigField("String", "OKHTTP_VERSION", "\"${libs.versions.okhttp.get()}\"")
        buildConfigField("String", "ROOM_VERSION", "\"${libs.versions.room.get()}\"")
        buildConfigField("String", "HILT_VERSION", "\"${libs.versions.hilt.get()}\"")
        buildConfigField("String", "HILT_NAVIGATION_COMPOSE_VERSION", "\"${libs.versions.hiltNavigationCompose.get()}\"")
        buildConfigField("String", "SCENEVIEW_VERSION", "\"${libs.versions.sceneview.get()}\"")
        buildConfigField("String", "BLOCKPRINT_CORE_VERSION", "\"${libs.versions.blockprint.get()}\"")
        buildConfigField("String", "MIUIX_VERSION", "\"${libs.versions.miuix.get()}\"")
        buildConfigField("String", "STARTUP_VERSION", "\"${libs.versions.startup.get()}\"")
        buildConfigField("String", "PROFILE_INSTALLER_VERSION", "\"${libs.versions.profileInstaller.get()}\"")
        buildConfigField("String", "BUGLY_VERSION", "\"${libs.versions.bugly.get()}\"")
        buildConfigField("String", "ZXING_VERSION", "\"${libs.versions.zxing.get()}\"")
        // Bugly 崩溃上报 — 从 local.properties 读取（不入 git，避免开源泄露）
        val buglyAppId = localProps.getProperty("BUGLY_APP_ID", "")
        buildConfigField("String", "BUGLY_APP_ID", "\"$buglyAppId\"")
        manifestPlaceholders["BUGLY_APP_ID"] = buglyAppId
    }

    signingConfigs {
        create("release") {
            storeFile = localProps.getProperty("RELEASE_STORE_FILE")?.let { file(it) }
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            // APK 直分发签名：v1(JAR) + v2 + v3 全开
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // GitHub 直分发：每个 ABI 各出一个独立 APK，用户按机型下载对应包。
    // x86/x86_64 仅模拟器需要；如需一并产出可加入 include。
    // isUniversalApk = true 时额外产出一个含全部 ABI 的通用包。
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigationevent)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.miuix.ui.android)
    implementation(libs.miuix.preference.android)
    implementation(libs.miuix.icons.android)
    implementation(libs.miuix.navigation3.ui.android)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.profileinstaller)
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
    testImplementation(libs.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
