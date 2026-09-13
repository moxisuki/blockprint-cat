package io.github.moxisuki.blockprint.cat.app.feature.community.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

internal const val McsBaseUrl = "https://www.mcschematic.top"
internal const val McsApiBaseUrl = "$McsBaseUrl/api/v1"
internal const val McsBrowserUserAgent =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

@Immutable
internal data class McsBlueprintAuthor(
    val id: String = "",
    val displayName: String = "",
    val level: Int? = null,
    val avatarUrl: String? = null,
)

@Immutable
internal data class McsBlueprintCategory(
    val id: String = "",
    val slug: String = "",
    val name: String = "",
    val description: String = "",
    val parentId: String? = null,
    val iconKey: String? = null,
    val blueprintCount: Int = 0,
)

@Immutable
internal data class McsBlueprintVersion(
    val number: Int = 1,
    val sourceFormat: String = "",
    val validationState: String? = null,
    val createdAt: String = "",
)

@Immutable
internal data class McsBlueprintEngagement(
    val viewCount: Int = 0,
    val downloadCount: Int = 0,
    val likeCount: Int = 0,
    val favouriteCount: Int = 0,
)

@Immutable
internal data class McsBlueprintGameVersion(
    val release: String = "",
    val edition: String = "",
    val family: String = "",
)

@Immutable
internal data class McsBlueprintDimensions(
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0,
) {
    override fun toString(): String = "$x x $y x $z"
}

@Immutable
internal data class McsBlueprintMaterial(
    val blockId: String,
    val count: Int,
)

@Immutable
internal data class McsBlueprintAnalysis(
    val gameVersion: String = "",
    val gameEdition: String = "",
    val gameFamily: String = "",
    val dimensions: McsBlueprintDimensions? = null,
    val sourceBlockCount: Int = 0,
    val visibleBlockCount: Int = 0,
    val paletteSize: Int = 0,
    val tileEntityCount: Int = 0,
    val entityCount: Int = 0,
    val materialKindCount: Int = 0,
    val namespaces: List<String> = emptyList(),
    val materials: List<McsBlueprintMaterial> = emptyList(),
    val materialsTruncated: Boolean = false,
    val transportAvailable: Boolean = false,
)

