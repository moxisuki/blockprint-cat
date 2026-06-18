package io.github.moxisuki.blockprint.cat.data.settings

import android.content.Context
import androidx.core.content.edit

/**
 * 持久化使用条款接受状态。第一次进入 App 时强制阅读并同意。
 */
class TermsAcceptance(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAccepted(): Boolean = prefs.getBoolean(KEY_ACCEPTED, false)

    fun accept() {
        prefs.edit { putBoolean(KEY_ACCEPTED, true) }
    }

    fun reset() {
        prefs.edit { remove(KEY_ACCEPTED) }
    }

    companion object {
        private const val PREFS_NAME = "terms_acceptance"
        private const val KEY_ACCEPTED = "accepted"
    }
}
