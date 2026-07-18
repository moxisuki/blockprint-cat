package io.github.moxisuki.blockprint.cat.app.core.network

interface AppHttpClient {
    suspend fun getString(url: String): AppNetworkResult<String>
}
