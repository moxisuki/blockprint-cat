package io.github.moxisuki.blockprint.cat.app.core.data.blueprint

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.moxisuki.blockprint.cat.app.core.network.AppHttpClient
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkConstants
import io.github.moxisuki.blockprint.cat.app.core.network.AppNetworkResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class IconNamespaceInfo(
    val version: String,
    val iconsPath: String,
)

@Singleton
class IconIndexResolver @Inject constructor(
    @ApplicationContext context: Context,
    private val httpClient: AppHttpClient,
) {
    private val cacheFile = File(context.cacheDir, CACHE_FILE)
    private val loadMutex = Mutex()
    private val namespaces = mutableMapOf<String, IconNamespaceInfo>()
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    @Volatile
    private var loaded = false

    suspend fun ensureLoaded() {
        if (loaded) return

        loadMutex.withLock {
            if (loaded) return@withLock
            withContext(Dispatchers.IO) {
                loadFromCache()
                refreshFromNetwork()
            }
        }
    }

    fun getIconUrls(blockId: String): List<String> =
        listOfNotNull(
            getIconUrl(blockId),
            getIconUrl(blockId, "_block"),
            getIconUrl(blockId, "_item"),
        )

    suspend fun invalidateCache() {
        loadMutex.withLock {
            withContext(Dispatchers.IO) {
                namespaces.clear()
                loaded = false
                _ready.value = false
                cacheFile.delete()
            }
        }
    }

    private fun loadFromCache() {
        if (!cacheFile.exists()) return
        runCatching {
            parseBody(cacheFile.readText())
            loaded = true
            _ready.value = true
        }.onFailure {
            cacheFile.delete()
        }
    }

    private suspend fun refreshFromNetwork() {
        when (val result = httpClient.getString(INDEX_URL)) {
            is AppNetworkResult.Success -> {
                parseBody(result.value)
                cacheFile.writeText(result.value)
                loaded = true
                _ready.value = true
            }
            is AppNetworkResult.Failure -> Unit
        }
    }

    private fun parseBody(body: String) {
        val root = JSONObject(body)
        val namespaceObject = root.optJSONObject("namespaces") ?: return
        for (key in namespaceObject.keys()) {
            val item = namespaceObject.getJSONObject(key)
            namespaces[key] = IconNamespaceInfo(
                version = item.optString("version", ""),
                iconsPath = item.optString("icons_path", ""),
            )
        }
    }

    private fun getIconUrl(blockId: String, suffix: String = ""): String? {
        val separatorIndex = blockId.indexOf(':')
        if (!loaded || separatorIndex <= 0) return null

        val namespace = blockId.substring(0, separatorIndex)
        val itemId = blockId.substring(separatorIndex + 1)
        val namespaceInfo = namespaces[namespace] ?: return null
        val path = namespaceInfo.iconsPath.removeSuffix("/")
        return "${AppNetworkConstants.CDN_BASE_URL}$path/$itemId$suffix.png"
    }

    private companion object {
        const val INDEX_URL = "${AppNetworkConstants.CDN_BASE_URL}/icons_index.json"
        const val CACHE_FILE = "icon_index_cache.json"
    }
}
