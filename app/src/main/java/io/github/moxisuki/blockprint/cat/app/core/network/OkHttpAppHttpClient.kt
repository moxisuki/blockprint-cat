package io.github.moxisuki.blockprint.cat.app.core.network

import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

private const val TAG = "OkHttpAppHttpClient"

class OkHttpAppHttpClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) : AppHttpClient {

    override suspend fun getString(url: String): AppNetworkResult<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    Log.w(TAG, "GET text failed code=${response.code} message=${response.message} url=$url")
                    return@withContext AppNetworkResult.Failure(code = response.code, message = response.message)
                }
                AppNetworkResult.Success(body)
            }
        }.getOrElse { cause ->
            Log.w(TAG, "GET text exception url=$url", cause)
            AppNetworkResult.Failure(cause = cause)
        }
    }

    override suspend fun getJson(url: String, userAgent: String?): AppNetworkResult<JSONObject> {
        val result = request(url, userAgent, ::text)
        return when (result) {
            is AppNetworkResult.Success -> runCatching { AppNetworkResult.Success(JSONObject(result.value)) }
                .getOrElse { AppNetworkResult.Failure(cause = it, message = "json parse") }
            is AppNetworkResult.Failure -> result
        }
    }

    override suspend fun getBytes(
        url: String,
        userAgent: String?,
        progress: ((Float) -> Unit)?,
    ): AppNetworkResult<ByteArray?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).get().apply {
                if (userAgent != null) header("User-Agent", userAgent)
            }.build()
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    Log.w(TAG, "GET bytes failed code=${response.code} message=${response.message} url=$url")
                    return@withContext AppNetworkResult.Failure(code = response.code, message = response.message)
                }
                val total = body.contentLength()
                var read = 0L
                val buffer = ByteArray(64 * 1024)
                val out = java.io.ByteArrayOutputStream()
                body.byteStream().use { input ->
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        out.write(buffer, 0, n)
                        read += n
                        if (progress != null && total > 0) {
                            progress((read.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
                if (total > 0 && read != total) {
                    Log.w(TAG, "GET bytes incomplete read=$read total=$total url=$url")
                    return@withContext AppNetworkResult.Failure(
                        message = "incomplete response: $read/$total bytes",
                    )
                }
                progress?.invoke(1f)
                AppNetworkResult.Success<ByteArray?>(out.toByteArray())
            }
        }.getOrElse { cause ->
            Log.w(TAG, "GET bytes exception url=$url", cause)
            AppNetworkResult.Failure(cause = cause)
        }
    }

    override suspend fun downloadTo(
        url: String,
        destination: File,
        userAgent: String?,
        progress: ((Float) -> Unit)?,
    ): AppNetworkResult<File> = withContext(Dispatchers.IO) {
        runCatching {
            destination.parentFile?.mkdirs()
            val request = Request.Builder().url(url).get().apply {
                if (userAgent != null) header("User-Agent", userAgent)
            }.build()
            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body
                if (!response.isSuccessful || body == null) {
                    destination.delete()
                    Log.w(TAG, "GET file failed code=${response.code} message=${response.message} url=$url")
                    return@withContext AppNetworkResult.Failure(code = response.code, message = response.message)
                }
                val total = body.contentLength()
                var read = 0L
                val buffer = ByteArray(64 * 1024)
                destination.outputStream().use { output ->
                    body.byteStream().use { input ->
                        while (true) {
                            val n = input.read(buffer)
                            if (n <= 0) break
                            output.write(buffer, 0, n)
                            read += n
                            if (progress != null && total > 0) {
                                progress((read.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
                if (total > 0 && read != total) {
                    destination.delete()
                    Log.w(TAG, "GET file incomplete read=$read total=$total url=$url")
                    return@withContext AppNetworkResult.Failure(
                        message = "incomplete response: $read/$total bytes",
                    )
                }
                progress?.invoke(1f)
                AppNetworkResult.Success(destination)
            }
        }.getOrElse { cause ->
            destination.delete()
            Log.w(TAG, "GET file exception url=$url", cause)
            AppNetworkResult.Failure(cause = cause)
        }
    }

    private suspend fun <T> request(
        url: String,
        userAgent: String?,
        onResponse: (okhttp3.Response, String?) -> AppNetworkResult<T>,
    ): AppNetworkResult<T> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).get().apply {
                if (userAgent != null) header("User-Agent", userAgent)
            }.build()
            okHttpClient.newCall(request).execute().use { onResponse(it, userAgent) }
        }.getOrElse { cause ->
            Log.w(TAG, "GET exception url=$url", cause)
            AppNetworkResult.Failure(cause = cause)
        }
    }

    private fun text(response: okhttp3.Response, ignored: String?): AppNetworkResult<String> {
        val body = response.body?.string()
        return if (!response.isSuccessful || body == null) {
            Log.w(TAG, "GET json/text failed code=${response.code} message=${response.message} url=${response.request.url}")
            AppNetworkResult.Failure(code = response.code, message = response.message)
        } else AppNetworkResult.Success(body)
    }
}
