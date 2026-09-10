package oblivion.v2.core.guard.detector

/**
 * Lockscreen keystroke buffer, deliberately free of Android types so it can be
 * unit tested (see PinCandidateBufferTest).
 *
 * Why suffixes rather than the whole buffer: the buffer is only cleared on
 * screen-off or a *successful* unlock, never after a failed attempt. Comparing
 * the whole buffer therefore meant that one mistyped attempt ("1111") left its
 * digits in front of the duress PIN ("1234" -> "11111234") and the secret could
 * never match again until the screen went off. On a tool whose only job is to
 * fire under duress -- where fumbling an attempt is precisely what happens --
 * that made the trigger dead when it mattered.
 *
 * Matching the trailing digits instead means "the last N digits you typed are
 * the secret", which is the documented behaviour (fires on the last keypress,
 * no need to press validate) and survives any number of failed attempts.
 *
 * Trade-off, documented in the README: a duress PIN that is a *suffix* of the
 * real PIN (real "981234", duress "1234") now fires on every normal unlock.
 */
class PinCandidateBuffer(private val maxLength: Int = DEFAULT_MAX_LENGTH) {
    private val digits = StringBuilder()

    val length: Int get() = digits.length

    // Slides the window instead of growing without bound. Dropping from the
    // front is safe because only trailing digits can form a candidate.
    fun append(c: Char) {
        if (digits.length >= maxLength) digits.deleteCharAt(0)
        digits.append(c)
    }

    fun deleteLast() {
        if (digits.isNotEmpty()) digits.deleteCharAt(digits.length - 1)
    }

    fun clear() {
        digits.setLength(0)
    }

    fun truncateTo(size: Int) {
        if (size in 0 until digits.length) digits.setLength(size)
    }

    /**
     * @param exactLength length of the stored secret, or 0 when unknown.
     *
     * Knowing the length yields a single candidate per keypress, which is what
     * keeps the cost to one PBKDF2 derivation. exactLength is 0 only for
     * configurations written before the length was persisted; those still use
     * the cheap legacy SHA-256, so scanning every suffix is affordable there.
     */
    fun candidates(exactLength: Int, minLength: Int): List<String> {
        if (digits.length < minLength) return emptyList()
        if (exactLength > 0) {
            if (exactLength < minLength || digits.length < exactLength) return emptyList()
            return listOf(digits.substring(digits.length - exactLength))
        }
        return (digits.length downTo minLength).map { digits.substring(digits.length - it) }
    }

    companion object {
        const val DEFAULT_MAX_LENGTH: Int = 64
    }
}
