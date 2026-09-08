package oblivion.v2.core.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsMatcherTest {
    @Test
    fun `normalise les separateurs de mise en forme`() {
        assertEquals("+33612345678", SmsMatcher.normalize("+33 6 12-34.56 78"))
        assertEquals("+33612345678", SmsMatcher.normalize("(+33) 612 345 678"))
        assertEquals("0612345678", SmsMatcher.normalize("06 12 34 56 78"))
    }

    @Test
    fun `normalise le prefixe international 00 en plus`() {
        assertEquals("+33612345678", SmsMatcher.normalize("0033612345678"))
    }

    @Test
    fun `rejette un expediteur alphanumerique`() {
        assertEquals("", SmsMatcher.normalize("ORANGE"))
        assertFalse(SmsMatcher.senderMatches("ORANGE", "+33612345678"))
        assertFalse(SmsMatcher.senderMatches("+33612345678", ""))
    }

    @Test
    fun `accepte le meme numero ecrit differemment`() {
        assertTrue(SmsMatcher.senderMatches("+33612345678", "+33 6 12 34 56 78"))
        assertTrue(SmsMatcher.senderMatches("0033612345678", "+33612345678"))
        assertTrue(SmsMatcher.senderMatches("0612345678", "0612345678"))
    }

    @Test
    fun `accepte l equivalence international national`() {
        assertTrue(SmsMatcher.senderMatches("+33612345678", "0612345678"))
        assertTrue(SmsMatcher.senderMatches("0612345678", "+33612345678"))
    }

    @Test
    fun `refuse la collision par suffixe entre deux abonnes sans rapport`() {
        assertFalse(SmsMatcher.senderMatches("+15551234567", "+33551234567"))
        assertFalse(SmsMatcher.senderMatches("+33612345678", "+33712345678"))
    }

    @Test
    fun `refuse un numero tronque ou rallonge`() {
        assertFalse(SmsMatcher.senderMatches("+3361234567", "+33612345678"))
        assertFalse(SmsMatcher.senderMatches("+336123456789", "+33612345678"))
    }

    @Test
    fun `refuse une equivalence sur un numero national trop court`() {
        assertFalse(SmsMatcher.senderMatches("+3312345", "012345"))
    }

    @Test
    fun `refuse une equivalence dont le prefixe n est pas un indicatif pays`() {
        assertFalse(SmsMatcher.senderMatches("+123456612345678", "0612345678"))
    }

    @Test
    fun `le mot cle matche comme occurrence delimitee`() {
        assertTrue(SmsMatcher.keywordMatches("Bonjour, ORAGE maintenant", "orage"))
        assertTrue(SmsMatcher.keywordMatches("orage", "ORAGE"))
        assertTrue(SmsMatcher.keywordMatches("code: orage.", "orage"))
        assertTrue(SmsMatcher.keywordMatches("[orage]", "orage"))
    }

    @Test
    fun `le mot cle ne matche pas au milieu d un autre mot`() {
        assertFalse(SmsMatcher.keywordMatches("orageux", "orage"))
        assertFalse(SmsMatcher.keywordMatches("un orages", "orage"))
        assertFalse(SmsMatcher.keywordMatches("https://site.fr/orage2", "orage"))
    }

    @Test
    fun `un mot cle vide ne matche jamais`() {
        assertFalse(SmsMatcher.keywordMatches("n importe quoi", ""))
        assertFalse(SmsMatcher.keywordMatches("n importe quoi", "   "))
    }

    @Test
    fun `un mot cle contenant des caracteres regex est traite litteralement`() {
        assertTrue(SmsMatcher.keywordMatches("declenche a.b maintenant", "a.b"))
        assertFalse(SmsMatcher.keywordMatches("declenche axb maintenant", "a.b"))
    }
}
