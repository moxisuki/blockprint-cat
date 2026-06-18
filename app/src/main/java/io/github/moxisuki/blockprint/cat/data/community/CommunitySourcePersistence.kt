package io.github.moxisuki.blockprint.cat.data.community

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunitySourcePersistence @Inject constructor(context: Context) {
    private val prefs =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    companion object {
        private const val PREF = "community_prefs"
        private const val KEY = "current_source"
    }

    fun load(): CommunitySource =
        prefs.getString(KEY, null)
            ?.let { runCatching { CommunitySource.valueOf(it) }.getOrNull() }
            ?: CommunitySource.MCS

    fun save(source: CommunitySource) {
        prefs.edit().putString(KEY, source.name).apply()
    }
}
