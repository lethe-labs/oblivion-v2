package oblivion.v2.core.decoy

data class DecoyConfig(
    val enabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val pinLength: Int = 0,
) {
    fun isReady(): Boolean =
        enabled && pinHash.isNotEmpty() && pinSalt.isNotEmpty()

    fun isConfigured(): Boolean =
        pinHash.isNotEmpty() && pinSalt.isNotEmpty()

    companion object {
        const val MIN_PIN_LENGTH: Int = 4
    }
}
