package io.github.moxisuki.blockprint.cat.ui.community

import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.stringResource
import io.github.moxisuki.blockprint.cat.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CmsBypassOverlay(onClose: () -> Unit) {
    val context = LocalContext.current
    var loaded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = {
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        loaded = true
                    }
                }
                loadUrl("https://www.creativemechanicserver.com/")
            }
        }, modifier = Modifier.fillMaxSize())

        // Loading indicator
        if (!loaded) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.cms_verify_in_progress))
            }
        }

        // Close button
        Button(
            onClick = {
                if (loaded) {
                    // Log cookies set by WebView
                    val cm = CookieManager.getInstance()
                    val cookies = cm.getCookie("https://www.creativemechanicserver.com") ?: "(none)"
                    android.util.Log.i("CmsBypass", "WebView cookies: $cookies")
                }
                onClose()
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) { Text(if (loaded) "验证完成，返回" else "跳过验证") }
    }
}
