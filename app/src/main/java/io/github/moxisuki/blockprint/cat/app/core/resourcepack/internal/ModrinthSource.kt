package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray

@Singleton
class ModrinthSource @Inject constructor(
    private val http: AppHttpClient,
) {
    suspend fun searchMods(query: String, limit: Int = 10): List<ModSearchHit> {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "${AssetMirrors.MODRINTH_API}/search?query=$encoded&limit=$limit&index=downloads"
        val body = (http.getJson(url, userAgent = AssetMirrors.BROWSER_UA) as? AppNetworkResult.Success)?.value
            ?: return emptyList()
        val hits = body.optJSONArray("hits") ?: return emptyList()
        return (0 until hits.length()).map { i ->
            val h = hits.getJSONObject(i)
            val slug = h.optString("slug").trim()
            ModSearchHit(
                slug = slug,
                title = h.optString("title"),
                description = h.optString("description").take(80),
                projectId = h.optString("project_id").ifBlank { slug },
                downloads = h.optInt("downloads", 0),
                iconUrl = h.optString("icon_url").takeIf { it.isNotBlank() },
            )
        }
    }

    suspend fun versionsFor(projectId: String): List<ModVersionInfo> {
        val url = "${AssetMirrors.MODRINTH_API}/project/$projectId/version"
        // Modrinth's /version endpoint returns a top-level JSON array, which AppHttpClient.getJson
        // (JSONObject-typed) cannot serve. We use getString + runCatching for parse safety.
        val rawText = (http.getString(url) as? AppNetworkResult.Success)?.value ?: return emptyList()
        val rawJsonArray = runCatching { JSONArray(rawText) }.getOrNull() ?: return emptyList()
        return runCatching {
            (0 until rawJsonArray.length()).mapNotNull { i ->
                val v = rawJsonArray.getJSONObject(i)
                val files = v.optJSONArray("files")
                var primary: org.json.JSONObject? = null
                for (j in 0 until (files?.length() ?: 0)) {
                    val f = files!!.getJSONObject(j)
                    if (f.optBoolean("primary", false)) { primary = f; break }
                }
                if (primary == null && files != null && files.length() > 0) primary = files.getJSONObject(0)
                val gameVersions = v.optJSONArray("game_versions")
                val versions = (0 until (gameVersions?.length() ?: 0)).map { gameVersions!!.getString(it) }
                val version = ModVersionInfo(
                    id = v.optString("id"),
                    name = v.optString("name"),
                    gameVersions = versions,
                    fileName = primary?.optString("filename") ?: "",
                    fileSize = primary?.optLong("size", -1L) ?: -1L,
                    fileUrl = primary?.optString("url") ?: "",
                )
                version.takeIf { it.id.isNotBlank() && it.fileName.isNotBlank() && it.fileUrl.isNotBlank() }
            }
        }.getOrElse { emptyList() }
    }
}
