package oblivion.v2.core.sms

import java.util.Locale

object SmsMatcher {
    private const val TRUNK_PREFIX = '0'

    private const val MIN_NATIONAL_DIGITS = 6

    private val COUNTRY_CODE = Regex("^\\+\\d{1,3}$")

    private val NON_DIALLABLE = Regex("[^\\d+]")

    fun normalize(number: String): String {
        val cleaned = NON_DIALLABLE.replace(number, "")
        if (cleaned.isEmpty()) return ""
        val plus = cleaned.startsWith("+")
        val digits = cleaned.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        return when {
            plus -> "+$digits"
            digits.startsWith("00") && digits.length > 2 -> "+${digits.substring(2)}"
            else -> digits
        }
    }

    fun senderMatches(received: String, configured: String): Boolean {
        val r = normalize(received)
        val c = normalize(configured)
        if (r.isEmpty() || c.isEmpty()) return false
        if (r == c) return true
        return sameAcrossFormats(r, c) || sameAcrossFormats(c, r)
    }

    fun keywordMatches(body: String, keyword: String): Boolean {
        val kw = keyword.trim().lowercase(Locale.ROOT)
        if (kw.isEmpty()) return false
        val delimited = Regex(
            "(?<![\\p{L}\\p{N}])" + Regex.escape(kw) + "(?![\\p{L}\\p{N}])",
        )
        return delimited.containsMatchIn(body.lowercase(Locale.ROOT))
    }

    private fun sameAcrossFormats(intl: String, local: String): Boolean {
        if (!intl.startsWith("+")) return false
        if (local.startsWith("+") || !local.startsWith(TRUNK_PREFIX)) return false

        val national = local.substring(1)
        if (national.length < MIN_NATIONAL_DIGITS) return false
        if (!intl.endsWith(national)) return false

        return COUNTRY_CODE.matches(intl.dropLast(national.length))
    }
}
