package oblivion.v2.feature.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import oblivion.v2.core.deadman.DeadmanConfig
import oblivion.v2.core.deadman.DeadmanConfigStore
import oblivion.v2.core.decoy.DecoyConfig
import oblivion.v2.core.decoy.DecoyConfigStore
import oblivion.v2.core.guard.GuardConfig
import oblivion.v2.core.guard.GuardConfigStore
import oblivion.v2.core.scheduled.ScheduledWipeConfig
import oblivion.v2.core.scheduled.ScheduledWipeStore
import oblivion.v2.core.sms.SmsKillConfig
import oblivion.v2.core.sms.SmsKillConfigStore
import oblivion.v2.core.system.SystemCheckHelper
import oblivion.v2.core.usb.UsbKillConfig
import oblivion.v2.core.usb.UsbKillConfigStore
import oblivion.v2.core.voice.VoiceKillConfig
import oblivion.v2.core.voice.VoiceKillConfigStore
import oblivion.v2.core.wipe.WipeGateway
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    app: Application,
    private val wipeGateway: WipeGateway,
    private val systemCheck: SystemCheckHelper,
    guardStore: GuardConfigStore,
    usbStore: UsbKillConfigStore,
    voiceStore: VoiceKillConfigStore,
    smsStore: SmsKillConfigStore,
    deadmanStore: DeadmanConfigStore,
    scheduledStore: ScheduledWipeStore,
    decoyStore: DecoyConfigStore,
) : AndroidViewModel(app) {
    val guardConfig: StateFlow<GuardConfig> = guardStore.config
    val usbConfig: StateFlow<UsbKillConfig> = usbStore.config
    val voiceConfig: StateFlow<VoiceKillConfig> = voiceStore.config
    val smsConfig: StateFlow<SmsKillConfig> = smsStore.config
    val deadmanConfig: StateFlow<DeadmanConfig> = deadmanStore.config
    val scheduledConfig: StateFlow<ScheduledWipeConfig> = scheduledStore.config
    val decoyConfig: StateFlow<DecoyConfig> = decoyStore.config

    private val _adminActive = MutableStateFlow(wipeGateway.isAdminActive())
    val adminActive: StateFlow<Boolean> = _adminActive.asStateFlow()

    private val _showPasswordOk = MutableStateFlow(systemCheck.isShowPasswordEnabled())
    val showPasswordOk: StateFlow<Boolean> = _showPasswordOk.asStateFlow()

    private val _pinPrivacyOk = MutableStateFlow(systemCheck.isEnhancedPinPrivacyDisabled())
    val pinPrivacyOk: StateFlow<Boolean> = _pinPrivacyOk.asStateFlow()

    fun refreshAdminState() {
        _adminActive.value = wipeGateway.isAdminActive()
        _showPasswordOk.value = systemCheck.isShowPasswordEnabled()
        _pinPrivacyOk.value = systemCheck.isEnhancedPinPrivacyDisabled()
    }

    fun requestAdminIntent() = wipeGateway.requestAdminIntent()

    val adminComponentName: String =
        wipeGateway.adminComponentName().flattenToShortString()
}
