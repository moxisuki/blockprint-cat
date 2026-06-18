package io.github.moxisuki.blockprint.cat.data.vanilla

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONObject
import java.io.File

/** Cached lang file loader for material display names. */
object LangManager {
    private val cache = mutableMapOf<String, JSONObject>()

    fun displayName(context: Context, blockId: String, localeHint: String? = null): String {
        val colon = blockId.indexOf(':')
        if (colon < 0) return blockId
        val ns = blockId.substring(0, colon)
        val key = blockId.substring(colon + 1)

        // 优先: localeHint > App locale > 默认 zh_cn → fallback en_us → raw
        val primary = localeHint ?: when {
            AppCompatDelegate.getApplicationLocales().toLanguageTags().startsWith("en") -> "en_us"
            else -> "zh_cn"
        }
        val fallback = if (primary == "zh_cn") "en_us" else "zh_cn"
        val p = loadLang(context, ns, primary)
        val f = loadLang(context, ns, fallback)
        return resolveFrom(p, ns, key)
            ?: resolveFrom(f, ns, key)
            ?: blockId
    }

    private fun resolveFrom(lang: JSONObject?, ns: String, key: String): String? {
        if (lang == null) return null
        return lang.optString("block.$ns.$key", null)
            ?: lang.optString("item.$ns.$key", null)
            ?: lang.optString("block.minecraft.$key", null)
            ?: lang.optString("item.minecraft.$key", null)
            ?: lang.optString(key, null)
            ?.replace(Regex("§[0-9a-fk-or]"), "")
    }

    fun clear() { cache.clear() }

    private fun loadLang(context: Context, namespace: String, locale: String): JSONObject? {
        val cacheKey = "$namespace:$locale"
        cache[cacheKey]?.let { return it }
        val file = File(context.filesDir, "blockprintcat/render_assets/$namespace/lang/$locale.json")
        if (!file.isFile) return null
        return try {
            JSONObject(file.readText()).also { cache[cacheKey] = it }
        } catch (_: Exception) { null }
    }
}
