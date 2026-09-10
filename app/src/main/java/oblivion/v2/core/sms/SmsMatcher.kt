package oblivion.v2.core.sms

import java.util.Locale

/**
 * Sender and keyword rules for the SMS trigger. Kept out of SmsKillReceiver so
 * it can be unit tested: this is the most exposed trigger in the app, and a
 * false positive here erases the phone remotely.
 *
 * Sender matching used to compare the last 9 digits, which made +15551234567
 * and +33551234567 -- unrelated subscribers -- equivalent. It now requires
 * strict equality after normalisation, or a structurally valid
 * international/national pair.
 *
 * RESIDUAL RISK, not fixable here: SMS sender identity is not authenticated by
 * the network and is forgeable through commercial gateways. Anyone who learns
 * the keyword can fire this trigger remotely. Treat the keyword as being worth
 * as much as the duress PIN itself.
 */
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

    // Delimited occurrence, not a substring: a plain contains() made a keyword
    // of "wipe" match inside "swiped", "wipes" or any URL containing it.
    fun keywordMatches(body: String, keyword: String): Boolean {
        val kw = keyword.trim().lowercase(Locale.ROOT)
        if (kw.isEmpty()) return false
        val delimited = Regex(
            "(?<![\\p{L}\\p{N}])" + Regex.escape(kw) + "(?![\\p{L}\\p{N}])",
        )
        return delimited.containsMatchIn(body.lowercase(Locale.ROOT))
    }

    /**
     * Accepts "+33612345678" against "0612345678": the international form must
     * end with the national number stripped of its trunk '0', and what remains
     * in front must be a well-formed country code.
     *
     * Necessarily lenient about *which* country code, since a number saved in
     * national form carries no country: "+44612345678" matches "0612345678"
     * too. Storing the authorised number in full international form removes
     * that leeway entirely, as matching then reduces to strict equality.
     */
    private fun sameAcrossFormats(intl: String, local: String): Boolean {
        if (!intl.startsWith("+")) return false
        if (local.startsWith("+") || !local.startsWith(TRUNK_PREFIX)) return false

        val national = local.substring(1)
        if (national.length < MIN_NATIONAL_DIGITS) return false
        if (!intl.endsWith(national)) return false

        return COUNTRY_CODE.matches(intl.dropLast(national.length))
    }
}
