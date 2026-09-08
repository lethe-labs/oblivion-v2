package oblivion.v2.core.wipe

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import oblivion.v2.core.log.SecLog
import oblivion.v2.core.admin.DeviceAdminManager
import oblivion.v2.core.admin.DeviceAdminReceiver

class WipeGateway(private val context: Context) {
    private val appCtx = context.applicationContext
    private val admin = DeviceAdminManager(appCtx)

    fun isAdminActive(): Boolean = admin.isActive()

    fun requestAdminIntent() = admin.makeRequestIntent()

    fun removeAdmin() {
        admin.remove()
    }

    fun adminComponentName(): ComponentName =
        ComponentName(appCtx, DeviceAdminReceiver::class.java)

    fun setMaxFailedAttemptsForWipe(count: Int) {
        val dpm = appCtx.getSystemService(DevicePolicyManager::class.java) ?: return
        val component = adminComponentName()
        if (!dpm.isAdminActive(component)) {
            SecLog.d(TAG, "setMaxFailedAttemptsForWipe: admin not active → no-op")
            return
        }
        val safe = count.coerceAtLeast(0)
        try {
            dpm.setMaximumFailedPasswordsForWipe(component, safe)
            SecLog.d(TAG, "setMaximumFailedPasswordsForWipe($safe) OK")
        } catch (t: Throwable) {
            SecLog.e(TAG, "setMaximumFailedPasswordsForWipe threw", t)
        }
    }

    fun getMaxFailedAttemptsForWipe(): Int {
        val dpm = appCtx.getSystemService(DevicePolicyManager::class.java) ?: return 0
        val component = adminComponentName()
        if (!dpm.isAdminActive(component)) return 0
        return runCatching { dpm.getMaximumFailedPasswordsForWipe(component) }.getOrDefault(0)
    }

    fun getCurrentFailedAttempts(): Int {
        val dpm = appCtx.getSystemService(DevicePolicyManager::class.java) ?: return 0
        val component = adminComponentName()
        if (!dpm.isAdminActive(component)) return 0
        return runCatching { dpm.currentFailedPasswordAttempts }.getOrDefault(0)
    }

    fun wipeNow(): WipeResult {
        val dpm = appCtx.getSystemService(DevicePolicyManager::class.java)
        if (dpm == null) {
            SecLog.e(TAG, "DevicePolicyManager service is null")
            return WipeResult.NoDpm
        }

        val component = adminComponentName()
        val active = dpm.isAdminActive(component)
        SecLog.d(TAG, "wipeNow() component=$component active=$active")

        if (!active) {
            return WipeResult.NotAdmin(component.flattenToShortString())
        }

        return try {
            SecLog.d(TAG, "Calling admin.wipeData() NOW…")
            admin.wipeData()

            SecLog.d(TAG, "admin.wipeData() returned without throwing")
            WipeResult.Called
        } catch (t: Throwable) {
            SecLog.e(TAG, "admin.wipeData() threw: ${t.javaClass.simpleName}: ${t.message}", t)
            WipeResult.Error(t.javaClass.simpleName, t.message ?: "(no message)")
        }
    }

    sealed class WipeResult {
        object NoDpm : WipeResult()

        object Called : WipeResult()

        data class NotAdmin(val expected: String) : WipeResult()

        data class Error(val type: String, val message: String) : WipeResult()
    }

    private companion object {
        private const val TAG = "WipeGateway"
    }
}
