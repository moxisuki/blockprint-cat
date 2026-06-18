package io.github.moxisuki.blockprint.cat.data.error

import io.github.moxisuki.blockprint.cat.data.community.CmsCloudflareException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class AppError {
    abstract val cause: Throwable?

    data class Network(override val cause: Throwable? = null) : AppError()
    data class Timeout(override val cause: Throwable? = null) : AppError()
    data class CmsCloudflare(override val cause: Throwable? = null) : AppError()
    data class Http(val code: Int, val msg: String? = null, override val cause: Throwable? = null) : AppError()
    data class Unknown(val message: String? = null, override val cause: Throwable? = null) : AppError()

    companion object {
        fun toAppError(e: Throwable): AppError = when (e) {
            is AppError -> e
            is CmsCloudflareException -> CmsCloudflare(e)
            is UnknownHostException -> Network(e)
            is SocketTimeoutException -> Timeout(e)
            else -> Unknown(e.message, e)
        }
    }
}
