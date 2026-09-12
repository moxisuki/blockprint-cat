package io.github.moxisuki.blockprint.cat.app.core.resourcepack.internal

/** Keeps extraction/download callbacks from turning every IO chunk into a UI recomposition. */
internal class PackProgressThrottle(
    private val minIntervalNanos: Long = 80_000_000L,
    private val minFractionDelta: Float = 0.01f,
) {
    private var lastEmitNanos = 0L
    private var lastFraction = -1f

    fun allow(fraction: Float?, force: Boolean = false): Boolean {
        if (force) {
            lastEmitNanos = System.nanoTime()
            lastFraction = fraction ?: -1f
            return true
        }
        val now = System.nanoTime()
        val value = fraction ?: -1f
        if (lastEmitNanos == 0L ||
            now - lastEmitNanos >= minIntervalNanos ||
            (value >= 0f && value - lastFraction >= minFractionDelta)
        ) {
            lastEmitNanos = now
            lastFraction = value
            return true
        }
        return false
    }
}
