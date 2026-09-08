package oblivion.v2.feature.decoy

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import oblivion.v2.core.decoy.DecoyConfig
import oblivion.v2.core.decoy.DecoyConfigStore
import oblivion.v2.core.wipe.WipeGateway
import oblivion.v2.ui.decoy.DecoyNotifier
import javax.inject.Inject

@HiltViewModel
class DecoyViewModel @Inject constructor(
    app: Application,
    private val store: DecoyConfigStore,
    private val wipeGateway: WipeGateway,
) : AndroidViewModel(app) {
    val config: StateFlow<DecoyConfig> = store.config

    fun isAdminActive(): Boolean = wipeGateway.isAdminActive()

    fun hasFullScreenIntentPermission(): Boolean =
        DecoyNotifier.canUseFullScreenIntent(getApplication())

    fun fullScreenIntentSettings(): Intent? =
        DecoyNotifier.fullScreenIntentSettingsIntent(getApplication())

    fun setEnabled(enabled: Boolean) {
        if (enabled && !wipeGateway.isAdminActive()) return

        if (enabled && !store.config.value.isConfigured()) return
        store.setEnabled(enabled)
    }

    fun setDecoyPin(pin: String) {
        store.setDecoyPin(pin)
    }

    fun clearDecoyPin() {
        store.setDecoyPin("")
    }
}
