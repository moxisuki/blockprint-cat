package io.github.moxisuki.blockprint.cat.app.feature.community.data

import android.util.Log
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintFormat
import io.github.moxisuki.blockprint.cat.app.core.data.blueprint.BlueprintRepository
import io.github.moxisuki.blockprint.cat.app.core.persistence.AppSettingsRepository
import io.github.moxisuki.blockprint.cat.app.core.persistence.McsAuthCookies
import io.github.moxisuki.blockprint.cat.app.feature.community.McsLoginLogTag
import io.github.moxisuki.blockprint.cat.app.feature.community.toDebugSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val McsPageSize = 15

@Singleton
internal class McsCommunityRepository @Inject constructor(
    private val remoteDataSource: McsCommunityRemoteDataSource,
    private val settingsRepository: AppSettingsRepository,
    private val blueprintRepository: BlueprintRepository,
) {
    val authCookies: Flow<McsAuthCookies> = settingsRepository.mcsAuthCookies

    suspend fun hasLocalLogin(): Boolean = settingsRepository.mcsAuthCookies.first().isLoggedIn

    suspend fun saveLoginCookies(cookies: McsAuthCookies) {
        Log.d(McsLoginLogTag, "Repository saveLoginCookies: ${cookies.toDebugSummary()}")
        settingsRepository.setMcsAuthCookies(cookies)
    }

    suspend fun verifyAndSaveLogin(cookies: McsAuthCookies): Boolean {
        Log.d(McsLoginLogTag, "Repository verifyAndSaveLogin start: ${cookies.toDebugSummary()}")
        if (!cookies.isLoggedIn) {
            Log.w(
                McsLoginLogTag,
                "Repository verifyAndSaveLogin rejected locally: isLoggedIn=${cookies.isLoggedIn}, hasUuid=${cookies.uuid.isNotBlank()}",
            )
            return false
        }
        val status = remoteDataSource.loginStatus(cookies)
        Log.d(
            McsLoginLogTag,
            "Repository loginStatus returned: uuid=${status.uuid}, authority=${status.authority}, permissions=${status.permissions}, message=${status.message.ifBlank { "-" }}",
        )
        if (status.uuid.isBlank()) {
            Log.w(McsLoginLogTag, "Repository verifyAndSaveLogin rejected: remote uuid blank")
            return false
        }
        settingsRepository.setMcsAuthCookies(
            cookies.copy(
                uuid = status.uuid,
                nickname = status.message
                    .takeIf { it.isNotBlank() && !it.equals("success", ignoreCase = true) }
                .orEmpty(),
            ),
        )
        Log.d(McsLoginLogTag, "Repository verifyAndSaveLogin saved auth cookies")
        return true
    }

    suspend fun clearLogin() {
        Log.d(McsLoginLogTag, "Repository clearLogin")
        settingsRepository.clearMcsAuthCookies()
    }

    suspend fun refreshLoginStatus(): McsLoginStatus? {
        Log.d(McsLoginLogTag, "Repository refreshLoginStatus start")
        if (!hasLocalLogin()) {
            Log.d(McsLoginLogTag, "Repository refreshLoginStatus skipped: no local login")
            return null
        }
        return runCatching { remoteDataSource.loginStatus() }
            .onSuccess { status ->
                Log.d(
                    McsLoginLogTag,
                    "Repository refreshLoginStatus success: uuid=${status.uuid}, message=${status.message.ifBlank { "-" }}",
                )
                val current = settingsRepository.mcsAuthCookies.first()
                val displayName = status.message
                    .takeIf { it.isNotBlank() && !it.equals("success", ignoreCase = true) }
                    ?: current.nickname
                settingsRepository.setMcsAuthCookies(
                    current.copy(
                        uuid = status.uuid.ifBlank { current.uuid },
                        nickname = displayName,
                    ),
                )
            }
            .onFailure { error ->
                Log.w(McsLoginLogTag, "Repository refreshLoginStatus failed", error)
            }
            .getOrNull()
    }

    suspend fun loadPage(
        begin: Int,
        filter: String,
        heatSort: Boolean,
    ): McsCommunityPage {
        val total = if (begin == 0) {
            remoteDataSource.schematicCount(filter)
        } else {
            -1
        }
        return McsCommunityPage(
            total = total,
            items = remoteDataSource.schematics(
                begin = begin,
                filter = filter,
                heatSort = heatSort,
            ),
        )
    }

    suspend fun loadTags(): List<McsCommunityTag> =
        remoteDataSource.tags(begin = 0)

    suspend fun loadRequirements(uuid: String): List<McsSchematicRequirement> =
        remoteDataSource.requirements(uuid)

    suspend fun loadMarkdown(uuid: String): String =
        remoteDataSource.markdown(uuid)

    suspend fun downloadToLocalBlueprints(
        uuid: String,
        title: String,
        format: BlueprintFormat,
        onProgress: suspend (bytes: Long, total: Long) -> Unit,
    ) {
        val bytes = remoteDataSource.downloadSchematic(
            uuid = uuid,
            onProgress = onProgress,
        )
        blueprintRepository.importDownloadedBlueprint(
            fileName = "${title.ifBlank { uuid }.toCommunityFileName()}${format.communityExtension()}",
            bytes = bytes,
        )
    }
}

internal fun mcsHasMore(
    total: Int,
    loaded: Int,
    latestPageSize: Int,
): Boolean =
    if (total > 0) {
        loaded < total
    } else {
        latestPageSize >= McsPageSize
    }

private fun BlueprintFormat.communityExtension(): String =
    when (this) {
        BlueprintFormat.Litematica -> ".litematic"
        BlueprintFormat.Schematic -> ".schem"
        BlueprintFormat.Nbt -> ".nbt"
        BlueprintFormat.BuildingHelper -> ".json"
        BlueprintFormat.Unknown -> ".litematic"
    }

private fun String.toCommunityFileName(): String =
    trim()
        .replace('\\', '_')
        .replace('/', '_')
        .replace(':', '_')
        .replace('*', '_')
        .replace('?', '_')
        .replace('"', '_')
        .replace('<', '_')
        .replace('>', '_')
        .replace('|', '_')
        .take(80)
        .ifBlank { "community_blueprint" }
