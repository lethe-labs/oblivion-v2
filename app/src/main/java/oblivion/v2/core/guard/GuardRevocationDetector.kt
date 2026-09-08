package oblivion.v2.core.guard

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import oblivion.v2.core.log.SecLog
import oblivion.v2.core.wipe.WipeGateway
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuardRevocationDetector @Inject constructor(
    private val store: GuardConfigStore,
    private val wipeGateway: WipeGateway,
) {
    fun checkAndWipeIfRevoked(context: Context): Boolean {
        val cfg = store.config.value

        if (!cfg.masterEnabled) {
            store.markDisarmed()
            return false
        }

        if (!store.isArmed()) {
            return false
        }

        if (isAccessibilityServiceEnabled(context)) {
            return false
        }

        SecLog.e(
            TAG,
            "Guard accessibility service REVOKED while masterEnabled=true → emergency wipe",
        )
        val result = wipeGateway.wipeNow()
        SecLog.e(TAG, "wipeNow() result=$result")
        return true
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        }.getOrNull().orEmpty()

        val targetComponent = "${context.packageName}/" +
            GuardAccessibilityService::class.java.name
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(targetComponent, ignoreCase = true)) {
                return true
            }
        }

        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager ?: return false
        if (!am.isEnabled) return false
        return am.getEnabledAccessibilityServiceList(0).orEmpty().any {
            it.resolveInfo?.serviceInfo?.packageName == context.packageName
        }
    }

    private companion object {
        private const val TAG = "GuardRevocation"
    }
}
