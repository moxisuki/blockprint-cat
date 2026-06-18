package io.github.moxisuki.blockprint.cat.data

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McschematicCookieStore @Inject constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS = "mcschematic_cookies"
        private const val K_UUID = "uuid"
        private const val K_USER_AUTH = "user_auth"
        private const val K_CF_CLEARANCE = "cf_clearance"
        private const val K_NICKNAME = "nickname"
        private const val K_SAVED_AT = "saved_at"
    }

    fun isLoggedIn(): Boolean = cookies().userAuth.isNotEmpty()

    data class Cookies(
        val uuid: String = "",
        val userAuth: String = "",
        val cfClearance: String = "",
    ) {
        fun toHeaderValue(): String = buildList {
            if (uuid.isNotEmpty()) add("uuid=$uuid")
            if (userAuth.isNotEmpty()) add("user_auth=$userAuth")
            if (cfClearance.isNotEmpty()) add("cf_clearance=$cfClearance")
        }.joinToString("; ")
    }

    fun cookies(): Cookies = Cookies(
        uuid = prefs.getString(K_UUID, "").orEmpty(),
        userAuth = prefs.getString(K_USER_AUTH, "").orEmpty(),
        cfClearance = prefs.getString(K_CF_CLEARANCE, "").orEmpty(),
    )

    fun save(uuid: String = "", userAuth: String = "", cfClearance: String = "") {
        prefs.edit().apply {
            if (uuid.isNotEmpty()) putString(K_UUID, uuid)
            if (userAuth.isNotEmpty()) putString(K_USER_AUTH, userAuth)
            if (cfClearance.isNotEmpty()) putString(K_CF_CLEARANCE, cfClearance)
            putLong(K_SAVED_AT, System.currentTimeMillis())
            apply()
        }
    }

    fun setNickname(name: String) {
        prefs.edit().putString(K_NICKNAME, name).apply()
    }

    fun nickname(): String = prefs.getString(K_NICKNAME, "").orEmpty()

    fun savedAt(): Long = prefs.getLong(K_SAVED_AT, 0L)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
