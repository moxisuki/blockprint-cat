package io.github.moxisuki.blockprint.cat.app.testfakes

import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import org.json.JSONObject

class FakeAppHttpClient(
    private val jsonResponses: Map<String, String> = emptyMap(),
    private val jsonFailures: Set<String> = emptySet(),
    private val bytesResponses: Map<String, ByteArray> = emptyMap(),
    private val progressRecorder: ((Float) -> Unit)? = null,
) : AppHttpClient {
    override suspend fun getString(url: String): AppNetworkResult<String> =
        AppNetworkResult.Success("")

    override suspend fun getJson(url: String, userAgent: String?): AppNetworkResult<JSONObject> {
        val body = jsonResponses[url] ?: return AppNetworkResult.Failure(message = "no fake for $url")
        if (url in jsonFailures) return AppNetworkResult.Failure(message = "forced failure")
        return runCatching { AppNetworkResult.Success(JSONObject(body)) }
            .getOrElse { AppNetworkResult.Failure(cause = it) }
    }

    override suspend fun getBytes(
        url: String,
        userAgent: String?,
        progress: ((Float) -> Unit)?,
    ): AppNetworkResult<ByteArray?> {
        val data = bytesResponses[url] ?: return AppNetworkResult.Failure(message = "no fake bytes for $url")
        progress?.invoke(0.5f)
        progress?.invoke(1f)
        progressRecorder?.invoke(1f)
        return AppNetworkResult.Success(data)
    }
}
