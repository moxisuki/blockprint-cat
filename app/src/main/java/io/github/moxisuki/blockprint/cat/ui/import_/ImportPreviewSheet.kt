package io.github.moxisuki.blockprint.cat.ui.import_

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.blueprint.BlueprintManager
import io.github.moxisuki.blockprint.cat.ui.format.BadgeColor
import io.github.moxisuki.blockprint.cat.ui.format.FormatCatalog
import io.github.moxisuki.blockprint.cat.ui.format.formatShortLabelRes
import io.github.moxisuki.blockprint.cat.ui.util.formatNumber
import io.github.moxisuki.blockprint.core.SchematicFormat
import io.github.moxisuki.blockprint.core.api.BlockPrintReader
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Bottom sheet shown when a blueprint file is queued for import from any
 * source. Designed to live over the underlying screen (no full takeover) —
 * the user can still see the previous state behind the scrim and dismiss
 * the sheet without context loss.
 *
 * Lifecycle (driven by caller-provided [uri]):
 *   - appears when caller passes a non-null [uri]
 *   - parses once via [ImportPreviewViewModel.parse]
 *   - renders Loading / Parsed / Failed
 *   - dismisses via [onDismissed] on Cancel / Confirm / Error back
 *
 * The Uri is read straight from the caller's argument (no String
 * roundtrips) so the implicit ACTION_VIEW grant from external file
 * providers survives all the way through to `ContentResolver.openInputStream`.
 *
 * Two upstream sources of Uri:
 *   - [MainActivity] writes it to the `pendingImportUri` flow for ACTION_VIEW
 *   - HomeScreen's SAF picker writes it through `onImportSafer(uri)` callback
 * Both surface through [LocalPendingImportUri] (see
 * `ui/navigation/LocalPendingImportUri.kt`); the AppNavGraph root reads
 * that and hands the Uri to this composable. After cancel/confirm/error,
 * [onDismissed] clears the upstream flow so the sheet hides.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewSheetContent(
    uri: Uri,
    onDismissed: () -> Unit,
    onConfirmSuccess: () -> Unit,
    viewModel: ImportPreviewViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uri) {
        viewModel.parse(context.contentResolver, uri)
    }

    val scopedOnDismiss: () -> Unit = {
        viewModel.dismiss()
        onDismissed()
    }

    ModalBottomSheet(
        onDismissRequest = scopedOnDismiss,
        sheetState = sheetState,
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
    ) {
        when (val s = state) {
            is PreviewState.Loading -> SheetLoading()
            is PreviewState.Parsed -> SheetParsed(
                parsed = s,
                onConfirm = {
                    viewModel.confirmImport {
                        scopedOnDismiss()
                        onConfirmSuccess()
                    }
                },
                onCancel = scopedOnDismiss,
            )
            is PreviewState.Failed -> SheetFailed(message = s.message, onBack = scopedOnDismiss)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ───────────────────────────── Sheet content ─────────────────────────────

@Composable
private fun SheetLoading() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.import_preview_loading),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SheetParsed(
    parsed: PreviewState.Parsed,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val importing by parsed.importing.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = parsed.displayName.ifBlank { parsed.fileName },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Same format chip styling as HomeBlueprintCard — keeps
                    // colors consistent across the Local list, community
                    // list, and the import sheet. Uses the locale-aware
                    // formatShortLabelRes() so this shows "Litematica",
                    // "WorldEdit", "NBT", "Building Helper" instead of
                    // the raw enum names.
                    FormatBadge(format = parsed.format)
                    Text(
                        text = formatFileSize(parsed.fileSizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }

        // Detail rows
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                DetailRow(
                    icon = Icons.Filled.Info,
                    label = stringResource(R.string.import_preview_filename),
                    value = parsed.fileName,
                )
            }
            item {
                DetailRow(
                    icon = Icons.Filled.Person,
                    label = stringResource(R.string.import_preview_author),
                    value = parsed.author.ifBlank { stringResource(R.string.import_preview_unknown) },
                )
            }
            item {
                DetailRow(
                    icon = Icons.Filled.ViewInAr,
                    label = stringResource(R.string.import_preview_regions),
                    value = "${parsed.regionCount}",
                )
            }
            item {
                DetailRow(
                    icon = Icons.Filled.ViewInAr,
                    label = stringResource(R.string.import_preview_block_count),
                    value = formatNumber(parsed.blockCount),
                )
            }
        }

        // Footnote
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = stringResource(R.string.import_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }

        // Action bar
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(onClick = onConfirm, enabled = !importing, modifier = Modifier.weight(1.5f)) {
                if (importing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.import_preview_confirm))
            }
        }
    }
}

