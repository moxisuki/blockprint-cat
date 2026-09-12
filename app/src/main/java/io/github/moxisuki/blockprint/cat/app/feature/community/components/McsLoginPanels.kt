package io.github.moxisuki.blockprint.cat.app.feature.community.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.moxisuki.blockprint.cat.R
import io.github.moxisuki.blockprint.cat.app.core.persistence.McsAuthCookies
import io.github.moxisuki.blockprint.cat.app.feature.community.CommunityLoginState
import io.github.moxisuki.blockprint.cat.app.feature.community.McsLoginLogTag
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsBaseUrl
import io.github.moxisuki.blockprint.cat.app.feature.community.data.McsBrowserUserAgent
import io.github.moxisuki.blockprint.cat.app.feature.community.toDebugSummary
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
internal fun McsLoginRequiredPanel(
    isCheckingLogin: Boolean,
    errorMessage: String?,
    onLoginClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 340.dp),
            insideMargin = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(
                text = "登录 MCS",
                color = MiuixTheme.colorScheme.onSurfaceContainer,
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "登录后浏览 mcschematic.top 社区蓝图。",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                style = MiuixTheme.textStyles.body2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    color = MiuixTheme.colorScheme.error,
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLoginClick,
                enabled = !isCheckingLogin,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                if (isCheckingLogin) {
                    CircularProgressIndicator(
                        size = 18.dp,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "QQ 登录",
                        color = MiuixTheme.colorScheme.onPrimary,
                        style = MiuixTheme.textStyles.button,
                        maxLines = 1,
                    )
                }
            }
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "重试",
                    onClick = onRetryClick,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun McsLoginWebViewScreen(
    state: CommunityLoginState,
    onCookiesCaptured: (McsAuthCookies) -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var webViewState by rememberSaveable { mutableStateOf<Bundle?>(null) }
    var lastCandidateKey by rememberSaveable { mutableStateOf("") }
    val currentOnCookiesCaptured by rememberUpdatedState(onCookiesCaptured)
    val currentState by rememberUpdatedState(state)
    val authorizeUrl = remember { mcsQqAuthorizeUrl() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(state.isVerifying, state.errorMessage) {
        Log.d(
            McsLoginLogTag,
            "Login UI state changed: verifying=${state.isVerifying}, error=${state.errorMessage.orEmpty().ifBlank { "-" }}",
        )
    }

    fun saveWebViewState() {
        webView?.let { currentWebView ->
            webViewState = Bundle().also { bundle ->
                currentWebView.saveState(bundle)
            }
            Log.d(
                McsLoginLogTag,
                "WebView state saved: url=${currentWebView.url.orEmpty().sanitizeUrlForLog()}, progress=${currentWebView.progress}",
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface),
    ) {
        if (progress in 1..99 || state.isVerifying) {
            LinearProgressIndicator(
                progress = if (state.isVerifying) 1f else progress / 100f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        LoginStatusStrip(
            isVerifying = state.isVerifying,
            errorMessage = state.errorMessage,
        )
        LoginScanHintCard()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        Log.d(McsLoginLogTag, "WebView factory: create login WebView")
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.userAgentString = McsBrowserUserAgent
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            settings.safeBrowsingEnabled = false
                        }
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                if (newProgress == 100 || newProgress % 25 == 0) {
                                    Log.d(
                                        McsLoginLogTag,
                                        "WebView progress: progress=$newProgress, url=${view.url.orEmpty().sanitizeUrlForLog()}",
                                    )
                                }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                Log.d(McsLoginLogTag, "WebView page started: ${url.sanitizeUrlForLog()}")
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                progress = loginProgressFor(url)
                                Log.d(
                                    McsLoginLogTag,
                                    "WebView page finished: url=${url.sanitizeUrlForLog()}, mappedProgress=$progress",
                                )
                                val cookies = readMcsAuthCandidate(url)
                                val candidateKey = "${cookies.uuid}:${cookies.userAuth}"
                                Log.d(
                                    McsLoginLogTag,
                                    "Auth candidate read: eligible=${cookies.isLoggedIn}, summary=${cookies.toDebugSummary()}, duplicated=${candidateKey == lastCandidateKey}, verifying=${currentState.isVerifying}",
                                )
                                if (!currentState.isVerifying &&
                                    cookies.isLoggedIn &&
                                    candidateKey != lastCandidateKey
                                ) {
                                    lastCandidateKey = candidateKey
                                    progress = 100
                                    Log.d(McsLoginLogTag, "Auth candidate emitted to ViewModel")
                                    currentOnCookiesCaptured(cookies)
                                }
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                super.onReceivedError(view, request, error)
                                Log.w(
                                    McsLoginLogTag,
                                    "WebView error: mainFrame=${request.isForMainFrame}, code=${error.errorCode}, desc=${error.description}, url=${request.url.toString().sanitizeUrlForLog()}",
                                )
                            }
                        }
                        val restored = webViewState?.let { restoreState(it) }
                        if (restored == null) {
                            Log.d(McsLoginLogTag, "WebView initial load: ${authorizeUrl.sanitizeUrlForLog()}")
                            loadUrl(authorizeUrl)
                        } else {
                            Log.d(McsLoginLogTag, "WebView state restored: entries=$restored")
                        }
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            Log.d(McsLoginLogTag, "Login lifecycle event: $event")
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webView?.onResume()
                    webView?.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    webView?.onPause()
                }
                Lifecycle.Event.ON_STOP -> {
                    saveWebViewState()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(McsLoginLogTag, "Login WebView dispose")
            saveWebViewState()
            webView?.destroy()
            webView = null
        }
    }
}

