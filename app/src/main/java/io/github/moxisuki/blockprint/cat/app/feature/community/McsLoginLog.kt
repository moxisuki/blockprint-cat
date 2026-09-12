package io.github.moxisuki.blockprint.cat.app.feature.community

import io.github.moxisuki.blockprint.cat.app.core.persistence.McsAuthCookies

internal const val McsLoginLogTag = "McsLogin"

internal fun McsAuthCookies.toDebugSummary(): String =
    "uuid=${uuid.maskForLog()}, userAuth=${userAuth.maskForLog()}, " +
        "cfClearance=${cfClearance.maskForLog()}, nickname=${nickname.ifBlank { "-" }}"

internal fun String.maskForLog(): String =
    when {
        isBlank() -> "-"
        length <= 8 -> "${take(2)}***"
        else -> "${take(4)}***${takeLast(4)}"
    }
