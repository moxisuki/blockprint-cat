package io.github.moxisuki.blockprint.cat.app.core.network

import org.json.JSONObject

interface AppHttpClient {
    suspend fun getString(url: String): AppNetworkResult<String>

    suspend fun getJson(
        url: String,
        userAgent: String? = null,
    ): AppNetworkResult<JSONObject>

    suspend fun getBytes(
        url: String,
        userAgent: String? = null,
        progress: ((Float) -> Unit)? = null,
    ): AppNetworkResult<ByteArray?>
}
