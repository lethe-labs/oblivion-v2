package oblivion.v2.feature.usb

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import oblivion.v2.core.usb.UsbKillConfig
import oblivion.v2.core.usb.UsbKillConfigStore
import oblivion.v2.core.usb.UsbKillService
import oblivion.v2.core.wipe.WipeGateway
import javax.inject.Inject

@HiltViewModel
class UsbKillViewModel @Inject constructor(
    app: Application,
    private val store: UsbKillConfigStore,
    private val wipeGateway: WipeGateway,
) : AndroidViewModel(app) {
    val config: StateFlow<UsbKillConfig> = store.config

    fun isAdminActive(): Boolean = wipeGateway.isAdminActive()

    fun setEnabled(enabled: Boolean) {
        if (enabled && !wipeGateway.isAdminActive()) return

        val next = config.value.copy(enabled = enabled)
        store.save(next)
        val ctx = getApplication<Application>()
        if (enabled) {
            UsbKillService.start(ctx)
        } else {
            UsbKillService.stop(ctx)
        }
    }

    fun setGraceSeconds(seconds: Int) {
        val safe = seconds.coerceIn(
            UsbKillConfig.MIN_GRACE_SECONDS,
            UsbKillConfig.MAX_GRACE_SECONDS,
        )
        store.save(config.value.copy(graceSeconds = safe))
    }
}
