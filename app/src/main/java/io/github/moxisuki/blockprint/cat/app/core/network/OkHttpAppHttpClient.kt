package io.github.moxisuki.blockprint.cat.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class OkHttpAppHttpClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : AppHttpClient {

    override suspend fun getString(url: String): AppNetworkResult<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    return@withContext AppNetworkResult.Failure(
                        code = response.code,
                        message = response.message,
                    )
                }
                AppNetworkResult.Success(body)
            }
        }.getOrElse { cause ->
            AppNetworkResult.Failure(cause = cause)
        }
    }
}
