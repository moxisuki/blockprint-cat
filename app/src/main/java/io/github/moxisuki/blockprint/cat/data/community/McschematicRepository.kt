package io.github.moxisuki.blockprint.cat.data.community

import android.content.Context
import io.github.moxisuki.blockprint.cat.data.McschematicClient
import io.github.moxisuki.blockprint.cat.data.McschematicCookieStore
import io.github.moxisuki.blockprint.cat.data.Schematic
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McschematicRepository @Inject constructor(
    private val client: McschematicClient,
    private val cookieStore: McschematicCookieStore,
) : CommunityRepository {

    override val source = CommunitySource.MCS

    override suspend fun isAvailable(): Boolean = cookieStore.isLoggedIn()

    override suspend fun listCount(filter: String): Int = client.schematicNum(filter = filter)

    override suspend fun list(begin: Int, filter: String, heatSort: Boolean): List<UnifiedSchematic> {
        return client.listSchematics(begin = begin, filter = filter, heatSort = heatSort)
            .map { it.toUnified() }
    }

    override suspend fun loadDetail(id: String): UnifiedDetail {
        val md = client.markdown(id)
        val reqs = client.requirements(id).map { req ->
            UnifiedMaterial(
                blockId = "${req.modId}:${req.blockName}",
                displayName = req.blockName,
                iconUrl = null,
                count = req.value,
                countText = req.value.toString(),
            )
        }
        val comments = client.comments(id)
        return UnifiedDetail(
            source = CommunitySource.MCS,
            id = id,
            markdown = md,
            requirements = reqs,
            comments = comments,
            loading = false,
        )
    }

    override suspend fun loadPreview(id: String, detail: UnifiedDetail?): ByteArray? =
        client.downloadPreview(id)

    override suspend fun downloadToFile(
        context: Context,
        schematic: UnifiedSchematic,
        detail: UnifiedDetail?,
        targetDir: File,
        onProgress: suspend (Long, Long) -> Unit,
    ): File = client.downloadSchematicToFile(
        context = context,
        uuid = schematic.id,
        targetDir = targetDir,
        schematicType = schematic.typeForSuffix,
        onProgress = { bytes, total -> onProgress(bytes, total) },
    )

    private fun Schematic.toUnified(): UnifiedSchematic = UnifiedSchematic(
        source = CommunitySource.MCS,
        id = uuid,
        name = name,
        author = nickName,
        heat = heat,
        size = size,
        tags = tags,
        description = description,
        updateTime = updateTime,
        typeForSuffix = type,
    )
}
