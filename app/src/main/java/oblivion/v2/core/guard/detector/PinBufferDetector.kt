package oblivion.v2.core.guard.detector

import android.view.accessibility.AccessibilityEvent
import oblivion.v2.core.crypto.PinHasher
import oblivion.v2.core.guard.GuardDetector
import oblivion.v2.core.log.SecLog

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
