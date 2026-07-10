package io.github.moxisuki.blockprint.cat.ui.navigation

import android.net.Uri
import androidx.compose.runtime.compositionLocalOf

/**
 * Compose CompositionLocal that carries the latest blueprint file Uri
 * handed to us via ACTION_VIEW or the Home tab's SAF picker. The default
 * value is `null` — `null` means "no pending import" and the
 * `ImportPreviewSheetContent` at the AppNavGraph root hides.
 *
 * The Uri is propagated as the **original** object (no String encoding /
 * Uri roundtrip) so the implicit ACTION_VIEW grant remains valid when
 * the sheet asks ContentResolver to open it.
 *
 * Producers:
 *   - `MainActivity.captureIncomingBlueprintUri()` writes ACTION_VIEW Uris
 *   - `HomeScreen.filePicker` callback writes Uris from the in-app picker
 *
 * Sole consumer: `AppNavGraph` reads `.current` and forwards to the sheet.
 */
val LocalPendingImportUri = compositionLocalOf<Uri?> { null }
