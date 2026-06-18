package io.github.moxisuki.blockprint.cat.ui.settings

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
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.NetworkConstants
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import io.github.moxisuki.blockprint.core.BLOCKPRINT_CORE_VERSION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
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
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.mipmap.ic_launcher_v4),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.about_version, appVersion), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))

        // 项目信息
        val engineLabel = stringResource(R.string.about_engine_label)
        val engineVersion = stringResource(R.string.about_engine_version, BLOCKPRINT_CORE_VERSION)
        InfoCard(title = stringResource(R.string.about_section_info)) {
            InfoRow(label = stringResource(R.string.about_label_app_name), value = stringResource(R.string.app_name))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            InfoRow(label = stringResource(R.string.about_label_version), value = stringResource(R.string.about_version, appVersion))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            InfoRow(label = engineLabel, value = engineVersion)
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
                title = "GitHub 仓库",
                url = "https://github.com/",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ExternalLinkItem(
                icon = Icons.Filled.Code,
                title = "BlockPrint Core",
                url = "https://github.com/moxisuki/blockprint-core",
            )
        }

        Spacer(Modifier.height(12.dp))

        // 开源信息
        InfoCard(title = stringResource(R.string.about_section_opensource)) {
            OpenSourceItem(
                name = "AndroidX Core KTX",
                version = "1.19.0",
                license = "Apache-2.0",
                url = "https://github.com/androidx/androidx",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Jetpack Compose",
                version = "BOM 2026.02.01",
                license = "Apache-2.0",
                url = "https://developer.android.com/jetpack/compose",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Navigation Compose",
                version = "2.8.5",
                license = "Apache-2.0",
                url = "https://developer.android.com/jetpack/compose/navigation",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Lifecycle (ViewModel / Runtime)",
                version = "2.10.0",
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
                version = "2.7.0",
                license = "Apache-2.0",
                url = "https://coil-kt.github.io/coil/",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "OkHttp",
                version = "4.12.0",
                license = "Apache-2.0",
                url = "https://square.github.io/okhttp/",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Room",
                version = "2.7.1",
                license = "Apache-2.0",
                url = "https://developer.android.com/jetpack/androidx/releases/room",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Hilt",
                version = "2.57.1",
                license = "Apache-2.0",
                url = "https://dagger.dev/hilt/",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "Hilt Navigation Compose",
                version = "1.2.0",
                license = "Apache-2.0",
                url = "https://dagger.dev/hilt/jetpack-integration",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            OpenSourceItem(
                name = "SceneView",
                version = "4.18.0",
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
}

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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
