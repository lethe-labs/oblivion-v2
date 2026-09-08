package oblivion.v2.feature.sms

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import oblivion.v2.core.sms.SmsKillConfig
import oblivion.v2.core.sms.SmsKillConfigStore
import oblivion.v2.core.sms.SmsMatcher
import oblivion.v2.core.wipe.WipeGateway
import javax.inject.Inject

@HiltViewModel
class SmsKillViewModel @Inject constructor(
    private val configStore: SmsKillConfigStore,
    private val wipeGateway: WipeGateway,
) : ViewModel() {
    val config: StateFlow<SmsKillConfig> = configStore.config

    fun setSenderNumber(value: String) = configStore.setSenderNumber(value)

    fun setKeyword(value: String) = configStore.setKeyword(value)

    fun setEnabled(value: Boolean) {
        val cfg = config.value
        if (value && cfg.keyword.length < SmsKillConfig.MIN_KEYWORD_LENGTH) return
        if (value && cfg.senderNumber.isBlank()) return
        configStore.setEnabled(value)
    }

    fun simulateSms(sender: String, body: String): Boolean {
        val cfg = configStore.load()
        if (!cfg.isReady()) return false

        val normalizedSender = SmsMatcher.normalize(sender)
        if (!SmsMatcher.senderMatches(normalizedSender, cfg.senderNumber)) return false
        if (!SmsMatcher.keywordMatches(body, cfg.keyword)) return false

        return true
    }
}
