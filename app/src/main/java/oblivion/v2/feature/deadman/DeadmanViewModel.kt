package oblivion.v2.feature.deadman

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import oblivion.v2.core.deadman.DeadmanConfig
import oblivion.v2.core.deadman.DeadmanConfigStore
import oblivion.v2.core.deadman.DeadmanScheduler
import oblivion.v2.core.wipe.WipeGateway
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class DeadmanViewModel @Inject constructor(
    app: Application,
    private val store: DeadmanConfigStore,
    private val wipeGateway: WipeGateway,
) : AndroidViewModel(app) {
    val config: StateFlow<DeadmanConfig> = store.config

    fun isAdminActive(): Boolean = wipeGateway.isAdminActive()

    fun setEnabled(enabled: Boolean) {
        if (enabled && !wipeGateway.isAdminActive()) return
        store.setEnabled(enabled)
        DeadmanScheduler.apply(getApplication(), enabled)
    }

    fun setIntervalHours(hours: Long) {
        val safeHours = hours.coerceAtLeast(1L)
        store.setIntervalMs(TimeUnit.HOURS.toMillis(safeHours))
    }

    fun setIntervalDays(days: Long) {
        val safeDays = days.coerceAtLeast(1L)
        store.setIntervalMs(TimeUnit.DAYS.toMillis(safeDays))
    }

    fun checkInNow() {
        store.touchCheckIn()
    }

    fun intervalAsHours(cfg: DeadmanConfig): Long =
        TimeUnit.MILLISECONDS.toHours(cfg.intervalMs).coerceAtLeast(1L)

    fun intervalAsDays(cfg: DeadmanConfig): Long =
        TimeUnit.MILLISECONDS.toDays(cfg.intervalMs).coerceAtLeast(1L)
}
