package oblivion.v2.core.admin

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import oblivion.v2.core.log.SecLog

class DeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        SecLog.e(TAG, "Admin disable REQUESTED via Settings → triggering emergency wipe")
        try {
            DeviceAdminManager(context).wipeData()
        } catch (t: Throwable) {
            SecLog.e(TAG, "Emergency wipe failed on disable request", t)
        }

        return "Désactiver Oblivion supprimera toutes les données de l'appareil."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        SecLog.w(TAG, "Admin has been disabled")
        super.onDisabled(context, intent)
    }

    companion object {
        private const val TAG = "DeviceAdminReceiver"

        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, DeviceAdminReceiver::class.java)
        }
    }
}