@Immutable
internal data class McsBlueprintSummary(
    val id: String,
    val slug: String = "",
    val title: String,
    val description: String = "",
    val attribution: String = "",
    val author: McsBlueprintAuthor = McsBlueprintAuthor(),
    val category: McsBlueprintCategory? = null,
    val featured: Boolean = false,
    val previewUrl: String? = null,
    val namespaces: List<String> = emptyList(),
    val currentVersion: McsBlueprintVersion = McsBlueprintVersion(),
    val engagement: McsBlueprintEngagement = McsBlueprintEngagement(),
    val gameVersion: McsBlueprintGameVersion = McsBlueprintGameVersion(),
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Immutable
internal data class McsBlueprintDetail(
    val summary: McsBlueprintSummary,
    val contentMarkdown: String = "",
    val viewerSourceFormat: String = "",
    val validationState: String? = null,
    val analysis: McsBlueprintAnalysis = McsBlueprintAnalysis(),
    val sourceUrl: String? = null,
    val downloadUrl: String? = null,
    val previewImages: List<String> = emptyList(),
    val originalSourceByteSize: Long? = null,
    val viewerSourceByteSize: Long? = null,
)

@Immutable
internal data class McsCommunityCategory(
    val slug: String,
    val name: String,
    val blueprintCount: Int,
)

@Immutable
internal data class McsCommunityPage(
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalCount: Int,
    val items: List<McsBlueprintSummary>,
)

internal object McsCommunityParser {
    fun parsePage(body: String): McsCommunityPage {
        val json = JSONObject(body)
        val items = json.optJSONArray("items").toBlueprintSummaries()
        return McsCommunityPage(
            page = json.optInt("page", 1),
            pageSize = json.optInt("pageSize", items.size),
            totalPages = json.optInt("totalPages", 0),
            totalCount = json.optInt("totalCount", 0),
            items = items,
        )
    }

    fun parseCategories(body: String): List<McsCommunityCategory> =
        JSONObject(body)
            .optJSONArray("items")
            .toCategoryList()
            .filter { it.name.isNotBlank() && it.slug.isNotBlank() }
            .sortedWith(compareByDescending<McsCommunityCategory> { it.blueprintCount }.thenBy { it.name })

    fun parseDetail(body: String): McsBlueprintDetail {
        val json = JSONObject(body)
        val summary = parseSummary(json)
        val analysisJson = json.optJSONObject("trustedAnalysis")
        val gameJson = analysisJson?.optJSONObject("sourceGameVersion")
        val dimensionsJson = analysisJson?.optJSONObject("dimensions")
        val version = json.optJSONObject("currentVersion")
        val previewImages = json.optJSONArray("previewImages")
            .toPreviewUrlList()
            .ifEmpty { listOfNotNull(json.optStringOrNull("previewUrl")) }
        val analysis = McsBlueprintAnalysis(
            gameVersion = gameJson?.optString("release").orEmpty(),
            gameEdition = gameJson?.optString("edition").orEmpty(),
            gameFamily = gameJson?.optString("family").orEmpty(),
            dimensions = dimensionsJson?.toDimensions(),
            sourceBlockCount = analysisJson?.optInt("sourceBlockCount") ?: 0,
            visibleBlockCount = analysisJson?.optInt("visibleBlockCount") ?: 0,
            paletteSize = analysisJson?.optInt("paletteSize") ?: 0,
            tileEntityCount = analysisJson?.optInt("tileEntityCount") ?: 0,
            entityCount = analysisJson?.optInt("entityCount") ?: 0,
            materialKindCount = analysisJson?.optInt("materialKindCount") ?: 0,
            namespaces = analysisJson?.optJSONArray("namespaces").toStringList(),
            materials = analysisJson?.optJSONArray("materials").toMaterials(),
            materialsTruncated = analysisJson?.optBoolean("materialsTruncated") ?: false,
            transportAvailable = analysisJson?.optBoolean("transportAvailable") ?: false,
        )
        return McsBlueprintDetail(
            summary = summary.copy(
                currentVersion = summary.currentVersion.copy(
                    number = version?.optInt("number", summary.currentVersion.number)
                        ?: summary.currentVersion.number,
                    validationState = version?.optStringOrNull("validationState"),
                ),
            ),
            contentMarkdown = json.optString("contentMarkdown"),
            viewerSourceFormat = json.optString("viewerSourceFormat"),
            validationState = version?.optStringOrNull("validationState"),
            analysis = analysis,
            sourceUrl = absoluteUrl(json.optStringOrNull("sourceUrl")),
            downloadUrl = absoluteUrl(json.optStringOrNull("downloadUrl")),
            previewImages = previewImages.mapNotNull(::absoluteUrl),
            originalSourceByteSize = json.optLongOrNull("originalSourceByteSize"),
            viewerSourceByteSize = json.optLongOrNull("viewerSourceByteSize"),
        )
    }

    fun parseSummary(json: JSONObject): McsBlueprintSummary {
        val version = json.optJSONObject("currentVersion")
        val gameVersion = json.optJSONObject("sourceGameVersion")
            ?: json.optJSONObject("trustedAnalysis")?.optJSONObject("sourceGameVersion")
        return McsBlueprintSummary(
            id = json.optString("id"),
            slug = json.optString("slug"),
            title = json.optString("title").ifBlank { json.optString("slug") },
            description = json.optString("description"),
            attribution = json.optString("attribution"),
            author = json.optJSONObject("author").toAuthor(),
            category = json.optJSONObject("category").toCategory(),
            featured = json.optBoolean("featured"),
            previewUrl = absoluteUrl(json.optStringOrNull("previewUrl")),
            namespaces = json.optJSONArray("namespaces").toStringList(),
            currentVersion = McsBlueprintVersion(
                number = version?.optInt("number", 1) ?: 1,
                sourceFormat = version?.optString("sourceFormat").orEmpty(),
                createdAt = version?.optString("createdAt").orEmpty(),
            ),
            engagement = json.optJSONObject("engagement").toEngagement(),
            gameVersion = McsBlueprintGameVersion(
                release = gameVersion?.optString("release").orEmpty(),
                edition = gameVersion?.optString("edition").orEmpty(),
                family = gameVersion?.optString("family").orEmpty(),
            ),
            createdAt = json.optString("createdAt"),
            updatedAt = json.optString("updatedAt"),
        )
    }
}

private fun JSONArray?.toBlueprintSummaries(): List<McsBlueprintSummary> =
    if (this == null) emptyList() else {
        (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let(McsCommunityParser::parseSummary)
        }
    }

private fun JSONArray?.toCategoryList(): List<McsCommunityCategory> =
    if (this == null) emptyList() else {
        (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let { item ->
                McsCommunityCategory(
                    slug = item.optString("slug"),
                    name = item.optString("name"),
                    blueprintCount = item.optInt("blueprintCount"),
                )
            }
        }
    }

private fun JSONArray?.toStringList(): List<String> =
    if (this == null) emptyList() else {
        (0 until length()).mapNotNull { index ->
            optString(index).trim().takeIf { it.isNotBlank() }
        }
    }

private fun JSONArray?.toPreviewUrlList(): List<String> =
    if (this == null) {
        emptyList()
    } else {
        (0 until length())
            .mapNotNull { index ->
                optJSONObject(index)?.let { item ->
                    item.optStringOrNull("url")?.let { url ->
                        PreviewImageUrl(
                            url = url,
                            isCover = item.optString("role").equals("cover", ignoreCase = true),
                            position = item.optInt("position", index),
                        )
                    }
                }
            }
            .sortedWith(compareByDescending<PreviewImageUrl> { it.isCover }.thenBy { it.position })
            .map { it.url }
    }

private data class PreviewImageUrl(
    val url: String,
    val isCover: Boolean,
    val position: Int,
)

private fun JSONArray?.toMaterials(): List<McsBlueprintMaterial> =
    if (this == null) emptyList() else {
        (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let { item ->
                val blockId = item.optString("blockId").trim()
                if (blockId.isBlank()) null
                else McsBlueprintMaterial(blockId = blockId, count = item.optInt("count"))
            }
        }.sortedByDescending { it.count }
    }

private fun JSONObject?.toAuthor(): McsBlueprintAuthor =
    this?.let {
        McsBlueprintAuthor(
            id = it.optString("id"),
            displayName = it.optString("displayName"),
            level = it.optIntOrNull("level"),
            avatarUrl = absoluteUrl(it.optStringOrNull("avatarUrl")),
        )
    } ?: McsBlueprintAuthor()

private fun JSONObject?.toCategory(): McsBlueprintCategory? =
    this?.let {
        McsBlueprintCategory(
            id = it.optString("id"),
            slug = it.optString("slug"),
            name = it.optString("name"),
            description = it.optString("description"),
            parentId = it.optStringOrNull("parentId"),
            iconKey = it.optStringOrNull("iconKey"),
            blueprintCount = it.optInt("blueprintCount"),
        )
    }

private fun JSONObject?.toEngagement(): McsBlueprintEngagement =
    this?.let {
        McsBlueprintEngagement(
            viewCount = it.optInt("viewCount"),
            downloadCount = it.optInt("downloadCount"),
            likeCount = it.optInt("likeCount"),
            favouriteCount = it.optInt("favouriteCount"),
        )
    } ?: McsBlueprintEngagement()

private fun JSONObject.toDimensions(): McsBlueprintDimensions =
    McsBlueprintDimensions(
        x = optInt("x"),
        y = optInt("y"),
        z = optInt("z"),
    )

private fun JSONObject.optStringOrNull(key: String): String? =
    optString(key).trim().takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null

internal fun absoluteUrl(url: String?): String? =
    url?.trim()?.takeIf { it.isNotBlank() }?.let {
        when {
            it.startsWith("http://") || it.startsWith("https://") -> it
            it.startsWith("/") -> "$McsBaseUrl$it"
            else -> "$McsBaseUrl/$it"
        }
    }

internal class McsCommunityException(message: String) : RuntimeException(message)
