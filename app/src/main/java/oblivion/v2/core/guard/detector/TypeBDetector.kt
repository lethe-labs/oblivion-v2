package oblivion.v2.core.guard.detector

import android.view.accessibility.AccessibilityEvent
import oblivion.v2.core.guard.GuardDetector

class TypeBDetector(
    private val targetLength: Int,
) : GuardDetector {
    override val name: String = "TypeB"

    private var pos = 0

    override fun onEvent(event: AccessibilityEvent): Boolean {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> {
            }
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> {
                // A rejection announcement means a full PIN was just submitted
                // (some lockscreens auto-submit at the expected length and never
                // send an OK click). Match FR + EN via the shared vocabulary so
                // this path is not English-only.
                if (LockscreenAnnouncements.isRejection(event.text.orEmpty())) {
                    val ok = pos >= targetLength
                    pos = 0
                    return ok
                }
                return false
            }
            else -> return false
        }

        val desc = event.contentDescription?.toString()?.lowercase()
        return when (desc) {
            BUTTON_DELETE_DESC -> {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
                    pos = 0
                } else if (pos > 0) {
                    pos--
                }
                false
            }
            BUTTON_OK_DESC, BUTTON_ENTER_DESC -> {
                val ok = pos >= targetLength
                pos = 0
                ok
            }
            null -> {
                pos = 0
                false
            }
            else -> {
                pos++

                pos >= targetLength
            }
        }
    }

    override fun reset() {
        pos = 0
    }

    private companion object {
        private const val BUTTON_DELETE_DESC = "delete"
        private const val BUTTON_OK_DESC = "ok"
        private const val BUTTON_ENTER_DESC = "enter"
    }
}
