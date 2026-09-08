package oblivion.v2.core.usb

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import oblivion.v2.core.log.SecLog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import oblivion.v2.MainActivity
import oblivion.v2.OblivionApp
import oblivion.v2.R
import oblivion.v2.core.wipe.WipeGateway
import javax.inject.Inject

@AndroidEntryPoint
class UsbKillService : Service() {
    @Inject lateinit var configStore: UsbKillConfigStore
    @Inject lateinit var wipeGateway: WipeGateway

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var countdownJob: Job? = null

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> onPowerConnected()
                Intent.ACTION_POWER_DISCONNECTED -> onPowerDisconnected()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        SecLog.d(TAG, "onCreate()")
        startForeground(NOTIFICATION_ID, buildNotification(countdown = null))

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            this,
            powerReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        configStore.config
            .onEach { cfg ->
                if (!cfg.enabled) {
                    SecLog.d(TAG, "config.enabled=false → stopSelf()")
                    stopSelf()
                }
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SecLog.d(TAG, "onStartCommand()")

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        SecLog.d(TAG, "onDestroy()")
        runCatching { unregisterReceiver(powerReceiver) }
        countdownJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun onPowerConnected() {
        val cfg = configStore.load()
        if (!cfg.enabled) {
            SecLog.d(TAG, "power connected but trigger disabled → ignore")
            return
        }
        val locked = isDeviceLocked()
        SecLog.d(TAG, "power connected — locked=$locked grace=${cfg.graceSeconds}s")
        if (!locked) {
            return
        }
        startCountdown(cfg.graceSeconds)
    }

    private fun onPowerDisconnected() {
        val running = countdownJob?.isActive == true
        SecLog.d(TAG, "power disconnected — countdown running=$running")
        if (running) {
            countdownJob?.cancel()
            countdownJob = null
            updateNotification(countdown = null)
        }
    }

    private fun startCountdown(graceSeconds: Int) {
        countdownJob?.cancel()
        if (graceSeconds <= 0) {
            SecLog.d(TAG, "grace=0 → wipe immediate")
            triggerWipe()
            return
        }
        countdownJob = scope.launch {
            for (remaining in graceSeconds downTo 1) {
                if (!isActive) return@launch
                updateNotification(countdown = remaining)
                delay(1_000L)
            }
            if (isActive) {
                SecLog.d(TAG, "countdown reached 0 → wipe")
                triggerWipe()
            }
        }
    }

    private fun triggerWipe() {
        updateNotification(countdown = 0)
        val result = wipeGateway.wipeNow()
        SecLog.d(TAG, "wipeNow() result=$result")
    }

    private fun isDeviceLocked(): Boolean {
        val km = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager ?: return false
        return km.isDeviceLocked || km.isKeyguardLocked
    }

    private fun buildNotification(countdown: Int?): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = when {
            countdown == null -> getString(R.string.usb_notif_idle)
            countdown <= 0 -> getString(R.string.usb_notif_wiping)
            else -> getString(R.string.usb_notif_countdown, countdown)
        }

        return NotificationCompat.Builder(this, OblivionApp.CHANNEL_USB_KILL)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.usb_notif_title))
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .setShowWhen(false)
            .build()
    }

    private fun updateNotification(countdown: Int?) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.notify(NOTIFICATION_ID, buildNotification(countdown))
    }

    companion object {
        private const val TAG = "UsbKillService"
        private const val NOTIFICATION_ID = 4201

        fun start(context: Context) {
            val intent = Intent(context, UsbKillService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsbKillService::class.java))
        }
    }
}
