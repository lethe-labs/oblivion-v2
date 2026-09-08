package oblivion.v2.core.scheduled

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import oblivion.v2.core.log.SecLog
import oblivion.v2.core.wipe.WipeGateway

class ScheduledWipeReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ScheduledWipeEntryPoint {
        fun wipeGateway(): WipeGateway
        fun scheduledWipeStore(): ScheduledWipeStore
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        SecLog.e(TAG, "onReceive action=$action")
        if (action != ACTION_FIRE) return

        try {
            val ep = EntryPointAccessors.fromApplication(
                context.applicationContext,
                ScheduledWipeEntryPoint::class.java,
            )
            val gateway = ep.wipeGateway()
            val store = ep.scheduledWipeStore()

            if (!gateway.isAdminActive()) {
                SecLog.e(TAG, "Admin not active — clearing schedule")
                store.clear()
                return
            }

            SecLog.e(TAG, "Scheduled wipe firing NOW")
            val r = gateway.wipeNow()
            SecLog.e(TAG, "wipeNow() returned $r")

            store.clear()
        } catch (t: Throwable) {
            SecLog.e(TAG, "onReceive threw", t)
        }
    }

    companion object {
        private const val TAG = "ScheduledWipeReceiver"
        const val ACTION_FIRE = "oblivion.v2.action.SCHEDULED_WIPE_FIRE"
    }
}
