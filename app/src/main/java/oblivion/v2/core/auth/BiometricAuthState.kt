package oblivion.v2.core.auth

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import oblivion.v2.core.deadman.DeadmanConfigStore
import oblivion.v2.core.log.SecLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthState @Inject constructor(
    private val deadmanConfigStore: DeadmanConfigStore,
) {
    @Volatile
    private var lastAuthAtElapsedMs: Long = 0L

    private val _isAuthenticated = MutableStateFlow(false)

    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun markAuthenticated() {
        lastAuthAtElapsedMs = SystemClock.elapsedRealtime()
        _isAuthenticated.value = true

        try {
            deadmanConfigStore.touchCheckIn()
        } catch (t: Throwable) {
            SecLog.e(TAG, "touchCheckIn threw", t)
        }
    }

    fun invalidate() {
        lastAuthAtElapsedMs = 0L
        _isAuthenticated.value = false
    }

    fun checkStillValid(): Boolean {
        if (lastAuthAtElapsedMs == 0L) {
            _isAuthenticated.value = false
            return false
        }
        val elapsed = SystemClock.elapsedRealtime() - lastAuthAtElapsedMs
        val stillValid = elapsed < INACTIVITY_TIMEOUT_MS
        if (!stillValid) {
            invalidate()
        } else {
            lastAuthAtElapsedMs = SystemClock.elapsedRealtime()
        }
        return stillValid
    }

    companion object {
        private const val TAG = "BiometricAuthState"

        const val INACTIVITY_TIMEOUT_MS: Long = 60_000L
    }
}
