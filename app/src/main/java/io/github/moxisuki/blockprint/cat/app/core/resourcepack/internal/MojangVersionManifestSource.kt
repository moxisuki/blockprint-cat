package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class MojangVersionManifestSource @Inject constructor(
    private val http: AppHttpClient,
) {
    suspend fun latestRelease(): AppNetworkResult<String> = when (val manifest = fetchManifest()) {
        is AppNetworkResult.Failure -> manifest
        is AppNetworkResult.Success -> runCatching {
            manifest.value.optJSONObject("latest")?.optString("release")
                ?: throw IllegalStateException("manifest.latest.release missing")
        }.fold(
            onSuccess = { AppNetworkResult.Success(it) },
            onFailure = { t -> AppNetworkResult.Failure(cause = t, message = "manifest parse") },
        )
    }

    /**
     * Fetches the version metadata JSON. Officially-hosted URLs (piston-meta.mojang.com)
     * are transparently redirected to the BMCLAPI mirror via [AssetMirrors.mojang].
     */
    suspend fun versionJson(url: String): AppNetworkResult<JSONObject> =
        http.getJson(AssetMirrors.mojang(url), userAgent = AssetMirrors.BROWSER_UA)

    private suspend fun fetchManifest(): AppNetworkResult<JSONObject> =
        http.getJson("${AssetMirrors.BMC_API}/mc/game/version_manifest.json", userAgent = AssetMirrors.BROWSER_UA)
}

private inline fun <T, R> AppNetworkResult<T>.map(transform: (T) -> R): AppNetworkResult<R> = when (this) {
    is AppNetworkResult.Success -> AppNetworkResult.Success(transform(value))
    is AppNetworkResult.Failure -> this
}