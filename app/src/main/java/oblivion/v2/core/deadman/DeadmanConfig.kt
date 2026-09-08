package oblivion.v2.core.deadman

import java.util.concurrent.TimeUnit

data class DeadmanConfig(
    val enabled: Boolean = false,
    val intervalMs: Long = DEFAULT_INTERVAL_MS,
    val lastCheckInMs: Long = 0L,
) {
    fun isReady(): Boolean = enabled && lastCheckInMs > 0 && intervalMs >= MIN_INTERVAL_MS

    fun remainingMs(nowMs: Long = System.currentTimeMillis()): Long =
        if (lastCheckInMs == 0L) intervalMs else (lastCheckInMs + intervalMs) - nowMs

    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean =
        isReady() && remainingMs(nowMs) <= 0

    companion object {
        val MIN_INTERVAL_MS: Long = TimeUnit.HOURS.toMillis(1)

        val MAX_INTERVAL_MS: Long = TimeUnit.DAYS.toMillis(60)

        val DEFAULT_INTERVAL_MS: Long = TimeUnit.HOURS.toMillis(48)
    }
}
