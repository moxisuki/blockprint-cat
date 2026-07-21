package io.github.moxisuki.blockprint.cat.app.feature.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DebugRoute(
    modifier: Modifier = Modifier,
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val onCopy: () -> Unit = remember(state) {
        {
            val text = buildDebugInfoText(state)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Debug Info", text))
        }
    }

    DebugScreen(
        state = state,
        onCopy = onCopy,
        modifier = modifier,
    )
}

private fun buildDebugInfoText(state: DebugState): String = buildString {
    appendLine("=== App Info ===")
    appendLine("Version: ${state.appVersion} (${state.appVersionCode})")
    appendLine("ABI: ${state.abi}")
    appendLine("Build type: ${state.buildType}")
    appendLine("Package: ${state.applicationId}")
    appendLine("BlockPrint Core: ${state.blockPrintCoreVersion}")
    appendLine("Miuix: ${state.miuixVersion}")
    appendLine("Kotlin: ${state.kotlinVersion}")
    appendLine("Compose BOM: ${state.composeBomVersion}")
    appendLine()
    appendLine("=== Device ===")
    appendLine("Model: ${state.deviceModel} (${state.deviceManufacturer})")
    appendLine("Android: ${state.androidVersion} (SDK ${state.sdkInt})")
    appendLine("Density: ${state.densityDpi} dpi")
    appendLine()
    appendLine("=== Memory ===")
    appendLine("Total RAM: ${state.totalRamMb} MB")
    appendLine("Heap limit: ${state.heapLimitMb} MB")
    appendLine("Large heap limit: ${state.heapMaxMb} MB")
    appendLine(
        if (state.isAppStorageLoading) {
            "App storage: calculating"
        } else {
            "App storage: ${state.appStorageMb} MB"
        },
    )
}
