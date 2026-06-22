package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.BuildConfig
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.NetworkConstants
import io.github.moxisuki.blockprint.cat.data.settings.AppIconManager
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.core.BLOCKPRINT_CORE_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, AboutScreenEntryPoint::class.java)
    }
    val appIconManager = entryPoint.appIconManager()
    val appIconCurrent by appIconManager.current.collectAsState()
    val appIconVariant = appIconManager.variants.firstOrNull { it.id == appIconCurrent } ?: appIconManager.variants.first()
    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }
    val appVersionCode = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }.getOrNull() ?: 1
    }
    var updateChecking by remember { mutableStateOf(false) }
    var updateDialog by remember { mutableStateOf<UpdateInfo?>(null) }
    var showLatestDialog by remember { mutableStateOf(false) }
    var showCrashTestDialog by remember { mutableStateOf(false) }
    // 彩蛋：点击版本号 5 次 → 长淡出 → 秘密页面
    var eggTaps by remember { mutableIntStateOf(0) }
    var eggPhase by remember { mutableStateOf(EggPhase.NORMAL) }

    // 内容淡出 / 淡入
    val contentAlpha = remember { Animatable(1f) }
    LaunchedEffect(eggPhase) {
        when (eggPhase) {
            EggPhase.SECRET -> {
                contentAlpha.animateTo(0f, tween(1200, easing = FastOutSlowInEasing))
                kotlinx.coroutines.delay(300)
            }
            EggPhase.NORMAL -> contentAlpha.snapTo(1f)
        }
    }
    // 一言（hitokoto）
    var hitokoto by remember { mutableStateOf<Hitokoto?>(null) }
    var hitokotoRefresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit, hitokotoRefresh) {
        // 点击刷新时先清空旧内容（触发淡出），再获取新内容（触发淡入）
        hitokoto = null
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder().url(NetworkConstants.HITOKOTO_API_URL).build()
                val json = client.newCall(request).execute().body?.string() ?: return@withContext
                val obj = org.json.JSONObject(json)
                hitokoto = Hitokoto(
                    text = obj.getString("hitokoto"),
                    from = obj.optString("from", ""),
                    fromWho = obj.optString("from_who", ""),
                )
            } catch (_: Exception) { /* 网络不可达时静默忽略 */ }
        }
    }
    val uriHandler = LocalUriHandler.current

    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .graphicsLayer { alpha = contentAlpha.value }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(appIconVariant.iconRes),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 一言 — 固定占位，内容进出用 crossfade
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hitokoto != null) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ) { hitokotoRefresh++ } else Modifier)
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(4.dp))
            androidx.compose.animation.AnimatedContent(
                targetState = hitokoto,
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                },
                label = "hitokoto",
            ) { h ->
                if (h != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "「${h.text}」",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        val realFromWho = h.fromWho.takeUnless { it == "null" || it.isBlank() }
                        val realFrom = h.from.takeUnless { it.isBlank() }
                        if (realFromWho != null || realFrom != null) {
                            Spacer(Modifier.height(4.dp))
                            val src = buildString {
                                if (realFromWho != null) append(realFromWho)
                                if (realFrom != null) {
                                    if (isNotEmpty()) append(" · ")
                                    append(realFrom)
                                }
                            }
                            Text(
                                "—— $src",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                } else {
                    // 空占位 — 保持高度，等待新内容淡入
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 项目信息
        val engineLabel = stringResource(R.string.about_engine_label)
        val engineVersion = stringResource(R.string.about_engine_version, BLOCKPRINT_CORE_VERSION)
        InfoCard(title = stringResource(R.string.about_section_info)) {
            InfoRow(label = stringResource(R.string.about_label_app_name), value = stringResource(R.string.app_name))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            InfoRow(
                label = stringResource(R.string.about_label_version),
                value = stringResource(R.string.about_version, appVersion),
                valueColor = if (eggPhase != EggPhase.NORMAL) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                onClick = {
                    eggTaps++
                    if (eggTaps >= 5 && eggPhase == EggPhase.NORMAL) {
                        eggPhase = EggPhase.SECRET
                        eggTaps = 0
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            InfoRow(label = engineLabel, value = engineVersion)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            InfoLinkRow(label = "GitHub", url = "https://github.com/moxisuki/blockprint-cat", displayOverride = "moxisuki/blockprint-cat")
        }

        Spacer(Modifier.height(12.dp))

        // 检查更新
        InfoCard(title = stringResource(R.string.about_section_update)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !updateChecking) {
                    updateChecking = true
                    MainScope().launch {
                        val info = withContext(Dispatchers.IO) { checkForUpdate(appVersionCode) }
                        updateChecking = false
                        if (info != null) updateDialog = info
                        else showLatestDialog = true
                    }
                }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (updateChecking) stringResource(R.string.about_checking_update)
                    else stringResource(R.string.about_check_update),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (updateChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Bugly 崩溃测试 — 仅在配置了 AppID 时显示
        if (BuildConfig.BUGLY_APP_ID.isNotEmpty()) {
            InfoCard(title = stringResource(R.string.about_section_bugly)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        // 二次确认
                        showCrashTestDialog = true
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        stringResource(R.string.about_test_crash),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 更新对话框
        updateDialog?.let { info ->
            AlertDialog(
                onDismissRequest = { updateDialog = null },
                title = { Text(stringResource(R.string.about_update_title)) },
                text = {
                    Column {
                        Text(
                            stringResource(R.string.about_update_version, info.latestVersion),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            info.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { uriHandler.openUri(info.downloadUrl) }) {
                        Text(stringResource(R.string.about_update_download))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { updateDialog = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        // 已是最新版本对话框
        if (showLatestDialog) {
            AlertDialog(
                onDismissRequest = { showLatestDialog = false },
                title = { Text(stringResource(R.string.about_update_latest_title)) },
                text = { Text(stringResource(R.string.about_update_latest_msg, appVersion)) },
                confirmButton = {
                    TextButton(onClick = { showLatestDialog = false }) {
                        Text(stringResource(R.string.action_confirm))
                    }
                },
            )
        }

        // Bugly 崩溃测试确认对话框
        if (showCrashTestDialog) {
            AlertDialog(
                onDismissRequest = { showCrashTestDialog = false },
                title = { Text(stringResource(R.string.about_test_crash_title)) },
                text = { Text(stringResource(R.string.about_test_crash_msg)) },
                confirmButton = {
                    TextButton(onClick = {
                        showCrashTestDialog = false
                        // 主动抛出一个未捕获异常 — Bugly 会捕获并上报
                        throw RuntimeException("Manual crash test from AboutScreen")
                    }) {
                        Text(stringResource(R.string.about_test_crash_confirm), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCrashTestDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // 功能
        InfoCard(title = stringResource(R.string.about_section_features)) {
            FeatureItem(Icons.Filled.Description, stringResource(R.string.about_feature_local_title), stringResource(R.string.about_feature_local_desc))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            FeatureItem(Icons.Filled.Code, stringResource(R.string.about_feature_preview_title), stringResource(R.string.about_feature_preview_desc))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            FeatureItem(
                Icons.Filled.Code,
                stringResource(R.string.about_feature_pc_title),
                stringResource(R.string.about_feature_pc_desc),
            )
        }

        Spacer(Modifier.height(12.dp))

        // 贡献者
        InfoCard(title = stringResource(R.string.about_section_contributors)) {
            ContributorItem(
                name = "moxisuki",
                role = stringResource(R.string.about_contributor_role_developer),
                url = "https://github.com/moxisuki",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ContributorItem(
                name = "酸不过橘子皮",
                role = stringResource(R.string.about_contributor_role_core_tester),
                url = null,
                avatarRes = R.drawable.contributor_orange_peel,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ContributorItem(
                name = "Fan Oblivion",
                role = stringResource(R.string.about_contributor_role_core_tester),
                url = null,
            )
        }

        Spacer(Modifier.height(12.dp))

        // 外部链接
        InfoCard(title = stringResource(R.string.about_section_links)) {
            ExternalLinkItem(
                icon = Icons.Filled.Public,
                title = "MCS 蓝图站",
                url = "https://mcschematic.top",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ExternalLinkItem(
                icon = Icons.Filled.Public,
                title = "CMS 蓝图站",
                url = "https://www.creativemechanicserver.com",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ExternalLinkItem(
                icon = Icons.Filled.CloudDownload,
                title = "BMCLAPI 镜像源",
                url = "https://bmclapidoc.bangbang93.com/",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ExternalLinkItem(
                icon = Icons.Filled.Code,
                title = "Modrinth 资源站",
                url = "https://modrinth.com",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ExternalLinkItem(
                icon = Icons.Filled.Code,
                title = "BlockPrint Core",
                url = "https://github.com/moxisuki/blockprint-core",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ExternalLinkItem(
                icon = Icons.Filled.Public,
                title = "一言 API",
                url = "https://hitokoto.cn/",
            )
        }

        Spacer(Modifier.height(12.dp))

        // 开源信息
        InfoCard(title = stringResource(R.string.about_section_opensource)) {
            OpenSourceItem(
                name = "AndroidX Core KTX",
                version = BuildConfig.ANDROIDX_CORE_KTX_VERSION,
                license = "Apache-2.0",
                url = "https://github.com/androidx/androidx",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Jetpack Compose",
                version = "BOM " + BuildConfig.COMPOSE_BOM_VERSION,
                license = "Apache-2.0",
                url = "https://developer.android.com/jetpack/compose",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Navigation Compose",
                version = BuildConfig.NAVIGATION_COMPOSE_VERSION,
                license = "Apache-2.0",
                url = "https://developer.android.com/jetpack/compose/navigation",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Lifecycle (ViewModel / Runtime)",
                version = BuildConfig.LIFECYCLE_VERSION,
                license = "Apache-2.0",
                url = "https://developer.android.com/jetpack/androidx/releases/lifecycle",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Material Icons Extended",
                version = "via Compose BOM",
                license = "Apache-2.0",
                url = "https://developer.android.com/jetpack/compose/resources/icons",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Coil",
                version = BuildConfig.COIL_VERSION,
                license = "Apache-2.0",
                url = "https://coil-kt.github.io/coil/",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "CameraX",
                version = BuildConfig.CAMERAX_VERSION,
                license = "Apache-2.0",
                url = "https://developer.android.com/jetpack/androidx/releases/camera",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "OkHttp",
                version = BuildConfig.OKHTTP_VERSION,
                license = "Apache-2.0",
                url = "https://square.github.io/okhttp/",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Room",
                version = BuildConfig.ROOM_VERSION,
                license = "Apache-2.0",
                url = "https://developer.android.com/jetpack/androidx/releases/room",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Hilt",
                version = BuildConfig.HILT_VERSION,
                license = "Apache-2.0",
                url = "https://dagger.dev/hilt/",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Hilt Navigation Compose",
                version = BuildConfig.HILT_NAVIGATION_COMPOSE_VERSION,
                license = "Apache-2.0",
                url = "https://dagger.dev/hilt/jetpack-integration",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "SceneView",
                version = BuildConfig.SCENEVIEW_VERSION,
                license = "Apache-2.0",
                url = "https://github.com/SceneView/sceneview-android",
            )
        }

        Spacer(Modifier.height(12.dp))

        // 链接
        InfoCard(title = stringResource(R.string.about_section_more)) {
            LinkItem(text = stringResource(R.string.about_link_terms), onClick = { navController.navigate(NavRoutes.TERMS) })
        }

        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.about_footer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            textAlign = TextAlign.Center,
        )
    }
    } // end scrollable Column

    // 彩蛋 — 秘密页面（长淡入）
    if (eggPhase == EggPhase.SECRET) {
        val secretAlpha = remember { Animatable(0f) }
        LaunchedEffect(Unit) { secretAlpha.animateTo(1f, tween(800, delayMillis = 300)) }
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = secretAlpha.value }) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(48.dp))
                Text(
                    "🐱",
                    style = MaterialTheme.typography.displayLarge,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "/_/_\\\\\n( o.o )\n > ^ <",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "你找到了一只秘密小猫",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "它守护着 BlockPrint Cat 的源代码\n—— 喵 ~",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                OutlinedButton(onClick = {
                    eggPhase = EggPhase.NORMAL
                    eggTaps = 0
                }) {
                    Text("返回关于页面")
                }
                Spacer(Modifier.height(48.dp))
            }
        }
        } // end secret alpha Box
    }
} // end Box

private data class UpdateInfo(
    val latestVersion: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val changelog: String,
)

private fun checkForUpdate(currentVersionCode: Int): UpdateInfo? {
    return try {
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url("${NetworkConstants.CDN_BASE_URL}/version.json")
            .build()
        val json = client.newCall(request).execute().use { it.body?.string() ?: return null }
        val obj = org.json.JSONObject(json)
        val remoteVersionCode = obj.optInt("latest_version_code", 0)
        if (remoteVersionCode <= currentVersionCode) return null
        val changelog = obj.optString("changelog_zh", "").ifEmpty { obj.optString("changelog_en", "") }
        UpdateInfo(
            latestVersion = obj.optString("latest_version", "?"),
            latestVersionCode = remoteVersionCode,
            downloadUrl = obj.optString("download_url", ""),
            changelog = changelog,
        )
    } catch (e: Exception) {
        android.util.Log.w("AboutScreen", "checkForUpdate failed", e)
        null
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            ),
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color? = null, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun InfoLinkRow(label: String, url: String, displayOverride: String? = null) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                displayOverride ?: url.removePrefix("https://").removePrefix("http://"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ContributorItem(
    name: String,
    role: String,
    url: String? = null,
    avatarRes: Int? = null,
) {
    val uriHandler = LocalUriHandler.current
    val hasLink = !url.isNullOrEmpty()
    val clickModifier = if (hasLink) {
        Modifier.clickable { uriHandler.openUri(url!!) }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatarRes != null) {
            Image(
                painter = painterResource(avatarRes),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                role,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasLink) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun LinkItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OpenSourceItem(
    name: String,
    version: String,
    license: String,
    url: String,
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                "v$version · $license",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun ExternalLinkItem(
    icon: ImageVector,
    title: String,
    url: String,
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                url.removePrefix("https://").removePrefix("http://"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

private enum class EggPhase { NORMAL, SECRET }

private data class Hitokoto(val text: String, val from: String, val fromWho: String)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AboutScreenEntryPoint {
    fun appIconManager(): AppIconManager
}
