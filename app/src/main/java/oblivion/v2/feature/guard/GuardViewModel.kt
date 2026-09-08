package oblivion.v2.feature.guard

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import oblivion.v2.core.guard.GuardAccessibilityService
import oblivion.v2.core.guard.GuardConfig
import oblivion.v2.core.guard.GuardConfigStore
import oblivion.v2.core.wipe.WipeGateway
import javax.inject.Inject

@HiltViewModel
class GuardViewModel @Inject constructor(
    app: Application,
    private val store: GuardConfigStore,
    private val wipeGateway: WipeGateway,
) : AndroidViewModel(app) {
    val config: StateFlow<GuardConfig> = store.config

    private val _accessibilityEnabled = MutableStateFlow(computeAccessibilityEnabled())
    val accessibilityEnabled: StateFlow<Boolean> = _accessibilityEnabled.asStateFlow()

    private val _failedAttemptsCount = MutableStateFlow(wipeGateway.getCurrentFailedAttempts())
    val failedAttemptsCount: StateFlow<Int> = _failedAttemptsCount.asStateFlow()

    init {
        store.config
            .onEach { _failedAttemptsCount.value = wipeGateway.getCurrentFailedAttempts() }
            .launchIn(viewModelScope)
    }

    fun refreshRuntimeState() {
        _accessibilityEnabled.value = computeAccessibilityEnabled()
        _failedAttemptsCount.value = wipeGateway.getCurrentFailedAttempts()
    }

    fun setMasterEnabled(enabled: Boolean) {
        store.save(config.value.copy(masterEnabled = enabled))
    }

    fun setTypeAEnabled(enabled: Boolean) {
        val current = config.value
        if (enabled && current.typeAHash.isEmpty()) {
            return
        }
        store.save(current.copy(typeAEnabled = enabled))
    }

    fun setTypeAPin(pin: String) {
        store.setTypeAPin(pin)
    }

    fun setTypeBEnabled(enabled: Boolean) {
        val current = config.value
        if (enabled && current.typeBLength < GuardConfig.MIN_TRAP_LENGTH) return
        store.save(current.copy(typeBEnabled = enabled))
    }

    fun setTypeBLength(length: Int) {
        val safe = length.coerceAtLeast(GuardConfig.MIN_TRAP_LENGTH)
        store.save(config.value.copy(typeBLength = safe))
    }

    fun setEmergencyEnabled(enabled: Boolean) {
        val current = config.value
        if (enabled && current.emergencyHash.isEmpty()) return
        store.save(current.copy(emergencyEnabled = enabled))
    }

    fun setEmergencyPin(pin: String) {
        store.setEmergencyPin(pin)
    }

    fun setFailedAttemptsEnabled(enabled: Boolean) {
        val current = config.value
        if (enabled && current.failedAttemptsThreshold < GuardConfig.MIN_FAILED_ATTEMPTS) return
        store.save(current.copy(failedAttemptsEnabled = enabled))
    }

    fun setFailedAttemptsThreshold(threshold: Int) {
        val safe = threshold.coerceAtLeast(GuardConfig.MIN_FAILED_ATTEMPTS)
        store.save(config.value.copy(failedAttemptsThreshold = safe))
    }

    fun refreshFailedAttemptsCount() {
        _failedAttemptsCount.value = wipeGateway.getCurrentFailedAttempts()
    }

    fun openAccessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun computeAccessibilityEnabled(): Boolean {
        val ctx = getApplication<Application>()

        val enabledServices = runCatching {
            Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            )
        }.getOrNull().orEmpty()
        val targetComponent = "${ctx.packageName}/" +
            GuardAccessibilityService::class.java.name
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(targetComponent, ignoreCase = true)) {
                return true
            }
        }

        val am = ctx.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager ?: return false
        if (!am.isEnabled) return false
        return am.getEnabledAccessibilityServiceList(0).orEmpty().any {
            it.resolveInfo?.serviceInfo?.packageName == ctx.packageName
        }
    }
}
