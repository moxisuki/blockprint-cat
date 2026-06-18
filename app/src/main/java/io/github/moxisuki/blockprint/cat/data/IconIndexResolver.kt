package io.github.moxisuki.blockprint.cat.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class IconNamespaceInfo(
    val version: String,
    val iconsPath: String,
)

@Singleton
class IconIndexResolver @Inject constructor(
    private val client: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val namespaces = mutableMapOf<String, IconNamespaceInfo>()
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()
    private var loaded = false

    private var cacheFile: File? = null

    fun init(context: Context) {
        cacheFile = File(context.cacheDir, CACHE_FILE)
    }

    suspend fun ensureLoaded() {
        if (loaded) return
        val cached = loadFromCache()
        if (cached) {
            loaded = true
            _ready.value = true
            Log.i(TAG, "Icon index loaded from cache: ${namespaces.size} namespace(s) — ${namespaces.keys}")
        }
        scope.launch {
            refreshFromNetwork()
        }
    }

    private fun loadFromCache(): Boolean {
        val f = cacheFile ?: return false
        if (!f.exists()) return false
        return try {
            val body = f.readText()
            parseBody(body)
            Log.d(TAG, "Index cache hit: ${f.length()} bytes")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Index cache read failed: ${e.message}")
            f.delete()
            false
        }
    }

    private suspend fun refreshFromNetwork() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Refreshing icon index from $INDEX_URL")
            val request = Request.Builder().url(INDEX_URL).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext
            parseBody(body)
            cacheFile?.writeText(body)
            val wasReady = loaded
            loaded = true
            if (!wasReady) {
                _ready.value = true
            }
            Log.i(TAG, "Icon index refreshed: ${namespaces.size} namespace(s) — ${namespaces.keys}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh icon index: ${e.message}", e)
        }
    }

    private fun parseBody(body: String) {
        val root = JSONObject(body)
        val nsObj = root.optJSONObject("namespaces") ?: return
        for (key in nsObj.keys()) {
            val info = nsObj.getJSONObject(key)
            namespaces[key] = IconNamespaceInfo(
                version = info.optString("version", ""),
                iconsPath = info.optString("icons_path", ""),
            )
        }
    }

    fun isReady(): Boolean = loaded

    fun getIconUrl(blockName: String, itemSuffix: String = ""): String? {
        if (!loaded) {
            Log.w(TAG, "getIconUrl($blockName): index not loaded yet")
            return null
        }
        val colon = blockName.indexOf(':')
        if (colon < 0) {
            Log.w(TAG, "getIconUrl($blockName): no namespace separator")
            return null
        }
        val namespace = blockName.substring(0, colon)
        val itemId = blockName.substring(colon + 1)
        val info = namespaces[namespace] ?: run {
            Log.w(TAG, "getIconUrl($blockName): unknown namespace '$namespace'")
            return null
        }
        val path = info.iconsPath.removeSuffix("/")
        val url = "${NetworkConstants.CDN_BASE_URL}$path/$itemId$itemSuffix.png"
        Log.v(TAG, "getIconUrl($blockName, suffix='$itemSuffix') → $url")
        return url
    }

    companion object {
        private const val TAG = "IconIndexResolver"
        private const val INDEX_URL = "${NetworkConstants.CDN_BASE_URL}/icons_index.json"
        private const val CACHE_FILE = "icon_index_cache.json"
    }
}