@Composable
private fun LoginStatusStrip(
    isVerifying: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    if (!isVerifying && errorMessage.isNullOrBlank()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = if (isVerifying) {
                "正在校验 MCS 登录状态"
            } else {
                errorMessage.orEmpty()
            },
            color = if (isVerifying) {
                MiuixTheme.colorScheme.onSurfaceContainer
            } else {
                MiuixTheme.colorScheme.error
            },
            style = MiuixTheme.textStyles.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun mcsQqAuthorizeUrl(): String =
    "https://graph.qq.com/oauth2.0/authorize" +
        "?response_type=code" +
        "&client_id=102611417" +
        "&redirect_uri=" + URLEncoder.encode("$McsBaseUrl/login", "UTF-8") +
        "&state=blockprintcat" +
        "&ptlang=2052" +
        "&display=pc"

private fun loginProgressFor(url: String): Int = when {
    url.startsWith("https://graph.qq.com") -> 30
    url.contains("mcschematic.top/login") -> 62
    url.contains("mcschematic.top") -> 84
    else -> 48
}

@Composable
private fun LoginScanHintCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.community_login_scan_hint_title),
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.community_login_scan_hint_summary),
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.body2,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun readMcsAuthCandidate(url: String): McsAuthCookies {
    val uri = runCatching { Uri.parse(url) }.getOrNull()
        ?: run {
            Log.d(McsLoginLogTag, "Auth candidate skipped: invalid url=${url.sanitizeUrlForLog()}")
            return McsAuthCookies()
        }
    val host = uri.host.orEmpty()
    val path = uri.path.orEmpty()
    if (!host.endsWith("mcschematic.top") ||
        path.startsWith("/login") ||
        !isMcsAuthenticatedPage(path)
    ) {
        Log.d(
            McsLoginLogTag,
            "Auth candidate skipped: host=$host, path=$path, url=${url.sanitizeUrlForLog()}",
        )
        return McsAuthCookies()
    }
    val cookieManager = CookieManager.getInstance()
    cookieManager.flush()
    val allCookies = listOf(
        cookieManager.getCookie(url).orEmpty(),
        cookieManager.getCookie(McsBaseUrl).orEmpty(),
    ).filter { it.isNotBlank() }
        .joinToString("; ")
    Log.d(
        McsLoginLogTag,
        "MCS cookie header captured: cookieCount=${allCookies.cookieCountForLog()}, sourceUrl=${url.sanitizeUrlForLog()}",
    )

    return McsAuthCookies(
        uuid = extractCookie(allCookies, "uuid"),
        userAuth = extractCookie(allCookies, "user_auth"),
        cfClearance = extractCookie(allCookies, "cf_clearance"),
    )
}

private fun isMcsAuthenticatedPage(path: String): Boolean =
    path.startsWith("/home")

private fun extractCookie(cookieHeader: String, name: String): String =
    cookieHeader.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("$name=") }
        ?.removePrefix("$name=")
        ?.let { URLDecoder.decode(it, "UTF-8") }
        .orEmpty()

private fun String.cookieCountForLog(): Int =
    split(";").count { it.trim().contains("=") }

private fun String.sanitizeUrlForLog(): String =
    runCatching {
        val uri = Uri.parse(this)
        buildString {
            append(uri.scheme.orEmpty())
            append("://")
            append(uri.host.orEmpty())
            if (!uri.path.isNullOrBlank()) append(uri.path)
            val queryNames = uri.queryParameterNames
            if (queryNames.isNotEmpty()) {
                append("?")
                append(queryNames.joinToString("&") { "$it=***" })
            }
        }
    }.getOrDefault(this.substringBefore("?"))
