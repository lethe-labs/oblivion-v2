package oblivion.v2.core.deadman

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import oblivion.v2.core.log.SecLog

/**
 * Re-arms the dead man's switch exact alarm after a reboot.
 *
 * Exact alarms do not survive a restart, so without this the switch would fall
 * back to the 15-minute worker until the app was next opened. The elapsed time
 * while powered off still counts against the deadline, because expiry is stored
 * as an absolute wall-clock instant (lastCheckIn + interval), not a countdown.
 */
class DeadmanBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        SecLog.d(TAG, "onReceive action=$action → reschedule")
        try {
            DeadmanScheduler.reschedule(context.applicationContext)
        } catch (t: Throwable) {
            SecLog.e(TAG, "onReceive threw", t)
        }
    }

    private companion object {
        private const val TAG = "DeadmanBootReceiver"
    }
}
