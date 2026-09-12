package io.github.moxisuki.blockprint.cat.app.core.resourcepack

import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object LangReader {
    private val cache = ConcurrentHashMap<String, JSONObject>()

    /**
     * Read-only logic for MaterialRow's display-name lookup.
     * Returns the raw block id when neither locale can resolve it.
     */
    fun chooseDisplayName(
        primaryJson: String?,
        fallbackJson: String?,
        blockId: String,
        primaryLocale: String,
        fallbackLocale: String,
    ): String {
        val colon = blockId.indexOf(':')
        if (colon < 0) return blockId
        val ns = blockId.substring(0, colon)
        val key = blockId.substring(colon + 1)

        val primary = resolveFrom(runCatching { JSONObject(primaryJson ?: "{}") }.getOrNull(), ns, key)
            ?: resolveFrom(runCatching { JSONObject(fallbackJson ?: "{}") }.getOrNull(), ns, key)
        return primary?.replace(Regex("§[0-9a-fk-or]"), "")
            ?: blockId
    }

    /** Resolves the first matching translation from the supplied locale files. */
    fun chooseDisplayName(
        localizedJsons: List<String?>,
        blockId: String,
    ): String? {
        val colon = blockId.indexOf(':')
        if (colon <= 0 || colon == blockId.lastIndex) return null
        val namespace = blockId.substring(0, colon)
        val key = blockId.substring(colon + 1)
        return localizedJsons
            .asSequence()
            .mapNotNull { json ->
                resolveFrom(
                    runCatching { JSONObject(json ?: "{}") }.getOrNull(),
                    namespace,
                    key,
                )
            }
            .map { it.replace(Regex("§[0-9a-fk-or]"), "") }
            .firstOrNull()
    }

    fun chooseDisplayNameFromObjects(
        localizedJsons: List<JSONObject?>,
        blockId: String,
    ): String? {
        val colon = blockId.indexOf(':')
        if (colon <= 0 || colon == blockId.lastIndex) return null
        val namespace = blockId.substring(0, colon)
        val key = blockId.substring(colon + 1)
        return localizedJsons
            .asSequence()
            .mapNotNull { lang -> resolveFrom(lang, namespace, key) }
            .map { it.replace(Regex("§[0-9a-fk-or]"), "") }
            .firstOrNull()
    }

    /**
     * Loads a lang JSON from disk and returns its content. Caches per (namespace, locale).
     */
    fun loadLang(assetsRoot: File, namespace: String, locale: String): JSONObject? {
        val key = "$namespace:$locale"
        cache[key]?.let { return it }
        val file = File(assetsRoot, "$namespace/lang/$locale.json")
        if (!file.isFile) return null
        return runCatching {
            JSONObject(file.readText()).also { cache[key] = it }
        }.getOrNull()
    }

    fun clearCache() { cache.clear() }

    private fun resolveFrom(lang: JSONObject?, ns: String, key: String): String? {
        if (lang == null) return null
        val candidates = listOf(
            "block.$ns.$key",
            "item.$ns.$key",
            "block.minecraft.$key",
            "item.minecraft.$key",
            key,
        )
        return candidates.firstNotNullOfOrNull { candidate ->
            lang.optString(candidate).takeIf { it.isNotBlank() }
        }
    }
}
