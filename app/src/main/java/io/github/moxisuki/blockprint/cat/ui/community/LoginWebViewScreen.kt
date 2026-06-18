package io.github.moxisuki.blockprint.cat.ui.community

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.moxisuki.blockprint.cat.data.McschematicClient
import io.github.moxisuki.blockprint.cat.data.McschematicCookieStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

/**
 * QQ OAuth login via in-app WebView. Cookie capture flow:
 *   1. Open the QQ authorize URL. The WebView passes any Cloudflare
 *      challenge, setting `cf_clearance` in CookieManager.
 *   2. User authorizes; QQ redirects to mcschematic.top/login?code=…;
 *      the server sets `uuid` and `user_auth` cookies and redirects to /home/.
 *   3. We detect the /home/ navigation, read all three cookies back from
 *      CookieManager, and persist them via [McschematicCookieStore].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginWebViewScreen(
    onLoginSuccess: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LoginScreenEntryPoint::class.java
        )
    }
    val cookieStore = entryPoint.cookieStore()
    val client = entryPoint.mcschematicClient()

    var progress by remember { mutableStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // QQ OAuth authorize URL (from README §"QQ OAuth 授权流程")
    val authorizeUrl = "https://graph.qq.com/oauth2.0/authorize" +
        "?response_type=code" +
        "&client_id=102611417" +
        "&redirect_uri=" + java.net.URLEncoder.encode(
            "https://www.mcschematic.top/login", "UTF-8"
        ) +
        "&state=blockprintcat" +
        "&ptlang=2052" +
        "&display=pc"

    Column(modifier = Modifier.fillMaxSize()) {
        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        // 1) Override User-Agent to desktop Chrome — QQ's "oneKey" /
                        //    "ptlogin" JS path does device detection and uses different
                        //    (less-restricted) endpoints when it sees a desktop UA.
                        val desktopUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/120.0.0.0 Safari/537.36"
                        settings.userAgentString = desktopUa
                        // 2) Standard WebView enablement
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        settings.databaseEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        // 3) Critical: disable Google Safe Browsing. Safe Browsing's
                        //    heuristics on `qq-web.cdn-go.cn` resources return
                        //    `net::err_blocked_by_orb` for QQ's login JS chain.
                        //    API 26+ — guarded by SDK check.
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            settings.safeBrowsingEnabled = false
                        }
                        // 4) Allow mixed content — QQ sometimes loads http sub-resources
                        //    from its main https page.
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        // 5) Cookies — third-party cookies must be accepted so the
                        //    `user_auth` set on mcschematic.top is visible to the
                        //    `cf_clearance` host.
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: android.webkit.WebResourceRequest,
                            ): Boolean = false

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                handleNavigation(
                                    client = client,
                                    cookieStore = cookieStore,
                                    scope = scope,
                                    url = url,
                                    onProgress = { progress = it },
                                    onSuccess = {
                                        view.stopLoading()
                                        onLoginSuccess()
                                    },
                                )
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: android.webkit.WebResourceRequest,
                                error: android.webkit.WebResourceError,
                            ) {
                                // Sub-resource errors (scripts, images) fire frequently
                                // during QQ login. The login flow itself still works
                                // because the cookies get set on the main redirect.
                                // Silently log to logcat for debugging; no UI noise.
                                android.util.Log.w(
                                    "LoginWebView",
                                    "sub-resource error: ${error.description} ${request.url}",
                                )
                            }
                        }
                        loadUrl(authorizeUrl)
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }
}

/**
 * Inspect the URL the WebView just loaded.
 * On the post-login redirect to mcschematic.top/home/, we extract cookies
 * from CookieManager and persist them.
 */
private fun handleNavigation(
    client: McschematicClient,
    cookieStore: McschematicCookieStore,
    scope: kotlinx.coroutines.CoroutineScope,
    url: String,
    onProgress: (Int) -> Unit,
    onSuccess: () -> Unit,
) {
    onProgress(
        when {
            url.startsWith("https://graph.qq.com") -> 30
            url.contains("mcschematic.top/login") -> 60
            url.contains("mcschematic.top") -> 80
            else -> 50
        }
    )

    if (url.contains("mcschematic.top") && !url.contains("/login?")) {
        scope.launch {
            try {
                val cm = CookieManager.getInstance()
                cm.flush()
                val cookies = cm.getCookie(url).orEmpty()
                val cfCookie = cm.getCookie("https://www.mcschematic.top").orEmpty()
                val all = listOf(cookies, cfCookie)
                    .filter { it.isNotEmpty() }
                    .joinToString("; ")

                val uuid = extractCookie(all, "uuid")
                val userAuth = extractCookie(all, "user_auth")
                val cfClearance = extractCookie(all, "cf_clearance")

                if (userAuth.isNotEmpty()) {
                    onProgress(100)
                    cookieStore.save(
                        uuid = uuid,
                        userAuth = userAuth,
                        cfClearance = cfClearance,
                    )
                    val status = runCatching { client.loginStatus() }.getOrNull()
                    if (status != null && status.uuid.isNotEmpty()) {
                        cookieStore.save(
                            uuid = status.uuid,
                            userAuth = userAuth,
                            cfClearance = cfClearance,
                        )
                    }
                    onSuccess()
                }
                // If userAuth is empty, do nothing — likely the server hasn't
                // set the cookie yet, or login failed. The MainActivity snackbar
                // will show whatever McschematicClient throws on the next call.
            } catch (e: Exception) {
                android.util.Log.w("LoginWebView", "cookie capture failed: ${e.message}")
            }
        }
    }
}

