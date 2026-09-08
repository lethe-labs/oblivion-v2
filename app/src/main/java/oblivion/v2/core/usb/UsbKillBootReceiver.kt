package oblivion.v2.core.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import oblivion.v2.core.log.SecLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UsbKillBootReceiver : BroadcastReceiver() {
    @Inject lateinit var configStore: UsbKillConfigStore

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val enabled = configStore.load().enabled
        SecLog.d(TAG, "boot received action=$action enabled=$enabled")
        if (enabled) {
            UsbKillService.start(context)
        }
    }

    private companion object {
        private const val TAG = "UsbKillBootReceiver"
    }
}
