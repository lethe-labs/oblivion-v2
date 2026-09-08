package oblivion.v2.core.usb

import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import oblivion.v2.core.prefs.SecurePrefs

class UsbKillConfigStore(private val securePrefs: SecurePrefs) {
    private val prefs get() = securePrefs.prefs

    private val _config = MutableStateFlow(loadInternal())
    val config: StateFlow<UsbKillConfig> = _config.asStateFlow()

    fun load(): UsbKillConfig = _config.value

    fun save(config: UsbKillConfig) {
        prefs.edit {
            putBoolean(K_ENABLED, config.enabled)
            putInt(K_GRACE_SECONDS, config.graceSeconds)
        }
        _config.value = config
    }

    private fun loadInternal(): UsbKillConfig = UsbKillConfig(
        enabled = prefs.getBoolean(K_ENABLED, false),
        graceSeconds = prefs.getInt(K_GRACE_SECONDS, UsbKillConfig.DEFAULT_GRACE_SECONDS),
    )

    private companion object {
        private const val K_ENABLED = "usb.enabled"
        private const val K_GRACE_SECONDS = "usb.grace_seconds"
    }
}
