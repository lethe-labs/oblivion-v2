package oblivion.v2.core.deadman

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import oblivion.v2.core.log.SecLog
import java.util.concurrent.TimeUnit

/**
 * Schedules the dead man's switch with two layers:
 *
 *  1. An EXACT AlarmManager alarm at the precise expiry time (lastCheckIn +
 *     interval). This is what makes the wipe land on the deadline.
 *  2. The periodic WorkManager job as a coarse safety net, in case the exact
 *     alarm is killed by an aggressive OEM battery manager.
 *
 * [reschedule] is the single entry point: it reads the current config and
 * arms or cancels both layers accordingly. Call it after any state change
 * (enable, interval change, check-in) and on boot.
 */
object DeadmanScheduler {
    private const val TAG = "DeadmanScheduler"
    private const val UNIQUE_WORK_NAME = "oblivion.deadman.periodic"
    private const val REQUEST_CODE = 0x0DEA_D000.toInt()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeadmanSchedulerEntryPoint {
        fun deadmanConfigStore(): DeadmanConfigStore
    }

    /**
     * Re-reads the config and brings both layers in line with it. Cancels
     * everything when the switch is off or not ready.
     */
    fun reschedule(context: Context) {
        val appCtx = context.applicationContext
        val cfg = try {
            EntryPointAccessors.fromApplication(appCtx, DeadmanSchedulerEntryPoint::class.java)
                .deadmanConfigStore()
                .load()
        } catch (t: Throwable) {
            SecLog.e(TAG, "reschedule() could not load config", t)
            return
        }

        if (!cfg.isReady()) {
            cancel(appCtx)
            return
        }

        scheduleWorker(appCtx)
        armExactAlarm(appCtx, cfg.lastCheckInMs + cfg.intervalMs)
    }

    fun cancel(context: Context) {
        val appCtx = context.applicationContext
        cancelExactAlarm(appCtx)
        try {
            WorkManager.getInstance(appCtx).cancelUniqueWork(UNIQUE_WORK_NAME)
            SecLog.d(TAG, "cancel() done")
        } catch (t: Throwable) {
            SecLog.e(TAG, "cancel() threw", t)
        }
    }

    // ── Exact alarm layer ────────────────────────────────────────────────

    private fun armExactAlarm(context: Context, wipeAtMs: Long) {
        try {
            val am = context.getSystemService(AlarmManager::class.java) ?: run {
                SecLog.e(TAG, "AlarmManager null")
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // No exact-alarm permission: the periodic worker still covers us,
                // just with up to 15 min of slack.
                SecLog.e(TAG, "canScheduleExactAlarms == false — relying on worker only")
                return
            }
            am.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                wipeAtMs,
                buildPendingIntent(context),
            )
            SecLog.d(TAG, "armExactAlarm() scheduled for $wipeAtMs")
        } catch (t: Throwable) {
            SecLog.e(TAG, "armExactAlarm() threw", t)
        }
    }

    private fun cancelExactAlarm(context: Context) {
        try {
            context.getSystemService(AlarmManager::class.java)
                ?.cancel(buildPendingIntent(context))
        } catch (t: Throwable) {
            SecLog.e(TAG, "cancelExactAlarm() threw", t)
        }
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DeadmanReceiver::class.java).apply {
            action = DeadmanReceiver.ACTION_FIRE
            setPackage(context.packageName)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
    }

    // ── Periodic worker layer (safety net) ───────────────────────────────

    private fun scheduleWorker(context: Context) {
        try {
            val req = PeriodicWorkRequestBuilder<DeadmanWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
            SecLog.d(TAG, "scheduleWorker() enqueued (15-min periodic, KEEP policy)")
        } catch (t: Throwable) {
            SecLog.e(TAG, "scheduleWorker() threw", t)
        }
    }
}
