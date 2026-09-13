package io.github.moxisuki.blockprint.cat.app.feature.settings

import androidx.compose.runtime.Immutable
import io.github.moxisuki.blockprint.cat.app.core.cache.AppCacheCategory
import io.github.moxisuki.blockprint.cat.app.core.cache.AppCacheStats

@Immutable
data class SettingsState(
    val localBlueprintTreeUri: String? = null,
    val localBlueprintTreeDocumentId: String? = null,
    val communityEnabled: Boolean = true,
    val isBlueprintDirectoryConfirmVisible: Boolean = false,
    val isRestoreConfirmVisible: Boolean = false,
    val pendingRestoreFileUri: String? = null,
    val pendingRestoreFileName: String? = null,
    val isBackupRunning: Boolean = false,
    val isRestoreRunning: Boolean = false,
    val backupRestoreFeedback: SettingsBackupRestoreFeedback? = null,
    val installedResourcePackCount: Int = 0,
    val isCacheManagerVisible: Boolean = false,
    val cacheStats: AppCacheStats? = null,
    val isCacheStatsLoading: Boolean = false,
    val isCacheClearing: Boolean = false,
    val pendingCacheClearCategory: AppCacheCategory? = null,
    val cacheErrorMessage: String? = null,
)

@Immutable
sealed interface SettingsBackupRestoreFeedback {
    data class BackupSuccess(
        val fileName: String,
        val fileCount: Int,
        val sizeBytes: Long,
    ) : SettingsBackupRestoreFeedback

    data class RestoreSuccess(
        val fileCount: Int,
        val hasManifest: Boolean,
    ) : SettingsBackupRestoreFeedback

    data class Error(
        val message: String,
    ) : SettingsBackupRestoreFeedback
}
