package oblivion.v2.core.scheduled

data class ScheduledWipeConfig(
    val enabled: Boolean = false,
    val wipeAtMs: Long = 0L,
) {
    fun isArmed(nowMs: Long = System.currentTimeMillis()): Boolean =
        enabled && wipeAtMs > nowMs

    fun remainingMs(nowMs: Long = System.currentTimeMillis()): Long =
        if (!enabled) 0L else wipeAtMs - nowMs
}
