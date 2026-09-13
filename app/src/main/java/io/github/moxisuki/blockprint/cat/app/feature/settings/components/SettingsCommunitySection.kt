package io.github.moxisuki.blockprint.cat.app.feature.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.design.PreviewAppTheme
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
internal fun SettingsCommunitySection(
    communityEnabled: Boolean,
    onCommunityEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwitchPreference(
        modifier = modifier,
        checked = communityEnabled,
        onCheckedChange = onCommunityEnabledChange,
        title = stringResource(R.string.community_config_enable_title),
        summary = stringResource(R.string.community_config_enable_subtitle),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsCommunitySectionPreview() {
    PreviewAppTheme {
        SettingsCommunitySection(
            communityEnabled = true,
            onCommunityEnabledChange = {},
        )
    }
}
