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
        versionCode = 3
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
    testImplementation(libs.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// =============================================================================
// generateChangelog — 把项目根 CHANGELOG.md 解析为结构化 Kotlin 数据
// 产物：app/build/generated/source/changelog/.../ChangelogContent.kt
// 触发：依赖 preBuild，每次构建自动运行；若 CHANGELOG.md 未变化则跳过
// =============================================================================

val changelogMdFile = rootProject.file("CHANGELOG.md")
val changelogOutDir = layout.buildDirectory.dir("generated/source/changelog")

val generateChangelog = tasks.register("generateChangelog") {
    inputs.file(changelogMdFile).withPropertyName("changelog")
    outputs.dir(changelogOutDir)
    // 解析器/发射器是脚本级私有函数，无法被配置缓存序列化；
    // 仅这一项任务禁用配置缓存，不影响整体构建缓存策略
    notCompatibleWithConfigurationCache(
        "Captures script-level parser/emitter which are not CC-serializable.",
    )
    doLast {
        val target = changelogOutDir.get().asFile
            .resolve("io/github/moxisuki/blockprint/cat/ui/settings")
        target.mkdirs()
        val text = changelogMdFile.readText(Charsets.UTF_8)
        val entries = parseChangelogMarkdown(text)
        target.resolve("ChangelogContent.kt")
            .writeText(emitChangelogKotlin(entries), Charsets.UTF_8)
    }
}

tasks.named("preBuild") { dependsOn(generateChangelog) }

// 把生成目录加入主源码集（Kotlin 编译会自动拾取 .kt 文件）
android.sourceSets["main"].kotlin.srcDir(changelogOutDir.get().asFile)

// -----------------------------------------------------------------------------
// 解析器与发射器（脚本级 — 仅在 build 阶段执行，不进 APK）
// -----------------------------------------------------------------------------

private data class PEntry(val title: String, val sections: List<PSection>)
private data class PSection(val title: String, val bullets: List<PBullet>)
private data class PBullet(val parts: List<PPart>)
private sealed interface PPart {
    data class Text(val text: String) : PPart
    data class Bold(val text: String) : PPart
}

private fun parseChangelogMarkdown(text: String): List<PEntry> {
    val lines = text.lines()
    val out = mutableListOf<PEntry>()
    var i = 0
    while (i < lines.size && !lines[i].startsWith("## ")) i++
    while (i < lines.size) {
        val title = lines[i].removePrefix("## ").trim()
        i++
        val block = mutableListOf<String>()
        while (i < lines.size && !lines[i].startsWith("## ")) {
            block.add(lines[i]); i++
        }
        out.add(PEntry(title, parseSections(block)))
    }
    return out
}

private fun parseSections(lines: List<String>): List<PSection> {
    val sections = mutableListOf<PSection>()
    val collected = mutableListOf<String>()
    var i = 0
    while (i < lines.size && !lines[i].startsWith("### ")) i++
    while (i < lines.size) {
        val title = lines[i].removePrefix("### ").trim()
        i++
        collected.clear()
        while (i < lines.size && !lines[i].startsWith("### ") && !lines[i].startsWith("## ")) {
            collected.add(lines[i]); i++
        }
        val bullets = parseBullets(collected)
        if (bullets.isNotEmpty()) sections.add(PSection(title, bullets))
    }
    if (sections.isEmpty()) {
        val fallback = lines.filter { it.isNotBlank() && !it.startsWith("#") }
            .map { PBullet(listOf(PPart.Text(it.trim()))) }
        if (fallback.isNotEmpty()) sections.add(PSection("变更", fallback))
    }
    return sections
}

private fun parseBullets(lines: List<String>): List<PBullet> {
    val out = mutableListOf<PBullet>()
    var current: StringBuilder? = null
    val topRe = Regex("^- (.+)$")
    val nestedRe = Regex("^\\s{2,}- (.+)$")
    for (raw in lines) {
        if (raw.isBlank()) { current = null; continue }
        val top = topRe.matchEntire(raw)
        val nested = nestedRe.matchEntire(raw)
        when {
            top != null -> {
                val t = top.groupValues[1]
                current = StringBuilder(t)
                out.add(PBullet(parseInline(t)))
            }
            nested != null && current != null -> {
                val t = " " + nested.groupValues[1]
                current.append(t)
                out[out.size - 1] = PBullet(parseInline(current.toString()))
            }
        }
    }
    return out
}

private fun parseInline(text: String): List<PPart> {
    val parts = mutableListOf<PPart>()
    val re = Regex("\\*\\*(.+?)\\*\\*")
    var last = 0
    for (m in re.findAll(text)) {
        if (m.range.first > last) parts.add(PPart.Text(text.substring(last, m.range.first)))
        parts.add(PPart.Bold(m.groupValues[1]))
        last = m.range.last + 1
    }
    if (last < text.length) parts.add(PPart.Text(text.substring(last)))
    return parts
}

private fun kotlinStringEscape(s: String): String {
    val sb = StringBuilder(s.length + 8)
    for (c in s) when (c) {
        '"' -> sb.append("\\\"")
        '\\' -> sb.append("\\\\")
        '$' -> sb.append("\\\$")
        '\n' -> sb.append("\\n")
        '\r' -> sb.append("\\r")
        '\t' -> sb.append("\\t")
        else -> sb.append(c)
    }
    return sb.toString()
}

private fun emitChangelogKotlin(entries: List<PEntry>): String = buildString {
    appendLine("// Generated by :app:generateChangelog from CHANGELOG.md — do not edit.")
    appendLine("package io.github.moxisuki.blockprint.cat.ui.settings")
    appendLine()
    appendLine("internal data class ChangelogEntry(")
    appendLine("    val title: String,")
    appendLine("    val sections: List<ChangelogSection>,")
    appendLine(")")
    appendLine()
    appendLine("internal data class ChangelogSection(")
    appendLine("    val title: String,")
    appendLine("    val bullets: List<ChangelogBullet>,")
    appendLine(")")
    appendLine()
    appendLine("internal data class ChangelogBullet(")
    appendLine("    val parts: List<ChangelogPart>,")
    appendLine(")")
    appendLine()
    appendLine("internal sealed interface ChangelogPart {")
    appendLine("    data class Text(val text: String) : ChangelogPart")
    appendLine("    data class Bold(val text: String) : ChangelogPart")
    appendLine("}")
    appendLine()
    appendLine("internal fun changelogEntries(): List<ChangelogEntry> = listOf(")
    for (entry in entries) {
        appendLine("    ChangelogEntry(")
        appendLine("        title = \"${kotlinStringEscape(entry.title)}\",")
        appendLine("        sections = listOf(")
        for (sec in entry.sections) {
            appendLine("            ChangelogSection(")
            appendLine("                title = \"${kotlinStringEscape(sec.title)}\",")
            appendLine("                bullets = listOf(")
            for (b in sec.bullets) {
                appendLine("                    ChangelogBullet(")
                appendLine("                        parts = listOf(")
                for (p in b.parts) {
                    when (p) {
                        is PPart.Text -> appendLine("                            ChangelogPart.Text(\"${kotlinStringEscape(p.text)}\"),")
                        is PPart.Bold -> appendLine("                            ChangelogPart.Bold(\"${kotlinStringEscape(p.text)}\"),")
                    }
                }
                appendLine("                        ),")
                appendLine("                    ),")
            }
            appendLine("                ),")
            appendLine("            ),")
        }
        appendLine("        ),")
        appendLine("    ),")
    }
    appendLine(")")
}
