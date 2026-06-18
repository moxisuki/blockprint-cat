package io.github.moxisuki.blockprint.cat.ui.util

import kotlin.math.ln
import kotlin.math.pow

fun formatNumber(n: Int): String = when {
    n < 1000 -> n.toString()
    n < 10000 -> "%.1fk".format(n / 1000.0)
    n < 1_000_000 -> "%.0fk".format(n / 1000.0)
    else -> "%.2fM".format(n / 1_000_000.0)
}
