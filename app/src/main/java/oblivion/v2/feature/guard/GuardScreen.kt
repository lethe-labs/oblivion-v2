package oblivion.v2.feature.guard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oblivion.v2.R
import oblivion.v2.core.guard.GuardConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardScreen(
    onBack: () -> Unit,
    vm: GuardViewModel = hiltViewModel(),
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val accessibilityEnabled by vm.accessibilityEnabled.collectAsStateWithLifecycle()
    val failedCount by vm.failedAttemptsCount.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshRuntimeState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.guard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MasterSection(
                config = config,
                accessibilityEnabled = accessibilityEnabled,
                onMasterToggle = vm::setMasterEnabled,
                onOpenAccessibility = {
                    runCatching { ctx.startActivity(vm.openAccessibilitySettingsIntent()) }
                },
            )

            TypeASection(
                enabled = config.typeAEnabled,
                configured = config.typeAHash.isNotEmpty(),
                onToggle = vm::setTypeAEnabled,
                onSetPin = vm::setTypeAPin,
            )

            TypeBSection(
                enabled = config.typeBEnabled,
                length = config.typeBLength,
                onToggle = vm::setTypeBEnabled,
                onLengthChange = vm::setTypeBLength,
            )

            EmergencySection(
                enabled = config.emergencyEnabled,
                configured = config.emergencyHash.isNotEmpty(),
                onToggle = vm::setEmergencyEnabled,
                onSetPin = vm::setEmergencyPin,
            )

            FailedAttemptsSection(
                enabled = config.failedAttemptsEnabled,
                threshold = config.failedAttemptsThreshold,
                currentCount = failedCount,
                onToggle = vm::setFailedAttemptsEnabled,
                onThresholdChange = vm::setFailedAttemptsThreshold,
                onRefreshCount = vm::refreshFailedAttemptsCount,
            )
        }
    }
}

@Composable
private fun MasterSection(
    config: GuardConfig,
    accessibilityEnabled: Boolean,
    onMasterToggle: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.guard_master_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = config.masterEnabled,
                    onCheckedChange = onMasterToggle,
                )
            }
            Text(
                text = stringResource(R.string.guard_master_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            Divider()
            Text(
                text = stringResource(
                    if (accessibilityEnabled) R.string.guard_accessibility_ok
                    else R.string.guard_accessibility_off
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = if (accessibilityEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontWeight = FontWeight.Bold,
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenAccessibility,
            ) {
                Text(text = stringResource(R.string.guard_open_accessibility))
            }
        }
    }
}

@Composable
private fun TypeASection(
    enabled: Boolean,
    configured: Boolean,
    onToggle: (Boolean) -> Unit,
    onSetPin: (String) -> Unit,
) {
    PinSection(
        title = stringResource(R.string.guard_type_a_title),
        description = stringResource(R.string.guard_type_a_desc),
        enabled = enabled,
        configured = configured,
        onToggle = onToggle,
        onSetPin = onSetPin,
    )
}

@Composable
private fun EmergencySection(
    enabled: Boolean,
    configured: Boolean,
    onToggle: (Boolean) -> Unit,
    onSetPin: (String) -> Unit,
) {
    PinSection(
        title = stringResource(R.string.guard_emergency_title),
        description = stringResource(R.string.guard_emergency_desc),
        enabled = enabled,
        configured = configured,
        onToggle = onToggle,
        onSetPin = onSetPin,
    )
}

@Composable
private fun PinSection(
    title: String,
    description: String,
    enabled: Boolean,
    configured: Boolean,
    onToggle: (Boolean) -> Unit,
    onSetPin: (String) -> Unit,
) {
    var pinInput by remember { mutableStateOf("") }
    val canEnable = configured || pinInput.length >= GuardConfig.MIN_SECRET_LENGTH

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    enabled = canEnable,
                    onCheckedChange = onToggle,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    if (configured) R.string.guard_pin_configured
                    else R.string.guard_pin_not_configured
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (configured) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
            )

            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it.take(32) },
                label = { Text(stringResource(R.string.guard_pin_input_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        text = stringResource(
                            R.string.guard_pin_min_hint,
                            GuardConfig.MIN_SECRET_LENGTH,
                        )
                    )
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        if (pinInput.length >= GuardConfig.MIN_SECRET_LENGTH) {
                            onSetPin(pinInput)
                            pinInput = ""
                        }
                    },
                    enabled = pinInput.length >= GuardConfig.MIN_SECRET_LENGTH,
                ) {
                    Text(stringResource(R.string.guard_pin_save))
                }
                if (configured) {
                    TextButton(
                        onClick = {
                            onSetPin("")
                            pinInput = ""
                        },
                    ) {
                        Text(stringResource(R.string.guard_pin_clear))
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBSection(
    enabled: Boolean,
    length: Int,
    onToggle: (Boolean) -> Unit,
    onLengthChange: (Int) -> Unit,
) {
    val display = length.coerceAtLeast(GuardConfig.MIN_TRAP_LENGTH)
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.guard_type_b_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                )
            }
            Text(
                text = stringResource(R.string.guard_type_b_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.guard_type_b_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.guard_type_b_length, display),
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = display.toFloat(),
                onValueChange = { onLengthChange(it.toInt()) },
                valueRange = GuardConfig.MIN_TRAP_LENGTH.toFloat()..20f,
                steps = 20 - GuardConfig.MIN_TRAP_LENGTH - 1,
            )
        }
    }
}

@Composable
private fun FailedAttemptsSection(
    enabled: Boolean,
    threshold: Int,
    currentCount: Int,
    onToggle: (Boolean) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onRefreshCount: () -> Unit,
) {
    val displayThreshold = threshold.coerceAtLeast(GuardConfig.MIN_FAILED_ATTEMPTS)
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.guard_failed_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                )
            }
            Text(
                text = stringResource(R.string.guard_failed_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.guard_failed_threshold, displayThreshold),
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = displayThreshold.toFloat(),
                onValueChange = { onThresholdChange(it.toInt()) },
                valueRange = GuardConfig.MIN_FAILED_ATTEMPTS.toFloat()..30f,
                steps = 30 - GuardConfig.MIN_FAILED_ATTEMPTS - 1,
            )
            Divider()
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (currentCount > 0) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.guard_failed_current,
                            currentCount,
                            displayThreshold,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onRefreshCount) {
                        Text(stringResource(R.string.guard_failed_refresh))
                    }
                }
            }
        }
    }
}
