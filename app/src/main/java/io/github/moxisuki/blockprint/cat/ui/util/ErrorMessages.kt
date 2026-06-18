package io.github.moxisuki.blockprint.cat.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.data.error.AppError

/** AppError → 本地化文案. 仅在 Composable 内调用. */
@Composable
fun appErrorMessage(e: AppError): String = when (e) {
    is AppError.Network -> stringResource(R.string.error_network)
    is AppError.Timeout -> stringResource(R.string.error_timeout)
    is AppError.CmsCloudflare -> stringResource(R.string.error_cms_cloudflare)
    is AppError.Http -> stringResource(R.string.error_http_code, e.code)
    is AppError.Unknown -> e.message ?: stringResource(R.string.error_unknown)
}

/** 非 Composable 上下文(协程/Effect)用. 接收 Activity Context 以读取 resources. */
fun appErrorMessage(context: Context, e: AppError): String = when (e) {
    is AppError.Network -> context.getString(R.string.error_network)
    is AppError.Timeout -> context.getString(R.string.error_timeout)
    is AppError.CmsCloudflare -> context.getString(R.string.error_cms_cloudflare)
    is AppError.Http -> context.getString(R.string.error_http_code, e.code)
    is AppError.Unknown -> e.message ?: context.getString(R.string.error_unknown)
}

/** 在 LaunchedEffect 等非-Composable 中可用 — 需要从外层 collect LocalContext. */
@Composable
fun rememberAppErrorResolver(): (AppError) -> String {
    val ctx = LocalContext.current
    return { e -> appErrorMessage(ctx, e) }
}

/** 通用 string resolver — LaunchedEffect / Dispatchers.IO 等非-Composable 上下文用. */
@Composable
fun rememberStringResolver(): (Int, String) -> String {
    val ctx = LocalContext.current
    return { resId, arg -> ctx.getString(resId, arg) }
}
