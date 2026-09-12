package io.github.moxisuki.blockprint.cat.app.core.persistence

import androidx.compose.runtime.Immutable

@Immutable
data class McsAuthCookies(
    val uuid: String = "",
    val userAuth: String = "",
    val cfClearance: String = "",
    val nickname: String = "",
    val savedAt: Long = 0L,
) {
    val isLoggedIn: Boolean
        get() = userAuth.isNotBlank()

    fun toHeaderValue(): String = buildList {
        if (uuid.isNotBlank()) add("uuid=$uuid")
        if (userAuth.isNotBlank()) add("user_auth=$userAuth")
        if (cfClearance.isNotBlank()) add("cf_clearance=$cfClearance")
    }.joinToString("; ")
}
