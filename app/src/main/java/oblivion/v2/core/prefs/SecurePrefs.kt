package oblivion.v2.core.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import oblivion.v2.core.admin.DeviceAdminManager
import oblivion.v2.core.log.SecLog
import java.io.File
import java.security.KeyStore

/**
 * Wrapper around EncryptedSharedPreferences, and the single most dangerous
 * class in the project: it can factory-reset the device on its own.
 *
 * When the store fails to open, two indistinguishable scenarios have to be told
 * apart from the exception alone:
 *   - tampering: someone altered the file to neutralise Oblivion -> wipe;
 *   - legitimate key loss: the Keystore master key is gone or unusable after an
 *     OEM update, a restore, or a vendor Keystore bug -> absolutely no wipe.
 *
 * The previous guard was "wipe if device admin is active", which discriminated
 * nothing: the admin is *always* active during a key loss, so a routine system
 * event irreversibly erased the phone.
 */
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

            // Second attempt on purpose: a single failure can be transient
            // (Keystore not ready right after LOCKED_BOOT_COMPLETED, disk
            // contention at boot). Wiping on a startup fluke is unacceptable.
            try {
                return SecurePrefs(buildEncrypted(appCtx))
            } catch (second: Throwable) {
                SecLog.e(TAG, "EncryptedSharedPreferences illisible (essai 2/2)", second)
                onUnreadable(appCtx)
            }

            return SecurePrefs(rebuildFromScratch(appCtx))
        }

        /**
         * Wipes only when all three tampering conditions hold. Removing any one
         * of these checks brings back irreversible resets on healthy devices --
         * weigh that before touching this method.
         *
         * The Keystore check is the decisive one: an attacker who can delete a
         * Keystore entry already has root, and would not bother mangling a
         * preferences file to get it.
         */
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

        /**
         * Start over with an empty store so the app stays launchable; the
         * configuration is lost, which is the price.
         *
         * Deleting the prefs file is enough to drop the Tink keysets too: they
         * live inside this very file (AndroidKeysetManager.withSharedPref binds
         * them to FILE_NAME). If that still fails, the master key itself is the
         * problem and gets rotated.
         *
         * A final failure is allowed to propagate on purpose. An app that
         * refuses to start beats an app that would fall back to storing duress
         * PINs in cleartext.
         */
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
