package io.github.moxisuki.blockprint.cat.app.feature.community.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

internal const val McsBaseUrl = "https://www.mcschematic.top"

internal const val McsBrowserUserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@Immutable
internal data class McsLoginStatus(
    val uuid: String,
    val authority: Int,
    val permissions: Int,
    val message: String,
)

@Immutable
internal data class McsSchematic(
    val uuid: String,
    val type: Int,
    val name: String,
    val nickName: String,
    val authorUuid: String,
    val avatarUrl: String?,
    val size: Triple<Int, Int, Int>?,
    val heat: Int,
    val tags: List<String>,
    val description: String,
    val updateTime: String,
    val uploadTime: String,
    val userPrivate: Int,
)

@Immutable
internal data class McsCommunityTag(
    val name: String,
    val priority: Int,
    val type: Int,
    val subtags: List<String>,
)

@Immutable
internal data class McsSchematicRequirement(
    val modId: String,
    val blockName: String,
    val count: Int,
) {
    val blockId: String
        get() = "$modId:$blockName"
}

@Immutable
internal data class McsCommunityPage(
    val total: Int,
    val items: List<McsSchematic>,
)

internal object McsCommunityParser {
    fun parseLoginStatus(body: String): McsLoginStatus {
        val json = JSONObject(body)
        return McsLoginStatus(
            uuid = json.optString("uuid"),
            authority = json.optInt("authority"),
            permissions = json.optInt("permissions"),
            message = json.optString("message"),
        )
    }

    fun parseSchematics(body: String): List<McsSchematic> {
        val json = JSONArray(body)
        return (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            val sizeParts = item.optString("size", "[0,0,0]")
                .removeSurrounding("[", "]")
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
            val size = if (sizeParts.size == 3) {
                Triple(sizeParts[0], sizeParts[1], sizeParts[2])
            } else {
                null
            }
            val tags = runCatching {
                val tagsJson = JSONArray(item.optString("tags", "[]"))
                (0 until tagsJson.length()).map { tagIndex -> tagsJson.getString(tagIndex) }
            }.getOrDefault(emptyList())

            McsSchematic(
                uuid = item.optString("uuid"),
                type = item.optInt("type"),
                name = item.optString("name"),
                nickName = item.optString("nickName"),
                authorUuid = item.optString("author"),
                avatarUrl = item.optString("avatarUrl")
                    .takeIf { it.isNotBlank() && it != "null" },
                size = size,
                heat = item.optInt("heat"),
                tags = tags,
                description = item.optString("description"),
                updateTime = item.optString("updateTime"),
                uploadTime = item.optString("uploadTime"),
                userPrivate = item.optInt("userPrivate"),
            )
        }
    }

    fun parseTags(body: String): List<McsCommunityTag> {
        val json = JSONArray(body)
        return (0 until json.length()).mapNotNull { index ->
            val item = json.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name").trim()
            if (name.isBlank()) return@mapNotNull null
            val subtags = runCatching {
                val subtagsJson = JSONArray(item.optString("subtags", "[]"))
                (0 until subtagsJson.length()).mapNotNull { subtagIndex ->
                    subtagsJson.optString(subtagIndex).trim().takeIf { it.isNotBlank() }
                }
            }.getOrDefault(emptyList())

            McsCommunityTag(
                name = name,
                priority = item.optInt("priority"),
                type = item.optInt("type"),
                subtags = subtags,
            )
        }
    }

    fun parseRequirements(body: String): List<McsSchematicRequirement> {
        val json = JSONArray(body)
        return (0 until json.length()).mapNotNull { index ->
            val raw = json.opt(index) ?: return@mapNotNull null
            val item = runCatching {
                when (raw) {
                    is String -> JSONObject(raw)
                    is JSONObject -> raw
                    else -> JSONObject(raw.toString())
                }
            }.getOrNull() ?: return@mapNotNull null
            val blockName = item.optString("blockName").trim()
            if (blockName.isBlank()) return@mapNotNull null
            McsSchematicRequirement(
                modId = item.optString("modId", "minecraft").ifBlank { "minecraft" },
                blockName = blockName,
                count = item.optInt("value"),
            )
        }
    }
}

internal class McsCommunityException(message: String) : RuntimeException(message)
