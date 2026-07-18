package io.github.moxisuki.blockprint.cat.app.feature.about.data

import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkConstants
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import org.json.JSONObject
import javax.inject.Inject

class HitokotoRemoteDataSource @Inject constructor(
    private val httpClient: AppHttpClient,
) {
    suspend fun fetchHitokoto(): AppNetworkResult<HitokotoQuote> {
        return when (val response = httpClient.getString(AppNetworkConstants.HITOKOTO_API_URL)) {
            is AppNetworkResult.Success -> parseHitokoto(response.value)
            is AppNetworkResult.Failure -> response
        }
    }

    private fun parseHitokoto(json: String): AppNetworkResult<HitokotoQuote> {
        return runCatching {
            val obj = JSONObject(json)
            val text = obj.optString("hitokoto").trim()
            require(text.isNotBlank())

            HitokotoQuote(
                text = text,
                from = obj.optString("from").cleanNullableText(),
                fromWho = obj.optString("from_who").cleanNullableText(),
            )
        }.fold(
            onSuccess = { AppNetworkResult.Success(it) },
            onFailure = { AppNetworkResult.Failure(cause = it) },
        )
    }
}

private fun String.cleanNullableText(): String =
    trim().takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }.orEmpty()
