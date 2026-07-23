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
    suspend fun latestRelease(): AppNetworkResult<String> = fetchManifest()
        .map { it.getJSONObject("latest").getString("release") }

    suspend fun versionJson(url: String): AppNetworkResult<JSONObject> = http.getJson(url)

    private suspend fun fetchManifest(): AppNetworkResult<JSONObject> =
        http.getJson("${AssetMirrors.BMC_API}/mc/game/version_manifest.json")
}

private inline fun <T, R> AppNetworkResult<T>.map(transform: (T) -> R): AppNetworkResult<R> = when (this) {
    is AppNetworkResult.Success -> AppNetworkResult.Success(transform(value))
    is AppNetworkResult.Failure -> this
}
