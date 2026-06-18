package io.github.moxisuki.blockprint.cat.ui.bridge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.bridge.SessionInfo

@Composable
fun ConnectedBody(
    state: ConnectionState.Connected,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SessionInfoBlock(state.session)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onDisconnect,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 32.dp),
        ) {
            Text(stringResource(R.string.bridge_action_disconnect), style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun SessionInfoBlock(session: SessionInfo) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Computer,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(8.dp))
        Column {
            Text(session.folderName, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "MC ${session.mcVersion} · ${session.loader} ${session.loaderVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = LocalContentColor.current.copy(alpha = 0.72f),
            )
        }
    }
}
