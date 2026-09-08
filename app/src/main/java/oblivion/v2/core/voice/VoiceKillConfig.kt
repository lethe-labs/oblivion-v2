package oblivion.v2.core.voice

data class VoiceKillConfig(
    val enabled: Boolean = false,
    val phrase: String = "",
    val strict: Boolean = true,
    val language: String = LANG_FR,
) {
    fun isReady(): Boolean = enabled && phrase.isNotBlank()

    companion object {
        const val MIN_PHRASE_LENGTH = 6
        const val MAX_PHRASE_LENGTH = 80

        const val LANG_FR = "fr"
        const val LANG_EN = "en"
    }
}
