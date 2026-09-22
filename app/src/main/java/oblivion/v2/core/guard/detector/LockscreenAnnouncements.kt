package oblivion.v2.core.guard.detector

import java.util.Locale

/**
 * Shared vocabulary for "wrong PIN / password" announcements the lockscreen
 * emits as TYPE_ANNOUNCEMENT events.
 *
 * Centralised so the two detectors that rely on it -- FailedAttemptsDetector
 * and TypeBDetector -- cannot drift apart. TypeBDetector previously matched
 * only the English "wrong"/"incorrect", which meant its announcement path was
 * dead on any non-English device: a French phone announces "Code incorrect"
 * or "Réessayez dans...", none of which start with those two words.
 *
 * The exact wording is locale- and OEM-dependent, so matching is deliberately
 * broad: case-insensitive, substring rather than prefix, and covering FR + EN.
 * Broadening this list is safe by construction -- both call sites only act on a
 * rejection once an independent length/count condition already holds, so a
 * spurious match cannot by itself cause a wipe.
 */
internal object LockscreenAnnouncements {

    /**
     *  - "incorrect" : same in FR and EN ("PIN incorrect", "Password incorrect")
     *  - "wrong"     : EN ("Wrong PIN")
     *  - "erron"     : FR stem ("erroné", "erronée")
     *  - "réessay" / "reessay" : FR ("Réessayez dans...", accent-stripped ROMs)
     *  - "try again" : EN
     *  - "faux"      : FR ("code faux")
     *  - "mauvais"   : FR ("mauvais code")
     */
    private val REJECTION_KEYWORDS = listOf(
        "incorrect",
        "wrong",
        "erron",
        "réessay",
        "reessay",
        "try again",
        "faux",
        "mauvais",
    )

    /** True if any phrase looks like a "wrong PIN/password" announcement. */
    fun isRejection(phrases: Iterable<CharSequence?>): Boolean =
        phrases.any { raw ->
            val text = raw?.toString()?.lowercase(Locale.ROOT).orEmpty()
            text.isNotEmpty() && REJECTION_KEYWORDS.any { text.contains(it) }
        }
}
