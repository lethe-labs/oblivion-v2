package oblivion.v2.core.deadman

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import oblivion.v2.core.log.SecLog
import oblivion.v2.core.wipe.WipeGateway

/**
 * Fires when the dead man's switch reaches its exact expiry time.
 *
 * The periodic worker (DeadmanWorker) stays as a coarse safety net, but this
 * exact alarm is what makes the wipe happen at the deadline instead of up to
 * the WorkManager minimum of 15 minutes late.
 *
 * If the alarm is delivered slightly early (Doze can nudge it), the config is
 * re-checked here and the alarm re-armed rather than wiping prematurely.
 */
class DeadmanReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeadmanReceiverEntryPoint {
        fun deadmanConfigStore(): DeadmanConfigStore
        fun wipeGateway(): WipeGateway
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return

        try {
            val ep = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DeadmanReceiverEntryPoint::class.java,
            )
            val cfg = ep.deadmanConfigStore().load()

            if (!cfg.isReady()) {
                SecLog.d(TAG, "Alarm fired but switch not ready — ignoring")
                return
            }

            if (!cfg.isExpired()) {
                // Delivered early: re-arm for the real deadline.
                SecLog.d(TAG, "Alarm fired before expiry — re-arming")
                DeadmanScheduler.reschedule(context.applicationContext)
                return
            }

            val gateway = ep.wipeGateway()
            if (!gateway.isAdminActive()) {
                SecLog.e(TAG, "Deadman expired but admin not active — cannot wipe")
                return
            }

            SecLog.e(TAG, "Deadman EXPIRED (exact alarm) → wipeNow()")
            val r = gateway.wipeNow()
            SecLog.e(TAG, "wipeNow() returned $r")
        } catch (t: Throwable) {
            SecLog.e(TAG, "onReceive threw", t)
        }
    }

    companion object {
        private const val TAG = "DeadmanReceiver"
        const val ACTION_FIRE = "oblivion.v2.action.DEADMAN_FIRE"
    }
}
