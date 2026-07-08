@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.moxisuki.blockprint.cat.ui.tools.blueprintcommon

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.moxisuki.blockprint.cat.R
import kotlinx.coroutines.launch

/**
 * 共享导出底部弹窗：PNG 保存 + BlueprintPreviewContent。
 * BlockPaint 和 TTB 共用。
 */
@Composable
fun ExportBottomSheet(
    exportPayload: String?,
    resultBitmap: Bitmap?,
    defaultSaveName: String,
    sheetState: SheetState,
    context: Context,
    onClose: () -> Unit,
    onViewBlueprint: (String) -> Unit,
) {
    if (exportPayload == null) return
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) onClose()
            }
        },
        sheetState = sheetState,
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            // PNG 导出
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.IosShare, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text("PNG", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    resultBitmap?.let { bmp ->
                        val f = java.io.File(context.cacheDir, "export_${System.currentTimeMillis()}.png")
                        java.io.FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, null))
                    }
                }) {
                    Text(stringResource(R.string.bp_save_png), color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // MC 命令 / 蓝图导出
            io.github.moxisuki.blockprint.cat.ui.tools.blueprintpreview.BlueprintPreviewContent(
                encodedResult = exportPayload,
                onViewBlueprint = { uuid ->
                    onViewBlueprint(uuid)
                },
                onDismiss = onClose,
                defaultSaveName = defaultSaveName,
            )
        }
    }
}
