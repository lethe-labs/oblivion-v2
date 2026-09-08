package oblivion.v2.core.deadman

import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import oblivion.v2.core.log.SecLog
import oblivion.v2.core.prefs.SecurePrefs

class DeadmanConfigStore(private val securePrefs: SecurePrefs) {
    private val prefs get() = securePrefs.prefs

    private val _config = MutableStateFlow(load())
    val config: StateFlow<DeadmanConfig> = _config.asStateFlow()

    fun load(): DeadmanConfig = DeadmanConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        intervalMs = prefs.getLong(KEY_INTERVAL_MS, DeadmanConfig.DEFAULT_INTERVAL_MS),
        lastCheckInMs = prefs.getLong(KEY_LAST_CHECKIN_MS, 0L),
    )

    fun setEnabled(value: Boolean) {
        try {
            prefs.edit {
                putBoolean(KEY_ENABLED, value)

                if (value) {
                    putLong(KEY_LAST_CHECKIN_MS, System.currentTimeMillis())
                }
            }
            _config.value = load()
        } catch (t: Throwable) {
            SecLog.e(TAG, "setEnabled threw", t)
        }
    }

    fun setIntervalMs(value: Long) {
        try {
            val coerced = value.coerceIn(
                DeadmanConfig.MIN_INTERVAL_MS,
                DeadmanConfig.MAX_INTERVAL_MS,
            )
            prefs.edit { putLong(KEY_INTERVAL_MS, coerced) }
            _config.value = load()
        } catch (t: Throwable) {
            SecLog.e(TAG, "setIntervalMs threw", t)
        }
    }

    fun touchCheckIn() {
        try {
            prefs.edit { putLong(KEY_LAST_CHECKIN_MS, System.currentTimeMillis()) }
            _config.value = load()
        } catch (t: Throwable) {
            SecLog.e(TAG, "touchCheckIn threw", t)
        }
    }

    companion object {
        private const val TAG = "DeadmanConfigStore"
        private const val KEY_ENABLED = "deadman.enabled"
        private const val KEY_INTERVAL_MS = "deadman.interval_ms"
        private const val KEY_LAST_CHECKIN_MS = "deadman.last_checkin_ms"
    }
}
