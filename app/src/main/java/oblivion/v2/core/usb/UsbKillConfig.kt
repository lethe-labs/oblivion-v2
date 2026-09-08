package oblivion.v2.core.usb

data class UsbKillConfig(
    val enabled: Boolean = false,
    val graceSeconds: Int = DEFAULT_GRACE_SECONDS,
) {
    fun isValid(): Boolean = graceSeconds in MIN_GRACE_SECONDS..MAX_GRACE_SECONDS

    companion object {
        const val MIN_GRACE_SECONDS = 0
        const val MAX_GRACE_SECONDS = 30
        const val DEFAULT_GRACE_SECONDS = 5
    }
}
