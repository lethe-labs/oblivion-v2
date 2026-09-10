package oblivion.v2.core.guard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import oblivion.v2.core.log.SecLog
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import oblivion.v2.core.decoy.DecoyConfig
import oblivion.v2.core.decoy.DecoyConfigStore
import oblivion.v2.core.guard.detector.DecoyDetector
import oblivion.v2.core.guard.detector.EmergencyDetector
import oblivion.v2.core.guard.detector.TypeADetector
import oblivion.v2.core.guard.detector.TypeBDetector
import oblivion.v2.core.wipe.WipeGateway
import oblivion.v2.ui.decoy.DecoyNotifier
import javax.inject.Inject

@AndroidEntryPoint
class GuardAccessibilityService : AccessibilityService() {
    @Inject lateinit var store: GuardConfigStore
    @Inject lateinit var decoyStore: DecoyConfigStore
    @Inject lateinit var wipeGateway: WipeGateway
    @Inject lateinit var revocationDetector: GuardRevocationDetector

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var configJob: Job? = null

    private var keyguardManager: KeyguardManager? = null

    private var detectors: List<GuardDetector> = emptyList()
    private var masterEnabled = false

    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    SecLog.d(TAG, "SCREEN_OFF → reset detectors")
                    detectors.forEach(GuardDetector::reset)
                }
                Intent.ACTION_USER_PRESENT -> {
                    SecLog.d(TAG, "USER_PRESENT → reset detectors (failed-count géré par DPM)")
                    detectors.forEach(GuardDetector::reset)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        SecLog.d(TAG, "Service onCreate")
        keyguardManager = getSystemService(KeyguardManager::class.java)
        registerReceiver(
            lockReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
        )

        configJob = combine(store.config, decoyStore.config) { g, d -> g to d }
            .onEach { (guardCfg, decoyCfg) -> rebuildDetectors(guardCfg, decoyCfg) }
            .launchIn(scope)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.also {
            it.eventTypes =
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED or
                    AccessibilityEvent.TYPE_ANNOUNCEMENT or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            it.flags =
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            it.notificationTimeout = 100L
            it.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            // Scoped to SystemUI: the guard must never see events from the
            // user's other apps. This is a privacy boundary, not an
            // optimisation -- an accessibility service that reads everything
            // would be indefensible in an app aimed at at-risk users.
            it.packageNames = arrayOf(SYSTEMUI_PACKAGE)
        }
        SecLog.d(TAG, "Service connected. Listening to $SYSTEMUI_PACKAGE")
    }

    override fun onDestroy() {
        super.onDestroy()
        SecLog.d(TAG, "Service onDestroy")
        configJob?.cancel()
        scope.cancel()
        runCatching { unregisterReceiver(lockReceiver) }
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!masterEnabled) return

        if (keyguardManager?.isDeviceLocked != true) {
            detectors.forEach(GuardDetector::reset)
            return
        }

        val pkg = event.packageName?.toString()
        if (pkg != null && pkg != SYSTEMUI_PACKAGE) return

        for (detector in detectors) {
            // One detector throwing must not take the others down with it:
            // they are independent triggers and the user may only have armed
            // the one that just failed.
            val matched = runCatching { detector.onEvent(event) }
                .onFailure { SecLog.e(TAG, "Detector ${detector.name} threw", it) }
                .getOrDefault(false)
            if (matched) {
                SecLog.d(TAG, "DETECTOR MATCH: ${detector.name} → wipe")
                if (detector.name == DecoyDetector.NAME) {
                    handleDecoyMatch()
                } else {
                    wipeGateway.wipeNow()
                }

                detectors.forEach(GuardDetector::reset)
                return
            }
        }
    }

    private fun rebuildDetectors(config: GuardConfig, decoy: DecoyConfig) {
        val decoyActive = decoy.isReady()
        masterEnabled = (config.masterEnabled && config.hasAnyDetectorEnabled()) || decoyActive
        val next = mutableListOf<GuardDetector>()
        if (config.masterEnabled) {
            if (config.typeAEnabled && config.typeAHash.isNotEmpty()) {
                next += TypeADetector(config.typeAHash, config.typeASalt, config.typeALength)
            }
            if (config.typeBEnabled && config.typeBLength >= GuardConfig.MIN_TRAP_LENGTH) {
                next += TypeBDetector(config.typeBLength)
            }
            if (config.emergencyEnabled && config.emergencyHash.isNotEmpty()) {
                next += EmergencyDetector(
                    config.emergencyHash,
                    config.emergencySalt,
                    config.emergencyLength,
                )
            }
        }
        if (decoyActive) {
            next += DecoyDetector(decoy.pinHash, decoy.pinSalt, decoy.pinLength)
        }
        detectors = next

        val wipeThreshold = if (config.masterEnabled &&
            config.failedAttemptsEnabled &&
            config.failedAttemptsThreshold >= GuardConfig.MIN_FAILED_ATTEMPTS
        ) config.failedAttemptsThreshold else 0
        wipeGateway.setMaxFailedAttemptsForWipe(wipeThreshold)

        if (config.masterEnabled) {
            store.markArmed()
        } else {
            store.markDisarmed()
        }

        SecLog.d(
            TAG,
            "Rebuilt detectors: masterEnabled=$masterEnabled, " +
                "active=${next.map { it.name }}, " +
                "dpmFailedThreshold=$wipeThreshold, " +
                "armed=${store.isArmed()}, " +
                "decoyActive=$decoyActive",
        )
    }

    /**
     * A full-screen-intent notification is the *only* supported way to put a UI
     * over the keyguard from a service: startActivity() is blocked by the
     * background-activity-launch restrictions while locked, and
     * TYPE_APPLICATION_OVERLAY is layered below the keyguard window on purpose
     * (anti-phishing). This is the mechanism incoming-call apps use.
     *
     * The delay lets the decoy screen reach a believable progress before
     * wipeData() kills the process -- long enough to convince, short enough
     * that the attacker cannot pull the battery or reach safe mode.
     */
    private fun handleDecoyMatch() {
        runCatching { DecoyNotifier.trigger(applicationContext) }
            .onFailure { SecLog.e(TAG, "DecoyNotifier.trigger threw", it) }
        Handler(Looper.getMainLooper()).postDelayed(
            { wipeGateway.wipeNow() },
            DECOY_WIPE_DELAY_MS,
        )
    }

    /**
     * Turning the accessibility service off in Settings is how an attacker
     * would neutralise the guard, so revocation while armed is itself a wipe
     * trigger. onUnbind also fires on app updates and low-memory kills, which
     * is why the decision is delegated to GuardRevocationDetector: it confirms
     * against Settings.Secure that the service really is disabled.
     */
    override fun onUnbind(intent: Intent?): Boolean {
        SecLog.w(TAG, "onUnbind called — checking for revocation")
        try {
            revocationDetector.checkAndWipeIfRevoked(applicationContext)
        } catch (t: Throwable) {
            SecLog.e(TAG, "Revocation check failed in onUnbind", t)
        }
        return super.onUnbind(intent)
    }

    private companion object {
        private const val TAG = "GuardAccessibility"
        private const val SYSTEMUI_PACKAGE = "com.android.systemui"

        private const val DECOY_WIPE_DELAY_MS: Long = 8_000L
    }
}
