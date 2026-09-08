package oblivion.v2.ui.decoy

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import oblivion.v2.R
import oblivion.v2.core.log.SecLog

object DecoyNotifier {
    private const val TAG = "DecoyNotifier"

    const val CHANNEL_ID = "oblivion_decoy_update"
    const val NOTIFICATION_ID = 0x0B1D0

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.decoy_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.decoy_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        return nm.canUseFullScreenIntent()
    }

    fun fullScreenIntentSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun trigger(context: Context) {
        val appCtx = context.applicationContext
        val nm = appCtx.getSystemService(NotificationManager::class.java)
        if (nm == null) {
            SecLog.e(TAG, "NotificationManager unavailable")
            return
        }
        ensureChannel(appCtx)

        val activityIntent = Intent(appCtx, DecoyActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
            )
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            appCtx,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif: Notification = Notification.Builder(appCtx, CHANNEL_ID)
            .setContentTitle(appCtx.getString(R.string.decoy_update_title))
            .setContentText(appCtx.getString(R.string.decoy_update_subtitle))

            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()

        try {
            nm.notify(NOTIFICATION_ID, notif)
            SecLog.d(TAG, "Decoy fullScreenIntent posted (FSI permitted=${canUseFullScreenIntent(appCtx)})")
        } catch (t: Throwable) {
            SecLog.e(TAG, "notify() threw", t)
        }
    }

    fun cancel(context: Context) {
        val nm = context.applicationContext
            .getSystemService(NotificationManager::class.java) ?: return
        nm.cancel(NOTIFICATION_ID)
    }
}
