package oblivion.v2.core.guard.detector

import android.view.accessibility.AccessibilityEvent
import oblivion.v2.core.crypto.PinHasher
import oblivion.v2.core.guard.GuardDetector
import oblivion.v2.core.log.SecLog

/**
 * Shared engine for the three "type a secret on the lockscreen" detectors:
 * TypeA, Emergency and Decoy. They used to be near-identical ~120-line copies,
 * which meant the buffer defect described in PinCandidateBuffer existed in
 * triplicate and had to be fixed three times.
 *
 * Two detection paths run side by side because neither is reliable alone:
 *   CLICK  primary -- TYPE_VIEW_CLICKED on each keypad key, digit read from
 *          contentDescription. Fires without pressing validate.
 *   TEXT   fallback -- cursor position in TYPE_VIEW_TEXT_CHANGED, for third
 *          party keyboards and OEM lockscreens that emit no usable clicks.
 *
 * @param expectedLength length of the secret, 0 when unknown (config written
 *        before the length was persisted).
 * @param resetOnWindowChange historical Emergency behaviour: treat a window
 *        change as a new attempt.
 */
abstract class PinBufferDetector(
    private val expectedHash: String,
    private val salt: String,
    private val expectedLength: Int,
    final override val name: String,
    private val resetOnWindowChange: Boolean,
) : GuardDetector {
    private val clickBuffer = PinCandidateBuffer(MAX_BUFFER)
    private val textBuffer = PinCandidateBuffer(MAX_BUFFER)
    private var textPos = 0

    final override fun onEvent(event: AccessibilityEvent): Boolean {
        return when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (resetOnWindowChange) reset()
                false
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> handleClick(event)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> handleText(event)
            else -> false
        }
    }

    final override fun reset() {
        clickBuffer.clear()
        textBuffer.clear()
        textPos = 0
    }

    private fun handleClick(event: AccessibilityEvent): Boolean {
        val desc = event.contentDescription?.toString()?.lowercase()?.trim().orEmpty()
        if (desc.isEmpty()) return false

        return when (desc) {
            BUTTON_DELETE_DESC -> {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
                    clickBuffer.clear()
                } else {
                    clickBuffer.deleteLast()
                }
                false
            }
            BUTTON_OK_DESC, BUTTON_ENTER_DESC -> {
                matches(clickBuffer, "click/validate")
            }
            else -> {
                val ch = desc.firstOrNull() ?: return false
                if (!ch.isDigit()) return false
                clickBuffer.append(ch)
                matches(clickBuffer, "click")
            }
        }
    }

    private fun handleText(event: AccessibilityEvent): Boolean {
        val text = event.text?.firstOrNull()?.toString()
        if (text.isNullOrEmpty()) {
            textBuffer.clear()
            textPos = 0
            return false
        }

        if (textPos > text.length) {
            if (textPos > 0) {
                textPos--
                textBuffer.truncateTo(textPos)
            }
            return false
        }

        val c = text.elementAtOrNull(textPos) ?: return false
        if (c == DOT_CHAR) return false
        if (!c.isDigit()) return false

        if (textBuffer.length < MAX_BUFFER) textBuffer.append(c)
        textPos++

        return matches(textBuffer, "text")
    }

    // Exactly one PBKDF2 derivation per call when the length is known; only
    // legacy configs (cheap SHA-256) fall back to scanning every suffix.
    private fun matches(buffer: PinCandidateBuffer, path: String): Boolean {
        if (expectedHash.isEmpty() || salt.isEmpty()) return false
        val candidates = buffer.candidates(expectedLength, MIN_CHECK)
        for (candidate in candidates) {
            if (PinHasher.verify(candidate, expectedHash, salt)) {
                SecLog.d(name, "$name ($path): hash match → wipe")
                return true
            }
        }
        return false
    }

    private companion object {
        private const val DOT_CHAR: Char = '•'
        private const val MIN_CHECK: Int = 4
        private const val MAX_BUFFER: Int = PinCandidateBuffer.DEFAULT_MAX_LENGTH
        private const val BUTTON_DELETE_DESC: String = "delete"
        private const val BUTTON_OK_DESC: String = "ok"
        private const val BUTTON_ENTER_DESC: String = "enter"
    }
}
