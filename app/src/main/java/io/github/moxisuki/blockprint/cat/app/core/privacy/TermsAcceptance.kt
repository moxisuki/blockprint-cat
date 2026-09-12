package io.github.moxisuki.blockprint.cat.app.core.privacy

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the user's acceptance of the app terms and privacy disclosures.
 *
 * Bugly is initialized only after this flag is true.
 */
@Singleton
class TermsAcceptance @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun isAccepted(): Boolean = preferences.getBoolean(KEY_ACCEPTED, false)

    fun accept() {
        preferences.edit { putBoolean(KEY_ACCEPTED, true) }
    }

    fun reset() {
        preferences.edit { remove(KEY_ACCEPTED) }
    }

    private companion object {
        const val PREFERENCES_NAME = "terms_acceptance"
        const val KEY_ACCEPTED = "accepted"
    }
}