@Composable
private fun SheetFailed(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.import_preview_failed_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text(stringResource(R.string.mgmt_cd_back))
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
}

/**
 * Locale-aware format badge mirroring `FormatChip` in HomeFilterChips.kt —
 * same tinted background per [BadgeColor] tier, same label resolution via
 * [formatShortLabelRes]. Lives here (not in `ui.home.components`) because
 * the import sheet is rendered above every tab and shouldn't depend on the
 * home-cards package; we'd otherwise pull HomeBlueprintCard transitively.
 */
@Composable
private fun FormatBadge(format: SchematicFormat) {
    val display = FormatCatalog.from(format)
    val bg = when (display.badgeColor) {
        BadgeColor.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        BadgeColor.Secondary -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
    }
    Box(
        Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            stringResource(formatShortLabelRes(format)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ───────────────────────────── ViewModel ─────────────────────────────

sealed interface PreviewState {
    data object Loading : PreviewState
    data class Parsed(
        val fileName: String,
        val displayName: String,
        val author: String,
        val regionCount: Int,
        val blockCount: Int,
        val format: SchematicFormat,
        val fileSizeBytes: Long,
        val importing: StateFlow<Boolean>,
    ) : PreviewState
    data class Failed(val message: String) : PreviewState
}

@HiltViewModel
class ImportPreviewViewModel @Inject constructor(
    private val blueprintManager: BlueprintManager,
) : ViewModel() {
    private val _state = MutableStateFlow<PreviewState>(PreviewState.Loading)
    val state: StateFlow<PreviewState> = _state.asStateFlow()

    private var pendingBytes: ByteArray? = null
    private var pendingFileName: String? = null
    private val importing = MutableStateFlow(false)

    fun parse(resolver: ContentResolver, uri: Uri) {
        // Idempotent on the URI: if the same Uri is parsed twice, skip the
        // I/O. We compare on toString() rather than Uri equality because
        // some providers mint throwaway Uri wrapper objects that compare
        // unequal even though they point to the same backing file.
        if (_state.value is PreviewState.Parsed &&
            (_state.value as PreviewState.Parsed).fileName ==
                (uri.lastPathSegment?.substringAfterLast('/') ?: "untitled")
        ) return
        _state.value = PreviewState.Loading
        viewModelScope.launch {
            val (fileName, bytes) = withContext(Dispatchers.IO) {
                // Resolve the actual file name. Uri.lastPathSegment is
                // not reliable for content:// URIs — providers like MT
                // Manager and Google Drive use the document ID as the
                // last segment, not the on-disk filename. Query
                // OpenableColumns.DISPLAY_NAME which most providers
                // supply; fall back to lastPathSegment for file:// URIs
                // (the SAF picker uses content:// so this rarely fires).
                val fileName = queryDisplayName(resolver, uri)
                    ?: uri.lastPathSegment?.substringAfterLast('/')
                    ?: "untitled"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: return@withContext null
                fileName to bytes
            } ?: run {
                _state.value = PreviewState.Failed("无法读取文件")
                return@launch
            }

            val size = bytes.size.toLong()
            pendingBytes = bytes
            pendingFileName = fileName

            val doc = runCatching { BlockPrintReader.readLenient(bytes) }.getOrNull()
            if (doc == null) {
                _state.value = PreviewState.Failed("解析失败：文件不是有效的 Minecraft 蓝图")
                return@launch
            }
            _state.value = PreviewState.Parsed(
                fileName = fileName,
                displayName = doc.name,
                author = doc.author,
                regionCount = doc.regions.size,
                blockCount = doc.blockCount(includeAir = false),
                format = doc.format,
                fileSizeBytes = size,
                importing = importing,
            )
        }
    }

    fun confirmImport(onDone: () -> Unit) {
        val bytes = pendingBytes ?: return
        val name = pendingFileName ?: return
        viewModelScope.launch {
            importing.value = true
            runCatching { blueprintManager.ingest(name, bytes) }
            importing.value = false
            onDone()
        }
    }

    /** Called when the sheet is dismissed; clears VM state for the next show. */
    fun dismiss() {
        pendingBytes = null
        pendingFileName = null
        _state.value = PreviewState.Loading
    }
}

/**
 * Resolve the user-visible file name for [uri] via
 * `OpenableColumns.DISPLAY_NAME`. Required because some file providers
 * (MT Manager, Solid Explorer, Google Drive) use opaque document IDs as
 * the last URI segment — `uri.lastPathSegment` would return those, not
 * the on-disk filename. Returns null if the provider doesn't expose a
 * display name (callers should fall back to `lastPathSegment`).
 */
private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    return runCatching {
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
    }.getOrNull()
}
