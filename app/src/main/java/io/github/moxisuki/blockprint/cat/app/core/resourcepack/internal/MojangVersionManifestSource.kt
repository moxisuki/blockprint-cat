package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import android.util.Log
import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

private const val TAG = "MojangManifest"

/** Latest release metadata together with the official version metadata URL. */
data class VanillaVersionInfo(
    val release: String,
    val officialVersionUrl: String,
)

@Singleton
class MojangVersionManifestSource @Inject constructor(
    private val http: AppHttpClient,
) {
    /**
     * Fetches the version manifest and returns the latest-release version id together
     * with the official Mojang URL of that version's metadata JSON (so the caller can
     * apply [AssetMirrors.mojang] to redirect to the BMCLAPI mirror).
     */
    suspend fun latestVersionInfo(): AppNetworkResult<VanillaVersionInfo> = when (val manifest = fetchManifest()) {
        is AppNetworkResult.Failure -> {
            Log.w(TAG, "manifest fetch failed: $manifest")
            manifest
        }
        is AppNetworkResult.Success -> runCatching {
            val root = manifest.value
            val release = root.optJSONObject("latest")?.optString("release")
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("manifest.latest.release missing")
            val versions = root.optJSONArray("versions")
                ?: throw IllegalStateException("manifest.versions missing")
            var officialUrl: String? = null
            for (i in 0 until versions.length()) {
                val v = versions.getJSONObject(i)
                if (v.optString("id") == release) {
                    officialUrl = v.optString("url").takeIf { it.isNotBlank() }
                    break
                }
            }
            officialUrl ?: throw IllegalStateException("manifest.versions[$release].url missing")
            Log.i(TAG, "latest=$release url=$officialUrl")
            VanillaVersionInfo(release = release, officialVersionUrl = officialUrl)
        }.fold(
            onSuccess = { AppNetworkResult.Success(it) },
            onFailure = { t ->
                Log.e(TAG, "manifest parse failed", t)
                AppNetworkResult.Failure(cause = t, message = "manifest parse")
            },
        )
    }

    /**
     * Fetches the version metadata JSON. Officially-hosted URLs (piston-meta.mojang.com)
     * are transparently redirected to the BMCLAPI mirror via [AssetMirrors.mojang].
     */
    suspend fun versionJson(url: String): AppNetworkResult<JSONObject> {
        val swapped = AssetMirrors.mojang(url)
        Log.d(TAG, "versionJson() $url → $swapped")
        return http.getJson(swapped, userAgent = AssetMirrors.BROWSER_UA)
    }

    private suspend fun fetchManifest(): AppNetworkResult<JSONObject> {
        val url = "${AssetMirrors.BMC_API}/mc/game/version_manifest.json"
        Log.d(TAG, "fetchManifest() $url")
        return http.getJson(url, userAgent = AssetMirrors.BROWSER_UA)
    }
}
