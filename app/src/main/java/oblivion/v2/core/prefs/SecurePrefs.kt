package oblivion.v2.core.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import oblivion.v2.core.admin.DeviceAdminManager
import oblivion.v2.core.log.SecLog
import java.io.File
import java.security.KeyStore

class SecurePrefs private constructor(val prefs: SharedPreferences) {
    companion object {
        private const val TAG = "SecurePrefs"
        private const val FILE_NAME = "oblivion_v2_secure_prefs"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        fun create(context: Context): SecurePrefs {
            val appCtx = context.applicationContext

            try {
                return SecurePrefs(buildEncrypted(appCtx))
            } catch (first: Throwable) {
                SecLog.e(TAG, "EncryptedSharedPreferences illisible (essai 1/2)", first)
            }

            try {
                return SecurePrefs(buildEncrypted(appCtx))
            } catch (second: Throwable) {
                SecLog.e(TAG, "EncryptedSharedPreferences illisible (essai 2/2)", second)
                onUnreadable(appCtx)
            }

            return SecurePrefs(rebuildFromScratch(appCtx))
        }

        private fun onUnreadable(appCtx: Context) {
            if (!prefsFileExists(appCtx)) {
                SecLog.w(TAG, "Aucun fichier de prefs sur le disque → première install, pas de wipe")
                return
            }

            if (masterKeyPresent() != true) {
                SecLog.w(TAG, "Clé maître absente du Keystore → perte de clé légitime, pas de wipe")
                return
            }

            val admin = DeviceAdminManager(appCtx)
            if (!admin.isActive()) {
                SecLog.w(TAG, "Altération suspectée mais admin inactif → reset des prefs")
                return
            }

            SecLog.e(TAG, "Altération du store chiffré confirmée → wipe d'urgence")
            try {
                admin.wipeData()
            } catch (t: Throwable) {
                SecLog.e(TAG, "Wipe d'urgence impossible, reset des prefs à la place", t)
            }
        }

        private fun rebuildFromScratch(appCtx: Context): SharedPreferences {
            appCtx.deleteSharedPreferences(FILE_NAME)
            return try {
                buildEncrypted(appCtx)
            } catch (t: Throwable) {
                SecLog.e(TAG, "Reset des prefs insuffisant → rotation de la clé maître", t)
                deleteMasterKey()
                buildEncrypted(appCtx)
            }
        }

        private fun masterKeyPresent(): Boolean? = runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE)
                .apply { load(null) }
                .containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }.getOrNull()

        private fun deleteMasterKey() {
            runCatching {
                KeyStore.getInstance(ANDROID_KEYSTORE)
                    .apply { load(null) }
                    .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            }.onFailure { SecLog.e(TAG, "Suppression de la clé maître impossible", it) }
        }

        private fun prefsFileExists(appCtx: Context): Boolean {
            val dataDir = appCtx.filesDir?.parentFile ?: return false
            val file = File(File(dataDir, "shared_prefs"), "$FILE_NAME.xml")
            return file.exists() && file.length() > 0L
        }

        private fun buildEncrypted(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
