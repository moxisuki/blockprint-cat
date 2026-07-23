package io.github.moxisuki.blockprint.cat.app.feature.resourcepacks.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ActiveInstall
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.PackProgress
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton

@Composable
internal fun ResourcePackProgressBanner(
    install: ActiveInstall,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressState by install.progress.collectAsStateWithLifecycle(initialValue = PackProgress.Idle)
    val progress = progressState
    Card(modifier = modifier.fillMaxWidth(), insideMargin = PaddingValues(16.dp)) {
        Column {
            Text(install.displayName)
            val summary = when (progress) {
                is PackProgress.Downloading -> stringResource(
                    R.string.resourcepacks_progress_installing,
                    progress.fileName,
                    (progress.fraction * 100).toInt(),
                )
                is PackProgress.Extracting -> stringResource(R.string.resourcepacks_progress_extracting)
                is PackProgress.FetchingManifest -> progress.label
                is PackProgress.Failed -> stringResource(R.string.resourcepacks_failed, progress.message)
                else -> ""
            }
            Text(summary)
            if (progress is PackProgress.Downloading) {
                LinearProgressIndicator(progress = progress.fraction, modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = onCancel, text = stringResource(android.R.string.cancel))
        }
    }
}
