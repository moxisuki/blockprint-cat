package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModSearchHit
import io.github.moxisuki.blockprint.cat.app.core.resourcepack.model.ModVersionInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModrinthSource @Inject constructor(
    private val http: AppHttpClient,
) {
    suspend fun searchMods(query: String, limit: Int = 10): List<ModSearchHit> {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "${AssetMirrors.MODRINTH_API}/search?query=$encoded&limit=$limit&index=downloads"
        val body = (http.getJson(url) as? AppNetworkResult.Success)?.value ?: return emptyList()
        val hits = body.optJSONArray("hits") ?: return emptyList()
        return (0 until hits.length()).map { i ->
            val h = hits.getJSONObject(i)
            ModSearchHit(
                slug = h.optString("slug"),
                title = h.optString("title"),
                description = h.optString("description").take(80),
                projectId = h.optString("project_id"),
                downloads = h.optInt("downloads", 0),
                iconUrl = h.optString("icon_url").takeIf { it.isNotBlank() },
            )
        }
    }

    suspend fun versionsFor(projectId: String): List<ModVersionInfo> {
        val url = "${AssetMirrors.MODRINTH_API}/project/$projectId/version"
        val rawJsonArray = org.json.JSONArray((http.getString(url) as? AppNetworkResult.Success)?.value ?: return emptyList())
        return (0 until rawJsonArray.length()).map { i ->
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
            ModVersionInfo(
                id = v.getString("id"),
                name = v.getString("name"),
                gameVersions = versions,
                fileName = primary?.optString("filename") ?: "",
                fileSize = primary?.optLong("size", -1L) ?: -1L,
                fileUrl = primary?.optString("url") ?: "",
            )
        }
    }
}
