package io.github.moxisuki.blockprint.cat.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.FullBlueprint
import io.github.moxisuki.blockprint.cat.data.vanilla.AssetNamespaceResolver
import io.github.moxisuki.blockprint.cat.data.vanilla.ModAssetManager
import io.github.moxisuki.blockprint.cat.ui.navigation.NavRoutes
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@Composable
fun NamespaceCard(bp: FullBlueprint, onNavigate: (String) -> Unit) {
    val ctx = LocalContext.current
    val renderEntry = EntryPointAccessors.fromApplication(
        ctx.applicationContext as android.app.Application, RenderPreviewEntryPoint::class.java
    )
    val hasVanilla = renderEntry.vanillaAssetDownloader().isAssetsAvailable()
    val modManager = renderEntry.modAssetManager()
    val namespaces = AssetNamespaceResolver.resolve(bp.raw).sorted()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.detail_resource_status), style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(10.dp))
            namespaces.forEachIndexed { index, ns ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(6.dp))
                }
                val modState = modManager.stateFor(ns)
                val available = when (ns) {
                    "minecraft" -> hasVanilla
                    else -> modState is ModAssetManager.ModState.Installed && modState.entity.fileCount > 0
                }
                val emptyMod = modState is ModAssetManager.ModState.Installed && modState.entity.fileCount == 0
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (emptyMod) Icons.Default.Check else if (available) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = if (available) "已安装" else "未安装",
                        modifier = Modifier.size(16.dp),
                        tint = when { emptyMod -> MaterialTheme.colorScheme.outline; available -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.error },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        ns,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (emptyMod) {
                        Text(stringResource(R.string.ns_no_resources), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    } else if (available) {
                        Text(stringResource(R.string.ns_installed), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(
                            "去安装",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onNavigate(if (ns == "minecraft") "" else ns) },
                        )
                    }
                }
            }
        }
    }
}
