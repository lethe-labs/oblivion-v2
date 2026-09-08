package oblivion.v2.core.guard.detector

class PinCandidateBuffer(private val maxLength: Int = DEFAULT_MAX_LENGTH) {
    private val digits = StringBuilder()

    val length: Int get() = digits.length

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
