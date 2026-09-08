package oblivion.v2.core.guard

import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import oblivion.v2.core.crypto.PinHasher
import oblivion.v2.core.prefs.SecurePrefs

class GuardConfigStore(private val securePrefs: SecurePrefs) {
    private val prefs get() = securePrefs.prefs

    private val _config = MutableStateFlow(loadInternal())
    val config: StateFlow<GuardConfig> = _config.asStateFlow()

    fun load(): GuardConfig = _config.value

    fun save(config: GuardConfig) {
        prefs.edit {
            putBoolean(K_MASTER_ENABLED, config.masterEnabled)

            putBoolean(K_TYPE_A_ENABLED, config.typeAEnabled)
            putString(K_TYPE_A_HASH, config.typeAHash)
            putString(K_TYPE_A_SALT, config.typeASalt)
            putInt(K_TYPE_A_LENGTH, config.typeALength)

            putBoolean(K_TYPE_B_ENABLED, config.typeBEnabled)
            putInt(K_TYPE_B_LENGTH, config.typeBLength)

            putBoolean(K_EMERGENCY_ENABLED, config.emergencyEnabled)
            putString(K_EMERGENCY_HASH, config.emergencyHash)
            putString(K_EMERGENCY_SALT, config.emergencySalt)
            putInt(K_EMERGENCY_LENGTH, config.emergencyLength)

            putBoolean(K_FAILED_ATTEMPTS_ENABLED, config.failedAttemptsEnabled)
            putInt(K_FAILED_ATTEMPTS_THRESHOLD, config.failedAttemptsThreshold)
        }
        _config.value = config
    }

    fun setTypeAPin(pin: String) {
        val current = load()
        val next = if (pin.isEmpty()) {
            current.copy(typeAEnabled = false, typeAHash = "", typeASalt = "", typeALength = 0)
        } else {
            val salt = PinHasher.newSalt()
            current.copy(
                typeAEnabled = true,
                typeAHash = PinHasher.hash(pin, salt),
                typeASalt = salt,
                typeALength = pin.length,
            )
        }
        save(next)
    }

    fun setEmergencyPin(pin: String) {
        val current = load()
        val next = if (pin.isEmpty()) {
            current.copy(
                emergencyEnabled = false,
                emergencyHash = "",
                emergencySalt = "",
                emergencyLength = 0,
            )
        } else {
            val salt = PinHasher.newSalt()
            current.copy(
                emergencyEnabled = true,
                emergencyHash = PinHasher.hash(pin, salt),
                emergencySalt = salt,
                emergencyLength = pin.length,
            )
        }
        save(next)
    }

    fun getFailedAttemptsCount(): Int =
        prefs.getInt(K_FAILED_ATTEMPTS_COUNT, 0)

    fun incrementFailedAttemptsCount(): Int {
        val next = getFailedAttemptsCount() + 1
        prefs.edit { putInt(K_FAILED_ATTEMPTS_COUNT, next) }
        return next
    }

    fun resetFailedAttemptsCount() {
        prefs.edit { putInt(K_FAILED_ATTEMPTS_COUNT, 0) }
    }

    fun isArmed(): Boolean = prefs.getBoolean(K_GUARD_ARMED, false)

    fun markArmed() {
        if (!prefs.getBoolean(K_GUARD_ARMED, false)) {
            prefs.edit { putBoolean(K_GUARD_ARMED, true) }
        }
    }

    fun markDisarmed() {
        if (prefs.getBoolean(K_GUARD_ARMED, false)) {
            prefs.edit { putBoolean(K_GUARD_ARMED, false) }
        }
    }

    private fun loadInternal(): GuardConfig = GuardConfig(
        masterEnabled = prefs.getBoolean(K_MASTER_ENABLED, false),
        typeAEnabled = prefs.getBoolean(K_TYPE_A_ENABLED, false),
        typeAHash = prefs.getString(K_TYPE_A_HASH, "").orEmpty(),
        typeASalt = prefs.getString(K_TYPE_A_SALT, "").orEmpty(),
        typeALength = prefs.getInt(K_TYPE_A_LENGTH, 0),
        typeBEnabled = prefs.getBoolean(K_TYPE_B_ENABLED, false),
        typeBLength = prefs.getInt(K_TYPE_B_LENGTH, 0),
        emergencyEnabled = prefs.getBoolean(K_EMERGENCY_ENABLED, false),
        emergencyHash = prefs.getString(K_EMERGENCY_HASH, "").orEmpty(),
        emergencySalt = prefs.getString(K_EMERGENCY_SALT, "").orEmpty(),
        emergencyLength = prefs.getInt(K_EMERGENCY_LENGTH, 0),
        failedAttemptsEnabled = prefs.getBoolean(K_FAILED_ATTEMPTS_ENABLED, false),
        failedAttemptsThreshold = prefs.getInt(K_FAILED_ATTEMPTS_THRESHOLD, 10),
    )

    private companion object {
        private const val K_MASTER_ENABLED = "guard.master_enabled"

        private const val K_TYPE_A_ENABLED = "guard.type_a.enabled"
        private const val K_TYPE_A_HASH = "guard.type_a.hash"
        private const val K_TYPE_A_SALT = "guard.type_a.salt"
        private const val K_TYPE_A_LENGTH = "guard.type_a.length"

        private const val K_TYPE_B_ENABLED = "guard.type_b.enabled"
        private const val K_TYPE_B_LENGTH = "guard.type_b.length"

        private const val K_EMERGENCY_ENABLED = "guard.emergency.enabled"
        private const val K_EMERGENCY_HASH = "guard.emergency.hash"
        private const val K_EMERGENCY_SALT = "guard.emergency.salt"
        private const val K_EMERGENCY_LENGTH = "guard.emergency.length"

        private const val K_FAILED_ATTEMPTS_ENABLED = "guard.failed_attempts.enabled"
        private const val K_FAILED_ATTEMPTS_THRESHOLD = "guard.failed_attempts.threshold"
        private const val K_FAILED_ATTEMPTS_COUNT = "guard.failed_attempts.count"
        private const val K_GUARD_ARMED = "guard.armed"
    }
}
