package oblivion.v2.core.voice

import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import oblivion.v2.core.prefs.SecurePrefs

class VoiceKillConfigStore(private val securePrefs: SecurePrefs) {
    private val prefs get() = securePrefs.prefs

    private val _config = MutableStateFlow(loadInternal())
    val config: StateFlow<VoiceKillConfig> = _config.asStateFlow()

    fun load(): VoiceKillConfig = _config.value

    fun save(config: VoiceKillConfig) {
        prefs.edit {
            putBoolean(K_ENABLED, config.enabled)
            putString(K_PHRASE, config.phrase)
            putBoolean(K_STRICT, config.strict)
            putString(K_LANGUAGE, config.language)
        }
        _config.value = config
    }

    private fun loadInternal(): VoiceKillConfig = VoiceKillConfig(
        enabled = prefs.getBoolean(K_ENABLED, false),
        phrase = prefs.getString(K_PHRASE, "").orEmpty(),
        strict = prefs.getBoolean(K_STRICT, true),
        language = prefs.getString(K_LANGUAGE, VoiceKillConfig.LANG_FR)
            ?: VoiceKillConfig.LANG_FR,
    )

    private companion object {
        private const val K_ENABLED = "voice.enabled"
        private const val K_PHRASE = "voice.phrase"
        private const val K_STRICT = "voice.strict"
        private const val K_LANGUAGE = "voice.language"
    }
}
