package io.github.moxisuki.blockprint.cat.app.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import io.github.moxisuki.blockprint.cat.app.core.persistence.McsAuthCookies
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsCommunitySection(
    communityEnabled: Boolean,
    mcsAuthCookies: McsAuthCookies,
    onCommunityEnabledChange: (Boolean) -> Unit,
    onMcsCookiesChange: (McsAuthCookies) -> Unit,
    onClearMcsCookiesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isCookieDialogVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        SwitchPreference(
            checked = communityEnabled,
            onCheckedChange = onCommunityEnabledChange,
            title = stringResource(R.string.community_config_enable_title),
            summary = stringResource(R.string.community_config_enable_subtitle),
        )
        ArrowPreference(
            modifier = Modifier.padding(top = 8.dp),
            title = stringResource(R.string.community_section_mcs),
            summary = mcsAuthCookies.toStatusText(),
            onClick = { isCookieDialogVisible = true },
        )
    }

    McsCookieDialog(
        show = isCookieDialogVisible,
        cookies = mcsAuthCookies,
        onDismissRequest = { isCookieDialogVisible = false },
        onSave = { cookies ->
            onMcsCookiesChange(cookies)
            isCookieDialogVisible = false
        },
        onClear = {
            onClearMcsCookiesClick()
            isCookieDialogVisible = false
        },
    )
}

@Composable
private fun McsCookieDialog(
    show: Boolean,
    cookies: McsAuthCookies,
    onDismissRequest: () -> Unit,
    onSave: (McsAuthCookies) -> Unit,
    onClear: () -> Unit,
) {
    var uuid by remember { mutableStateOf(cookies.uuid) }
    var userAuth by remember { mutableStateOf(cookies.userAuth) }
    var cfClearance by remember { mutableStateOf(cookies.cfClearance) }
    var nickname by remember { mutableStateOf(cookies.nickname) }

    LaunchedEffect(show, cookies) {
        if (show) {
            uuid = cookies.uuid
            userAuth = cookies.userAuth
            cfClearance = cookies.cfClearance
            nickname = cookies.nickname
        }
    }

    val saveCookies = {
        onSave(
            McsAuthCookies(
                uuid = uuid.trim(),
                userAuth = userAuth.trim(),
                cfClearance = cfClearance.trim(),
                nickname = nickname.trim(),
            ),
        )
    }

    OverlayBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.community_section_mcs),
        startAction = {
            TextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismissRequest,
            )
        },
        endAction = {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (cookies.isLoggedIn) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = MiuixIcons.Delete,
                            contentDescription = stringResource(R.string.action_logout),
                            tint = MiuixTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(
                    text = stringResource(R.string.action_save),
                    onClick = saveCookies,
                    enabled = userAuth.isNotBlank(),
                )
            }
        },
        insideMargin = androidx.compose.ui.unit.DpSize(16.dp, 18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextField(
                value = uuid,
                onValueChange = { uuid = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.cookie_uuid),
                singleLine = true,
                textStyle = MiuixTheme.textStyles.body2,
            )
            TextField(
                value = userAuth,
                onValueChange = { userAuth = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.cookie_user_auth_required),
                singleLine = true,
                textStyle = MiuixTheme.textStyles.body2,
            )
            TextField(
                value = cfClearance,
                onValueChange = { cfClearance = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.cookie_cf_clearance_optional),
                singleLine = true,
                textStyle = MiuixTheme.textStyles.body2,
            )
            TextField(
                value = nickname,
                onValueChange = { nickname = it },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.settings_community_nick),
                singleLine = true,
                textStyle = MiuixTheme.textStyles.body2,
            )
            Text(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                text = stringResource(R.string.cookie_manual_hint),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
            )
        }
    }
}

@Composable
private fun McsAuthCookies.toStatusText(): String {
    val status = when {
        !isLoggedIn -> stringResource(R.string.settings_community_not_logged_in)
        nickname.isNotBlank() -> stringResource(R.string.community_logged_in_with, nickname)
        else -> stringResource(R.string.settings_community_no_nick)
    }
    val cookieCount = listOf(uuid, userAuth, cfClearance).count { it.isNotBlank() }
    return if (cookieCount > 0) {
        "$status · ${stringResource(R.string.community_section_cms_status_count, cookieCount)}"
    } else {
        status
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsCommunitySectionPreview() {
    PreviewAppTheme {
        SettingsCommunitySection(
            communityEnabled = true,
            mcsAuthCookies = McsAuthCookies(
                uuid = "preview",
                userAuth = "token",
                cfClearance = "clearance",
                nickname = "moxisuki",
            ),
            onCommunityEnabledChange = {},
            onMcsCookiesChange = {},
            onClearMcsCookiesClick = {},
        )
    }
}
