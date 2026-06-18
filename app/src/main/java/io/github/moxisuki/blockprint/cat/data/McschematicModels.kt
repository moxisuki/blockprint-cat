package io.github.moxisuki.blockprint.cat.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Domain models for mcschematic.top API responses.
 *
 * Most endpoints return JSON but several have quirks (see parsers below):
 *   - `/api/requirements` returns an array of independent JSON-encoded strings —
 *     each element must be parsed separately.
 *   - `/api/markdown` returns a plain string (sometimes empty `""`).
 *   - `/api/schematicFile` returns gzip-compressed octet-stream with
 *     `Content-Disposition: attachment; filename="<uuid>.litematic"`.
 */
data class LoginStatus(
    val uuid: String,
    val authority: Int,
    val permissions: Int,
    val message: String,
)

data class Schematic(
    val uuid: String,
    val type: Int,            // 0=nbt, 1=litematic, 2=schem, 3=schematic
    val name: String,
    val nickName: String,     // 作者昵称
    val authorUuid: String,
    val avatarUrl: String?,
    val size: Triple<Int, Int, Int>?,  // [x, y, z]
    val heat: Int,
    val tags: List<String>,
    val description: String,
    val updateTime: String,
    val uploadTime: String,
    val userPrivate: Int,
) {
    /** "minecraft:oak_planks" → 可用于 BlockI18n 查询 */
    fun asBlockNames(): List<String> = tags.map { tag ->
        // 标签本身可能就是方块名（minecraft / 其他 modId:xxx）
        if (tag.contains(":")) tag else "minecraft:$tag"
    }
}

data class SchematicRequirement(
    val modId: String,
    val blockName: String,
    val value: Int,
)

data class SchematicComment(
    val uuid: String,
    val content: String,
    val nickName: String,
    val avatarUrl: String?,
    val createTime: String,
)

object McschematicParser {

    fun parseLoginStatus(json: JSONObject): LoginStatus = LoginStatus(
        uuid = json.optString("uuid"),
        authority = json.optInt("authority"),
        permissions = json.optInt("permissions"),
        message = json.optString("message"),
    )

    fun parseSchematics(json: JSONArray): List<Schematic> = (0 until json.length()).map { i ->
        val o = json.getJSONObject(i)
        val sizeStr = o.optString("size", "[0,0,0]").removeSurrounding("[", "]")
        val parts = sizeStr.split(",").mapNotNull { it.trim().toIntOrNull() }
        val size = if (parts.size == 3) Triple(parts[0], parts[1], parts[2]) else null

        // tags 字段是 JSON 字符串二次序列化
        val tags: List<String> = runCatching {
            val arr = JSONArray(o.optString("tags", "[]"))
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())

        Schematic(
            uuid = o.getString("uuid"),
            type = o.optInt("type"),
            name = o.optString("name"),
            nickName = o.optString("nickName"),
            authorUuid = o.optString("author"),
            avatarUrl = o.optString("avatarUrl").takeIf { it.isNotEmpty() && it != "null" },
            size = size,
            heat = o.optInt("heat"),
            tags = tags,
            description = o.optString("description"),
            updateTime = o.optString("updateTime"),
            uploadTime = o.optString("uploadTime"),
            userPrivate = o.optInt("userPrivate"),
        )
    }

    /**
     * `/api/requirements` returns an array whose elements are themselves
     * JSON-encoded strings — each must be parsed individually.
     */
    fun parseRequirements(json: JSONArray): List<SchematicRequirement> = (0 until json.length()).mapNotNull { i ->
        val item = json.get(i)
        val str = when (item) {
            is String -> item
            else -> item?.toString() ?: return@mapNotNull null
        }
        runCatching {
            val o = JSONObject(str)
            SchematicRequirement(
                modId = o.optString("modId", "minecraft"),
                blockName = o.optString("blockName"),
                value = o.optInt("value"),
            )
        }.getOrNull()
    }

    fun parseComments(json: JSONArray): List<SchematicComment> = (0 until json.length()).map { i ->
        val o = json.getJSONObject(i)
        SchematicComment(
            uuid = o.optString("uuid"),
            content = o.optString("content"),
            nickName = o.optString("nickName"),
            avatarUrl = o.optString("avatarUrl").takeIf { it.isNotEmpty() && it != "null" },
            createTime = o.optString("createTime"),
        )
    }

    fun parseSchematicNum(json: JSONObject): Int = json.optInt("count", 0)
}
