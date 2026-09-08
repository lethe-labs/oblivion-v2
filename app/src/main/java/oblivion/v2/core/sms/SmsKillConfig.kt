package oblivion.v2.core.sms

data class SmsKillConfig(
    val enabled: Boolean = false,
    val senderNumber: String = "",
    val keyword: String = "",
) {
    fun isReady(): Boolean =
        enabled && senderNumber.isNotBlank() && keyword.length >= MIN_KEYWORD_LENGTH

    companion object {
        const val MIN_KEYWORD_LENGTH = 4
        const val MAX_KEYWORD_LENGTH = 50
    }
}
