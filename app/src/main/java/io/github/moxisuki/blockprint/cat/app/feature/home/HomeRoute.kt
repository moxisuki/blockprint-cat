package io.github.moxisuki.blockprint.cat.app.feature.home

import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    onBlueprintClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.onAction(
                HomeAction.SafDirectorySelected(
                    treeUri = uri.toString(),
                    treeDocumentId = runCatching {
                        DocumentsContract.getTreeDocumentId(uri)
                    }.getOrNull(),
                ),
            )
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onAction(HomeAction.ImportFileSelected(uri.toString()))
        }
    }

    HomeScreen(
        state = state,
        onPickSafDirectory = {
            safLauncher.launch(null)
        },
        onImportBlueprint = {
            importLauncher.launch(
                arrayOf(
                    "application/octet-stream",
                    "application/json",
                    "text/*",
                    "*/*",
                ),
            )
        },
        onAction = { action ->
            viewModel.onAction(action)
        },
        onBlueprintClick = onBlueprintClick,
        modifier = modifier,
    )
}
