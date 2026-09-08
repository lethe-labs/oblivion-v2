package oblivion.v2.core.sms

import oblivion.v2.core.log.SecLog
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import oblivion.v2.core.prefs.SecurePrefs

class SmsKillConfigStore(private val securePrefs: SecurePrefs) {
    private val prefs get() = securePrefs.prefs

    private val _config = MutableStateFlow(load())
    val config: StateFlow<SmsKillConfig> = _config.asStateFlow()

    fun load(): SmsKillConfig = SmsKillConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        senderNumber = prefs.getString(KEY_SENDER, "") ?: "",
        keyword = prefs.getString(KEY_KEYWORD, "") ?: "",
    )

    fun setEnabled(value: Boolean) {
        try {
            prefs.edit { putBoolean(KEY_ENABLED, value) }
            _config.value = load()
        } catch (t: Throwable) {
            SecLog.e(TAG, "setEnabled threw", t)
        }
    }

    fun setSenderNumber(value: String) {
        try {
            prefs.edit { putString(KEY_SENDER, value.trim()) }
            _config.value = load()
        } catch (t: Throwable) {
            SecLog.e(TAG, "setSenderNumber threw", t)
        }
    }

    fun setKeyword(value: String) {
        try {
            prefs.edit { putString(KEY_KEYWORD, value.trim()) }
            _config.value = load()
        } catch (t: Throwable) {
            SecLog.e(TAG, "setKeyword threw", t)
        }
    }

    companion object {
        private const val TAG = "SmsKillConfigStore"
        private const val KEY_ENABLED = "sms_kill_enabled"
        private const val KEY_SENDER = "sms_kill_sender"
        private const val KEY_KEYWORD = "sms_kill_keyword"
    }
}
