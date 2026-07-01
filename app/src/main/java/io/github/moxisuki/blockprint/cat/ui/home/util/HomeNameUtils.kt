package io.github.moxisuki.blockprint.cat.ui.home.util

/**
 * True if [name] contains any character outside the "safe" set for WorldEdit
 * (Sponge) schematic files: ASCII letters, digits, underscore, dot, hyphen.
 * WorldEdit rejects anything else (Chinese characters, spaces, special
 * punctuation), so the upload step must warn the user before sending.
 */
internal fun hasUnsafeWorldEditChars(name: String): Boolean =
    !name.matches(Regex("^[A-Za-z0-9_.\\-]+$"))

/**
 * Produce a WorldEdit-safe filename by **removing** any character outside
 * the safe set (ASCII letters, digits, underscore, dot, hyphen). Strips
 * unsafe chars rather than replacing them with `_` — "樱花小屋.schem"
 * becomes "小屋.schem" (or just ".schem" if all leading chars are unsafe).
 * Also collapses runs of dots and trims leading/trailing dots so the result
 * is a valid filename stem, not just a prefix of dots.
 */
internal fun safeWorldEditName(name: String): String {
    val stripped = name.replace(Regex("[^A-Za-z0-9_.\\-]"), "")
    val collapsed = stripped.replace(Regex("\\.+"), ".")
    return collapsed.trim('.', '_', '-')
}