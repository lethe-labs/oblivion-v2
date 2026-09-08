package oblivion.v2.core.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
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

    fun hash(pin: String, salt: String): String =
        runCatching {
            "$PBKDF2_PREFIX$SEP$PBKDF2_ITERATIONS$SEP${pbkdf2(pin, salt, PBKDF2_ITERATIONS)}"
        }.getOrElse { legacyHash(pin, salt) }

    fun legacyHash(pin: String, salt: String): String {
        val md = MessageDigest.getInstance(LEGACY_ALGO)
        md.update(decode(salt))
        return encode(md.digest(pin.toByteArray(Charsets.UTF_8)))
    }

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
