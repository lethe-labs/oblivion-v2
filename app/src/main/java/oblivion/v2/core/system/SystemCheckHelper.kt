package oblivion.v2.core.system

import android.content.Context
import android.provider.Settings
import oblivion.v2.core.log.SecLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemCheckHelper @Inject constructor(
    @ApplicationContext private val appCtx: Context,
) {
    fun isShowPasswordEnabled(): Boolean {
        return try {
            Settings.System.getInt(
                appCtx.contentResolver,
                Settings.System.TEXT_SHOW_PASSWORD,
                0,
            ) == 1
        } catch (e: Exception) {
            SecLog.d(TAG, "Cannot read TEXT_SHOW_PASSWORD: ${e.message}")
            false
        }
    }

    fun isEnhancedPinPrivacyDisabled(): Boolean {
        return try {
            val value = Settings.Secure.getInt(
                appCtx.contentResolver,
                ENHANCED_PIN_PRIVACY_KEY,
                0,
            )
            value == 0
        } catch (e: Exception) {
            SecLog.d(TAG, "Cannot read enhanced_pin_privacy: ${e.message}")
            true
        }
    }

    companion object {
        private const val TAG = "SystemCheckHelper"

        private const val ENHANCED_PIN_PRIVACY_KEY = "enhanced_pin_privacy"
    }
}
