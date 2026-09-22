package oblivion.v2.core.guard.detector

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockscreenAnnouncementsTest {

    @Test
    fun `detecte les annonces de rejet anglaises`() {
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Wrong PIN")))
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Incorrect password")))
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Try again in 30 seconds")))
    }

    @Test
    fun `detecte les annonces de rejet francaises`() {
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Code incorrect")))
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Code erroné")))
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Réessayez dans 30 secondes")))
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Reessayez plus tard")))
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Mauvais code")))
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Code faux")))
    }

    @Test
    fun `insensible a la casse`() {
        assertTrue(LockscreenAnnouncements.isRejection(listOf("INCORRECT")))
        assertTrue(LockscreenAnnouncements.isRejection(listOf("wRoNg pin")))
    }

    @Test
    fun `ignore les annonces neutres`() {
        assertFalse(LockscreenAnnouncements.isRejection(listOf("Enter PIN")))
        assertFalse(LockscreenAnnouncements.isRejection(listOf("Saisissez votre code")))
        assertFalse(LockscreenAnnouncements.isRejection(listOf("Emergency call")))
    }

    @Test
    fun `gere les listes vides ou nulles`() {
        assertFalse(LockscreenAnnouncements.isRejection(emptyList()))
        assertFalse(LockscreenAnnouncements.isRejection(listOf<CharSequence?>(null)))
        assertFalse(LockscreenAnnouncements.isRejection(listOf("")))
    }

    @Test
    fun `matche meme si le mot cle est noye dans plusieurs phrases`() {
        assertFalse(LockscreenAnnouncements.isRejection(listOf("Verrouillé", "Batterie 80%")))
        assertTrue(LockscreenAnnouncements.isRejection(listOf("Verrouillé", "Code incorrect")))
    }
}
