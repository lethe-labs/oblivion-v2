package oblivion.v2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import oblivion.v2.core.deadman.DeadmanConfigStore
import oblivion.v2.core.deadman.DeadmanScheduler
import oblivion.v2.core.log.SecLog
import oblivion.v2.ui.decoy.DecoyNotifier

@HiltAndroidApp
class OblivionApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        reScheduleDeadmanIfEnabled()
    }

    private fun reScheduleDeadmanIfEnabled() {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                this,
                AppEntryPoint::class.java,
            )
            val cfg = entryPoint.deadmanConfigStore().load()
            if (cfg.enabled) {
                DeadmanScheduler.schedule(this)
            }
        } catch (t: Throwable) {
            SecLog.e("OblivionApp", "reScheduleDeadmanIfEnabled threw", t)
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppEntryPoint {
        fun deadmanConfigStore(): DeadmanConfigStore
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return

        runCatching { nm.deleteNotificationChannel("oblivion.usb_kill") }
        runCatching { nm.deleteNotificationChannel("oblivion.usb_kill.v2") }

        val usbChannel = NotificationChannel(
            CHANNEL_USB_KILL,
            getString(R.string.usb_channel_name),

            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.usb_channel_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }
        nm.createNotificationChannel(usbChannel)

        val voiceChannel = NotificationChannel(
            CHANNEL_VOICE_KILL,
            getString(R.string.voice_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.voice_channel_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }
        nm.createNotificationChannel(voiceChannel)

        runCatching { DecoyNotifier.ensureChannel(this) }
    }

    companion object {
        const val CHANNEL_USB_KILL = "oblivion.usb_kill.v3"
        const val CHANNEL_VOICE_KILL = "oblivion.voice_kill.v1"
    }
}
