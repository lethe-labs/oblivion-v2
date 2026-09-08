package oblivion.v2.core.guard.detector

import oblivion.v2.core.log.SecLog
import android.view.accessibility.AccessibilityEvent
import oblivion.v2.core.guard.GuardConfigStore
import oblivion.v2.core.guard.GuardDetector

class FailedAttemptsDetector(
    private val store: GuardConfigStore,
    private val threshold: Int,
) : GuardDetector {
    override val name: String = "FailedAttempts"

    override fun onEvent(event: AccessibilityEvent): Boolean {
        if (event.eventType != AccessibilityEvent.TYPE_ANNOUNCEMENT) return false

        val phrases = event.text.orEmpty().mapNotNull { it?.toString() }
        if (phrases.isEmpty()) return false

        SecLog.d(TAG, "ANNOUNCEMENT: $phrases")

        val matched = phrases.any { raw ->
            val t = raw.lowercase()
            KEYWORDS.any { kw -> t.contains(kw) }
        }
        if (!matched) return false

        val count = store.incrementFailedAttemptsCount()
        SecLog.d(TAG, "Failed attempt detected. Count = $count / threshold = $threshold")
        return count >= threshold
    }

    override fun reset() {
    }

    private companion object {
        private const val TAG = "FailedAttemptsDetector"

        private val KEYWORDS = listOf(
            "incorrect",
            "wrong",
            "erron",
            "réessay",
            "try again",
            "faux",
        )
    }
}