private fun extractCookie(cookieHeader: String, name: String): String {
    if (cookieHeader.isEmpty()) return ""
    return cookieHeader.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("$name=") }
        ?.removePrefix("$name=")
        ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
        ?: ""
}

/**
 * Proxy a QQ cross-origin sub-resource through HttpURLConnection and
 * return a WebResourceResponse with CORS headers. This bypasses
 * Chromium's Opaque Response Blocking (`net::err_blocked_by_orb`).
 *
 * Constraints:
 *  - **Main-frame requests are NOT proxied** — they're the page
 *    navigation itself; ORB doesn't apply and proxying would break
 *    the redirect → cookie capture flow on mcschematic.top.
 *  - **Only HTTP/HTTPS schemes** — skip `data:`, `blob:`, `chrome:`, etc.
 *  - **Only GET requests** — POST/PUT can't be proxied because
 *    `WebResourceRequest` doesn't expose the body, so attempting
 *    would silently drop the body and trigger server errors that
 *    show up as `err_connection_refused` to the user.
 *  - **Only QQ domains** — mcschematic.top and everything else
 *    pass through normally.
 *  - **Any failure falls back to null** — the WebView then makes
 *    the request itself.
 */
private fun proxyRequest(request: WebResourceRequest): WebResourceResponse? {
    val urlStr = request.url.toString()
    val scheme = request.url.scheme?.lowercase()
    val isMainFrame = request.isForMainFrame
    val isGet = request.method.equals("GET", ignoreCase = true) ||
        request.method.equals("OPTIONS", ignoreCase = true)

    val shouldProxy = !isMainFrame &&
        isGet &&
        (scheme == "http" || scheme == "https") &&
        (
            urlStr.contains("qq.com") ||
                urlStr.contains("qq-web.cdn-go.cn") ||
                urlStr.contains("gtimg.cn") ||
                urlStr.contains("qzone.qq.com") ||
                urlStr.contains("qpic.cn")
            )
    if (!shouldProxy) {
        return null
    }
    android.util.Log.d("LoginWebView", "proxy: $urlStr method=${request.method}")

    return try {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = request.method
            connectTimeout = 15_000
            readTimeout = 30_000
            // Don't follow redirects ourselves — let the WebView handle them
            // so the navigation history stays correct.
            instanceFollowRedirects = false
            setRequestProperty(
                "User-Agent",
                request.requestHeaders["User-Agent"]
                    ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            )
            setRequestProperty(
                "Accept",
                request.requestHeaders["Accept"] ?: "application/json, text/plain, */*",
            )
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
            // Forward cookies from CookieManager
            val cookie = CookieManager.getInstance().getCookie(urlStr)
            if (!cookie.isNullOrEmpty()) {
                setRequestProperty("Cookie", cookie)
            }
            // Forward Origin / Referer for QQ's CSRF checks
            request.requestHeaders["Origin"]?.let { setRequestProperty("Origin", it) }
            request.requestHeaders["Referer"]?.let { setRequestProperty("Referer", it) }
        }
        val code = conn.responseCode
        // Handle redirects by returning null so the WebView re-navigates
        if (code in 300..399) {
            conn.disconnect()
            android.util.Log.d("LoginWebView", "proxy: $urlStr → redirect $code, letting WebView handle")
            return null
        }
        val contentType = conn.contentType ?: "application/octet-stream"
        val mime = contentType.substringBefore(";").trim().ifEmpty { "application/octet-stream" }
        val charset = contentType.substringAfter("charset=", "").trim().ifEmpty { "utf-8" }
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        if (stream == null) {
            conn.disconnect()
            return null
        }

        // Build response with CORS headers — this is what unblocks ORB.
        val headers = mutableMapOf<String, String>()
        conn.headerFields.filter { it.key != null }
            .forEach { (k, v) -> if (v != null) headers[k] = v.joinToString(", ") }
        headers["Access-Control-Allow-Origin"] = "*"
        headers["Access-Control-Allow-Credentials"] = "true"
        headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
        headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization"

        WebResourceResponse(mime, charset, stream).apply {
            responseHeaders = headers
        }
    } catch (e: Exception) {
        android.util.Log.w("LoginWebView", "proxyRequest failed for $urlStr: ${e.message}")
        null
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LoginScreenEntryPoint {
    fun cookieStore(): McschematicCookieStore
    fun mcschematicClient(): McschematicClient
}
