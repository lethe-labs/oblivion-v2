package oblivion.v2.core.decoy

import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import oblivion.v2.core.crypto.PinHasher
import oblivion.v2.core.log.SecLog
import oblivion.v2.core.prefs.SecurePrefs

class DecoyConfigStore(private val securePrefs: SecurePrefs) {
    private val prefs get() = securePrefs.prefs

    private val _config = MutableStateFlow(load())
    val config: StateFlow<DecoyConfig> = _config.asStateFlow()

    fun load(): DecoyConfig = DecoyConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        pinHash = prefs.getString(KEY_HASH, "").orEmpty(),
        pinSalt = prefs.getString(KEY_SALT, "").orEmpty(),
        pinLength = prefs.getInt(KEY_LENGTH, 0),
    )

    fun save(config: DecoyConfig) {
        try {
            prefs.edit {
                putBoolean(KEY_ENABLED, config.enabled)
                putString(KEY_HASH, config.pinHash)
                putString(KEY_SALT, config.pinSalt)
                putInt(KEY_LENGTH, config.pinLength)
            }
            _config.value = config
        } catch (t: Throwable) {
            SecLog.e(TAG, "save threw", t)
        }
    }

    fun setEnabled(value: Boolean) {
        save(_config.value.copy(enabled = value))
    }

    fun setDecoyPin(pin: String) {
        val current = _config.value
        val next = if (pin.isEmpty()) {
            current.copy(enabled = false, pinHash = "", pinSalt = "", pinLength = 0)
        } else {
            val salt = PinHasher.newSalt()
            current.copy(
                pinHash = PinHasher.hash(pin, salt),
                pinSalt = salt,
                pinLength = pin.length,
            )
        }
        save(next)
    }

    private companion object {
        private const val TAG = "DecoyConfigStore"
        private const val KEY_ENABLED = "decoy.enabled"
        private const val KEY_HASH = "decoy.pin_hash"
        private const val KEY_SALT = "decoy.pin_salt"
        private const val KEY_LENGTH = "decoy.pin_length"
    }
}
