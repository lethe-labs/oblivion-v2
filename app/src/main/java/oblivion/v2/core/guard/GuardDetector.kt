package oblivion.v2.core.guard

import android.view.accessibility.AccessibilityEvent

interface GuardDetector {
    val name: String

    fun onEvent(event: AccessibilityEvent): Boolean

    fun reset()
}
