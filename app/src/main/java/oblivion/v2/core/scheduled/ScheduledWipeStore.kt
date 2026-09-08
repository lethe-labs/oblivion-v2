package oblivion.v2.core.scheduled

import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import oblivion.v2.core.log.SecLog
import oblivion.v2.core.prefs.SecurePrefs

class ScheduledWipeStore(private val securePrefs: SecurePrefs) {
    private val prefs get() = securePrefs.prefs

    private val _config = MutableStateFlow(load())
    val config: StateFlow<ScheduledWipeConfig> = _config.asStateFlow()

    fun load(): ScheduledWipeConfig = ScheduledWipeConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        wipeAtMs = prefs.getLong(KEY_WIPE_AT_MS, 0L),
    )

    fun saveArmed(wipeAtMs: Long) {
        try {
            prefs.edit {
                putBoolean(KEY_ENABLED, true)
                putLong(KEY_WIPE_AT_MS, wipeAtMs)
            }
            _config.value = load()
        } catch (t: Throwable) {
            SecLog.e(TAG, "saveArmed threw", t)
        }
    }

    fun clear() {
        try {
            prefs.edit {
                putBoolean(KEY_ENABLED, false)
                putLong(KEY_WIPE_AT_MS, 0L)
            }
            _config.value = load()
        } catch (t: Throwable) {
            SecLog.e(TAG, "clear threw", t)
        }
    }

    companion object {
        private const val TAG = "ScheduledWipeStore"
        private const val KEY_ENABLED = "scheduled.enabled"
        private const val KEY_WIPE_AT_MS = "scheduled.wipe_at_ms"
    }
}
