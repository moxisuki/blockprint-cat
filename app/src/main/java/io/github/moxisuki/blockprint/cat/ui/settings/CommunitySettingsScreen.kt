package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.github.moxisuki.blockprint.cat.data.McschematicCookieStore
import io.github.moxisuki.blockprint.cat.data.community.CmsCookieStore
import io.github.moxisuki.blockprint.cat.data.community.CommunityConfigManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CommunitySettingsEntryPoint {
    fun cookieStore(): McschematicCookieStore
    fun communityConfig(): CommunityConfigManager
    fun cmsCookieStore(): CmsCookieStore
}

@Composable
fun CommunitySettingsScreen() {
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, CommunitySettingsEntryPoint::class.java)
    }
    val mcsStore = entryPoint.cookieStore()
    val cmsStore = entryPoint.cmsCookieStore()
    val communityConfig = entryPoint.communityConfig()
    val enabled by communityConfig.enabled.collectAsState()

    var mcsCookies by remember { mutableStateOf(mcsStore.cookies()) }
    var mcsNickname by remember { mutableStateOf(mcsStore.nickname()) }
    var showManual by remember { mutableStateOf(false) }
    var manualUuid by remember { mutableStateOf("") }
    var manualUserAuth by remember { mutableStateOf("") }
    var manualCf by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { cmsStore.ensureLoaded() }
    val cmsCookies by produceState(initialValue = emptyList<Pair<String, String>>(), key1 = cmsStore) {
        cmsStore.ensureLoaded()
        value = cmsStore.cookiesSnapshot()
    }
    val mcsLoggedIn = mcsCookies.userAuth.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // ── 启用社区功能 开关 ──
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.community_config_enable_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.community_config_enable_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = { communityConfig.setEnabled(it) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── MCS 社区子分区 ──
        McsSection(
            loggedIn = mcsLoggedIn,
            nickname = mcsNickname,
            cookies = mcsCookies,
            showManual = showManual,
            manualUuid = manualUuid,
            manualUserAuth = manualUserAuth,
            manualCf = manualCf,
            onToggleManual = {
                if (!showManual) {
                    manualUuid = mcsCookies.uuid
                    manualUserAuth = mcsCookies.userAuth
                    manualCf = mcsCookies.cfClearance
                }
                showManual = !showManual
            },
            onManualChange = { field, value ->
                when (field) {
                    "uuid" -> manualUuid = value
                    "user_auth" -> manualUserAuth = value
                    "cf_clearance" -> manualCf = value
                }
            },
            onSaveManual = {
                mcsStore.save(manualUuid.trim(), manualUserAuth.trim(), manualCf.trim())
                mcsCookies = mcsStore.cookies()
                mcsNickname = mcsStore.nickname()
                showManual = false
            },
            onLogout = {
                mcsStore.clear()
                mcsCookies = mcsStore.cookies()
                mcsNickname = ""
            },
        )

        Spacer(Modifier.height(12.dp))

        // ── CMS 社区子分区 ──
        CmsSection(
            cookies = cmsCookies,
            onClear = {
                cmsStore.clear()
                cmsStore.saveToDisk()
            },
        )

        Spacer(Modifier.height(16.dp))

        // ── 底部「Cookie 获取方式」说明 ──
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.cs_how_to_get),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Bullet(stringResource(R.string.cs_how_to_get_1))
                Spacer(Modifier.height(6.dp))
                Bullet(stringResource(R.string.cs_how_to_get_2))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun McsSection(
    loggedIn: Boolean,
    nickname: String,
    cookies: McschematicCookieStore.Cookies,
    showManual: Boolean,
    manualUuid: String,
    manualUserAuth: String,
    manualCf: String,
    onToggleManual: () -> Unit,
    onManualChange: (field: String, value: String) -> Unit,
    onSaveManual: () -> Unit,
    onLogout: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.community_section_mcs),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.People, null,
                    tint = if (loggedIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (loggedIn) {
                        if (nickname.isNotEmpty()) stringResource(R.string.cs_signed_in_nick, nickname)
                        else stringResource(R.string.cs_signed_in)
                    } else stringResource(R.string.cs_not_signed_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (loggedIn) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                )
            }

            if (loggedIn) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                CookieRow("uuid", cookies.uuid)
                CookieRow("user_auth", cookies.userAuth)
                if (cookies.cfClearance.isNotEmpty()) {
                    CookieRow("cf_clearance", cookies.cfClearance)
                }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onLogout) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.cs_logout), color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.cs_manual_cookie),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onToggleManual) {
                    Icon(if (showManual) Icons.Default.Delete else Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (showManual) stringResource(R.string.cs_cancel_edit) else stringResource(R.string.action_detail))
                }
            }

            if (showManual) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualUuid, onValueChange = { onManualChange("uuid", it) },
                    label = { Text("uuid") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualUserAuth, onValueChange = { onManualChange("user_auth", it) },
                    label = { Text(stringResource(R.string.cs_user_auth_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = manualCf, onValueChange = { onManualChange("cf_clearance", it) },
                    label = { Text(stringResource(R.string.cs_cf_clearance_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = onSaveManual,
                    enabled = manualUserAuth.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cs_save_cookie))
                }
            }
        }
    }
}

@Composable
private fun CmsSection(
    cookies: List<Pair<String, String>>,
    onClear: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.community_section_cms),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (cookies.isEmpty()) stringResource(R.string.community_section_cms_status_none)
                       else stringResource(R.string.community_section_cms_status_count, cookies.size),
                style = MaterialTheme.typography.bodyMedium,
                color = if (cookies.isEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
            )

            if (cookies.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                cookies.forEach { (name, value) -> CookieRow(name, value) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClear) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.community_section_cms_clear), color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.community_section_cms_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CookieRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            if (value.length > 24) value.take(18) + "…" + value.takeLast(6) else value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}
