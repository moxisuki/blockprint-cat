package io.github.moxisuki.blockprint.cat.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R

/**
 * Minimal loading overlay shown while SceneView initialises and the GLB
 * is being parsed by Filament.
 *
 * Visual style: a single centred spinner over a translucent black scrim.
 * No corner brackets, no status chrome — the user just needs to see
 * "something is loading" while the engine spins up on the main thread.
 */
@Composable
internal fun HudStartupOverlay(visible: Boolean) {
    if (!visible) return
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                color = primary,
                modifier = Modifier.size(40.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.preview_hud_status),
                style = MaterialTheme.typography.bodyMedium,
                color = primary,
            )
        }
    }
}
