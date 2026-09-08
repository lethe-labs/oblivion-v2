package oblivion.v2.core.guard.detector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCandidateBufferTest {
    private fun bufferOf(digits: String, maxLength: Int = 64) =
        PinCandidateBuffer(maxLength).apply { digits.forEach { append(it) } }

    @Test
    fun `le pin de detresse matche encore apres une tentative ratee`() {
        val buffer = bufferOf("1111" + "1234")
        assertEquals(listOf("1234"), buffer.candidates(exactLength = 4, minLength = 4))
    }

    @Test
    fun `le pin de detresse matche encore apres plusieurs tentatives ratees`() {
        val buffer = bufferOf("1111" + "2222" + "9999" + "1234")
        assertEquals(listOf("1234"), buffer.candidates(exactLength = 4, minLength = 4))
    }

    @Test
    fun `aucun candidat tant que la longueur attendue n est pas atteinte`() {
        assertTrue(bufferOf("123").candidates(exactLength = 4, minLength = 4).isEmpty())
    }

    @Test
    fun `longueur inconnue renvoie tous les suffixes du plus long au plus court`() {
        assertEquals(
            listOf("123456", "23456", "3456", "456"),
            bufferOf("123456").candidates(exactLength = 0, minLength = 3),
        )
    }

    @Test
    fun `longueur inconnue respecte la longueur minimale`() {
        assertTrue(bufferOf("12").candidates(exactLength = 0, minLength = 4).isEmpty())
    }

    @Test
    fun `supprimer retire le dernier chiffre`() {
        val buffer = bufferOf("12345")
        buffer.deleteLast()
        assertEquals(listOf("1234"), buffer.candidates(exactLength = 4, minLength = 4))
    }

    @Test
    fun `supprimer sur un buffer vide ne leve pas`() {
        val buffer = PinCandidateBuffer()
        buffer.deleteLast()
        assertEquals(0, buffer.length)
    }

    @Test
    fun `vider annule la saisie en cours`() {
        val buffer = bufferOf("1234")
        buffer.clear()
        assertEquals(0, buffer.length)
        assertTrue(buffer.candidates(exactLength = 4, minLength = 4).isEmpty())
    }

    @Test
    fun `la fenetre glisse par la gauche au dela de la taille maximale`() {
        val buffer = bufferOf("123456", maxLength = 4)
        assertEquals(4, buffer.length)
        assertEquals(listOf("3456"), buffer.candidates(exactLength = 4, minLength = 4))
    }

    @Test
    fun `la fenetre glissante conserve un pin de detresse recent`() {
        val buffer = bufferOf("9".repeat(80) + "1234")
        assertEquals(listOf("1234"), buffer.candidates(exactLength = 4, minLength = 4))
    }

    @Test
    fun `tronquer recale le buffer sur le curseur`() {
        val buffer = bufferOf("123456")
        buffer.truncateTo(4)
        assertEquals(listOf("1234"), buffer.candidates(exactLength = 4, minLength = 4))
    }

    @Test
    fun `tronquer au dela de la taille courante ne change rien`() {
        val buffer = bufferOf("1234")
        buffer.truncateTo(10)
        assertEquals(4, buffer.length)
    }

    @Test
    fun `une longueur attendue inferieure au minimum ne produit aucun candidat`() {
        assertTrue(bufferOf("123456").candidates(exactLength = 2, minLength = 4).isEmpty())
    }
}
