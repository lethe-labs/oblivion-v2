package oblivion.v2.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Threat model: an attacker who images the device and breaks the
 * EncryptedSharedPreferences layer gets the duress hash, and can then search
 * offline for *which* PIN triggers the wipe -- in order to avoid typing it.
 * A single-round SHA-256 over a 4-8 digit PIN falls in milliseconds, hence
 * PBKDF2.
 *
 * Stored hashes are self-describing so both generations can coexist:
 *   pbkdf2$<iterations>$<digest>  current
 *   <bare base64, no '$'>         legacy SHA-256, still verified
 *
 * Do not drop the legacy path: it would silently invalidate the duress PIN of
 * every device already in the field. Legacy hashes are re-encoded to PBKDF2
 * the next time the user sets the PIN.
 *
 * Uses java.util.Base64 (API 26+, our minSdk) rather than android.util.Base64:
 * byte-for-byte identical output, but keeps this class testable without
 * Robolectric.
 */
object PinHasher {
    /**
     * Cost/latency trade-off. This runs on the accessibility service's main
     * thread on every keypress once the expected length is reached: roughly
     * 100-150 ms on a recent phone, ~400 ms on a low-end Android 8. It does not
     * slow the lockscreen down (we only observe events, we never block
     * SystemUI), it only delays our own trigger. Raising it would make
     * detection visibly sluggish on the old devices this project targets.
     */
    const val PBKDF2_ITERATIONS: Int = 100_000

    private const val SALT_BYTES = 32

    private const val PBKDF2_ALGO = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_PREFIX = "pbkdf2"
    private const val PBKDF2_KEY_BITS = 256
    private const val LEGACY_ALGO = "SHA-256"
    private const val SEP = '$'

    fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(bytes)
        return encode(bytes)
    }

    // Falls back to the legacy digest if the platform has no PBKDF2 provider
    // (exotic ROM), rather than leaving the user unable to set a duress PIN at
    // all. verify() accepts both formats, so the downgrade is transparent.
    fun hash(pin: String, salt: String): String =
        runCatching {
            "$PBKDF2_PREFIX$SEP$PBKDF2_ITERATIONS$SEP${pbkdf2(pin, salt, PBKDF2_ITERATIONS)}"
        }.getOrElse { legacyHash(pin, salt) }

    fun legacyHash(pin: String, salt: String): String {
        val md = MessageDigest.getInstance(LEGACY_ALGO)
        md.update(decode(salt))
        return encode(md.digest(pin.toByteArray(Charsets.UTF_8)))
    }

    // Never throws: this sits on the lockscreen path, where an exception would
    // take the guard down exactly when it is needed. A malformed stored hash
    // must read as "no match", not as a crash.
    fun verify(pin: String, expectedHash: String, salt: String): Boolean {
        if (pin.isEmpty() || expectedHash.isEmpty() || salt.isEmpty()) return false
        return runCatching {
            val parts = expectedHash.split(SEP)
            if (parts.size == 3 && parts[0] == PBKDF2_PREFIX) {
                val iterations = parts[1].toIntOrNull() ?: return@runCatching false
                if (iterations <= 0) return@runCatching false
                constantTimeEquals(pbkdf2(pin, salt, iterations), parts[2])
            } else {
                constantTimeEquals(legacyHash(pin, salt), expectedHash)
            }
        }.getOrDefault(false)
    }

    fun isLegacyFormat(storedHash: String): Boolean =
        storedHash.isNotEmpty() && !storedHash.startsWith("$PBKDF2_PREFIX$SEP")

    private fun pbkdf2(pin: String, salt: String, iterations: Int): String {
        val spec = PBEKeySpec(pin.toCharArray(), decode(salt), iterations, PBKDF2_KEY_BITS)
        try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGO)
            return encode(factory.generateSecret(spec).encoded)
        } finally {
            spec.clearPassword()
        }
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}
