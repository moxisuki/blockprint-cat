package io.github.moxisuki.blockprint.cat.app.core.network

import org.json.JSONObject
import java.io.File

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

    suspend fun downloadTo(
        url: String,
        destination: File,
        userAgent: String? = null,
        progress: ((Float) -> Unit)? = null,
    ): AppNetworkResult<File>
}
