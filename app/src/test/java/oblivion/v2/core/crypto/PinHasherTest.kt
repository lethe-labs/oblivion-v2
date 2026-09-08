package oblivion.v2.core.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64

class PinHasherTest {
    private fun legacySha256(saltB64: String, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(Base64.getDecoder().decode(saltB64))
        return Base64.getEncoder().encodeToString(md.digest(pin.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun `le sel fait 32 octets`() {
        assertEquals(32, Base64.getDecoder().decode(PinHasher.newSalt()).size)
    }

    @Test
    fun `deux sels consecutifs different`() {
        assertNotEquals(PinHasher.newSalt(), PinHasher.newSalt())
    }

    @Test
    fun `le hash courant est au format pbkdf2 auto descriptif`() {
        val hash = PinHasher.hash("1234", PinHasher.newSalt())
        val parts = hash.split('$')
        assertEquals(3, parts.size)
        assertEquals("pbkdf2", parts[0])
        assertEquals(PinHasher.PBKDF2_ITERATIONS.toString(), parts[1])
        assertTrue(parts[2].isNotEmpty())
    }

    @Test
    fun `verify accepte le bon pin au format courant`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("482913", salt)
        assertTrue(PinHasher.verify("482913", hash, salt))
    }

    @Test
    fun `verify refuse un mauvais pin`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("482913", salt)
        assertFalse(PinHasher.verify("482914", hash, salt))
        assertFalse(PinHasher.verify("48291", hash, salt))
        assertFalse(PinHasher.verify("4829130", hash, salt))
        assertFalse(PinHasher.verify("", hash, salt))
    }

    @Test
    fun `verify refuse le bon pin avec un autre sel`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("482913", salt)
        assertFalse(PinHasher.verify("482913", hash, PinHasher.newSalt()))
    }

    @Test
    fun `verify accepte encore les hash SHA-256 herites`() {
        val salt = PinHasher.newSalt()
        val legacy = legacySha256(salt, "1234")
        assertTrue(PinHasher.verify("1234", legacy, salt))
        assertFalse(PinHasher.verify("1235", legacy, salt))
    }

    @Test
    fun `le format herite est bien detecte comme tel`() {
        val salt = PinHasher.newSalt()
        assertTrue(PinHasher.isLegacyFormat(legacySha256(salt, "1234")))
        assertFalse(PinHasher.isLegacyFormat(PinHasher.hash("1234", salt)))
    }

    @Test
    fun `verify ne leve jamais sur une entree corrompue`() {
        val salt = PinHasher.newSalt()
        assertFalse(PinHasher.verify("1234", "pbkdf2\$pasunnombre\$abc", salt))
        assertFalse(PinHasher.verify("1234", "pbkdf2\$0\$abc", salt))
        assertFalse(PinHasher.verify("1234", "pbkdf2\$-1\$abc", salt))
        assertFalse(PinHasher.verify("1234", "n'importe quoi", salt))
        assertFalse(PinHasher.verify("1234", PinHasher.hash("1234", salt), "pas du base64 !"))
        assertFalse(PinHasher.verify("1234", "", salt))
        assertFalse(PinHasher.verify("1234", "abc", ""))
    }
}
