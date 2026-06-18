package io.github.moxisuki.blockprint.cat.data.bridge

/**
 * Parsed QR connection payload from a `blockprintcat://` URI.
 * Mirrors the format produced by BlockPrint Link's QrHudRenderer.
 */
data class QrConnection(
    val host: String,
    val port: Int,
    val token: String,
)

private val QR_URI_REGEX = Regex(
    """^blockprintcat://([^/:]+):(\d+)/ws\?token=(.+)$"""
)

/**
 * Returns null if the raw text is not a valid `blockprintcat://` connection URI.
 * Trims whitespace, validates host non-blank, port in 1..65535, token non-blank.
 */
fun parseQrPayload(raw: String): QrConnection? {
    val m = QR_URI_REGEX.matchEntire(raw.trim()) ?: return null
    val host = m.groupValues[1]
    val port = m.groupValues[2].toIntOrNull() ?: return null
    val token = m.groupValues[3]
    if (host.isBlank() || port !in 1..65535 || token.isBlank()) return null
    return QrConnection(host, port, token)
}