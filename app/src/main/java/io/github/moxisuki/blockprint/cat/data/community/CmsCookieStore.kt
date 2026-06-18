package io.github.moxisuki.blockprint.cat.data.community

import android.content.Context
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CmsCookieStore @Inject constructor(private val context: Context) {
    private val mu = Any()
    private val store = mutableMapOf<String, String>()

    @Volatile
    private var hydrated = false

    fun ensureLoaded() {
        synchronized(mu) {
            if (hydrated) return
            val f = File(context.filesDir, FILE)
            if (f.exists()) {
                runCatching {
                    val obj = JSONObject(f.readText(Charsets.UTF_8))
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        store[k] = obj.optString(k)
                    }
                }
            }
            hydrated = true
        }
    }

    fun put(name: String, value: String) {
        synchronized(mu) {
            store[name] = value
        }
    }

    fun get(name: String): String? {
        synchronized(mu) {
            return store[name]
        }
    }

    /** Snapshot the current in-memory cookies as a stable list of (name, value) pairs. */
    fun cookiesSnapshot(): List<Pair<String, String>> {
        synchronized(mu) {
            return store.entries.map { it.key to it.value }
        }
    }

    fun headerValue(): String {
        synchronized(mu) {
            return store.entries.joinToString("; ") { "${it.key}=${it.value}" }
        }
    }

    fun saveToDisk() {
        synchronized(mu) {
            val obj = JSONObject()
            for ((k, v) in store) {
                obj.put(k, v)
            }
            File(context.filesDir, FILE).writeText(obj.toString(), Charsets.UTF_8)
        }
    }

    fun remove(name: String) {
        synchronized(mu) { store.remove(name) }
    }

    /** Sync cookies from WebView's CookieManager (for Cloudflare clearance). */
    fun importFromWebView(url: String) {
        val cm = android.webkit.CookieManager.getInstance() ?: return
        val cookies = cm.getCookie(url) ?: return
        android.util.Log.i("CmsCookieStore", "importFromWebView: ${cookies.take(200)}")
        synchronized(mu) {
            for (pair in cookies.split(";")) {
                val eq = pair.indexOf('=')
                if (eq > 0) store[pair.substring(0, eq).trim()] = pair.substring(eq + 1).trim()
            }
        }
        saveToDisk()
    }

    fun clear() {
        synchronized(mu) {
            store.clear()
            hydrated = true
        }
    }

    internal fun resetForTest() {
        synchronized(mu) {
            store.clear()
            hydrated = false
        }
    }

    companion object {
        private const val FILE = "cms_cookies.json"
    }
}
