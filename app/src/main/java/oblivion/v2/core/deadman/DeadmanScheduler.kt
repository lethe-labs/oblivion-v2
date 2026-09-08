package oblivion.v2.core.deadman

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import oblivion.v2.core.log.SecLog
import java.util.concurrent.TimeUnit

object DeadmanScheduler {
    private const val TAG = "DeadmanScheduler"
    private const val UNIQUE_WORK_NAME = "oblivion.deadman.periodic"

    fun apply(context: Context, enabled: Boolean) {
        if (enabled) schedule(context) else cancel(context)
    }

    fun schedule(context: Context) {
        try {
            val req = PeriodicWorkRequestBuilder<DeadmanWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
            SecLog.d(TAG, "schedule() enqueued (15-min periodic, KEEP policy)")
        } catch (t: Throwable) {
            SecLog.e(TAG, "schedule() threw", t)
        }
    }

    fun cancel(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            SecLog.d(TAG, "cancel() done")
        } catch (t: Throwable) {
            SecLog.e(TAG, "cancel() threw", t)
        }
    }
}
