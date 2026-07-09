package io.github.moxisuki.blockprint.cat.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.settings.LanguageManager
import dagger.hilt.android.EntryPointAccessors

@Composable
fun LanguageSection() {
    val context = LocalContext.current
    val lm = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LanguageSectionEntryPoint::class.java
        ).languageManager()
    }
    val mode by lm.mode.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val subtitle = when (mode) {
        LanguageManager.Mode.SYSTEM -> stringResource(R.string.settings_language_subtitle_system)
        LanguageManager.Mode.ZH_CN -> stringResource(R.string.settings_language_subtitle_zh)
        LanguageManager.Mode.EN -> stringResource(R.string.settings_language_subtitle_en)
    }

    SettingsCard(
        icon = Icons.Default.Language,
        title = stringResource(R.string.settings_language_title),
        subtitle = subtitle,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.settings_language_title))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LanguageManager.Mode.values().forEach { m ->
                        val label = when (m) {
                            LanguageManager.Mode.SYSTEM -> stringResource(R.string.settings_language_subtitle_system)
                            LanguageManager.Mode.ZH_CN -> stringResource(R.string.settings_language_subtitle_zh)
                            LanguageManager.Mode.EN -> stringResource(R.string.settings_language_subtitle_en)
                        }
                        val selected = mode == m
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { lm.setMode(m); showDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = if (selected)
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            else
                                null,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                )
                                if (selected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface LanguageSectionEntryPoint {
    fun languageManager(): LanguageManager
}
