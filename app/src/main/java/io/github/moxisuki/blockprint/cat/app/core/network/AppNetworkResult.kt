package io.github.moxisuki.blockprint.cat.app.core.network

sealed interface AppNetworkResult<out T> {
    data class Success<T>(val value: T) : AppNetworkResult<T>

    data class Failure(
        val code: Int? = null,
        val message: String? = null,
        val cause: Throwable? = null,
    ) : AppNetworkResult<Nothing>
}
