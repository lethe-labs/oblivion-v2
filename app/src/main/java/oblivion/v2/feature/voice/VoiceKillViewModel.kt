package oblivion.v2.feature.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import oblivion.v2.core.voice.VoiceKillConfig
import oblivion.v2.core.voice.VoiceKillConfigStore
import oblivion.v2.core.voice.VoiceKillService
import oblivion.v2.core.wipe.WipeGateway
import javax.inject.Inject

@HiltViewModel
class VoiceKillViewModel @Inject constructor(
    app: Application,
    private val store: VoiceKillConfigStore,
    private val wipeGateway: WipeGateway,
) : AndroidViewModel(app) {
    val config: StateFlow<VoiceKillConfig> = store.config

    fun isAdminActive(): Boolean = wipeGateway.isAdminActive()

    fun setEnabled(enabled: Boolean) {
        if (enabled && !wipeGateway.isAdminActive()) return

        if (enabled && config.value.phrase.isBlank()) return
        store.save(config.value.copy(enabled = enabled))
        val ctx = getApplication<Application>()
        if (enabled) VoiceKillService.start(ctx) else VoiceKillService.stop(ctx)
    }

    fun setPhrase(phrase: String) {
        val trimmed = phrase.trim()
        store.save(config.value.copy(phrase = trimmed))
    }

    fun setStrict(strict: Boolean) {
        store.save(config.value.copy(strict = strict))
    }

    fun setLanguage(language: String) {
        if (language == config.value.language) return
        store.save(config.value.copy(language = language))
    }
}
