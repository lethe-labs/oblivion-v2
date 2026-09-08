package oblivion.v2.feature.decoy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oblivion.v2.R
import oblivion.v2.core.decoy.DecoyConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoyScreen(
    onBack: () -> Unit,
    vm: DecoyViewModel = hiltViewModel(),
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val adminActive = vm.isAdminActive()
    val fsiOk = vm.hasFullScreenIntentPermission()
    val context = LocalContext.current

    val needsNotifPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var hasNotifPerm by remember {
        mutableStateOf(
            !needsNotifPerm || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotifPerm = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.decoy_title)) },
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
            if (!adminActive) {
                AdminRequiredCard()
            }

            if (needsNotifPerm && !hasNotifPerm) {
                NotifPermCard(
                    onRequest = {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            if (!fsiOk) {
                FullScreenIntentPermissionCard(
                    onGrant = {
                        vm.fullScreenIntentSettings()?.let { intent ->
                            runCatching { context.startActivity(intent) }
                        }
                    },
                )
            }

            MasterCard(
                enabled = config.enabled,
                canToggle = adminActive && config.isConfigured(),
                onToggle = { turnOn ->

                    if (turnOn && needsNotifPerm && !hasNotifPerm) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.setEnabled(turnOn)
                },
            )

            PinCard(
                configured = config.isConfigured(),
                onSetPin = vm::setDecoyPin,
                onClearPin = vm::clearDecoyPin,
            )

            RulesCard()
        }
    }
}

@Composable
private fun AdminRequiredCard() {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.decoy_admin_required),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.decoy_admin_required_desc),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun NotifPermCard(onRequest: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.decoy_notif_perm_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.decoy_notif_perm_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequest,
            ) {
                Text(text = stringResource(R.string.decoy_notif_perm_grant))
            }
        }
    }
}

@Composable
private fun FullScreenIntentPermissionCard(onGrant: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.decoy_fsi_required),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.decoy_fsi_required_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onGrant) {
                Text(stringResource(R.string.decoy_fsi_grant))
            }
        }
    }
}

@Composable
private fun MasterCard(
    enabled: Boolean,
    canToggle: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.decoy_master_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    enabled = canToggle,
                    onCheckedChange = onToggle,
                )
            }
            Text(
                text = stringResource(R.string.decoy_master_desc),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PinCard(
    configured: Boolean,
    onSetPin: (String) -> Unit,
    onClearPin: () -> Unit,
) {
    var pinInput by remember { mutableStateOf("") }
    val canSave = pinInput.length >= DecoyConfig.MIN_PIN_LENGTH

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.decoy_pin_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.decoy_pin_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    if (configured) R.string.decoy_pin_configured
                    else R.string.decoy_pin_not_configured
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (configured) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(32) },
                label = { Text(stringResource(R.string.decoy_pin_input_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(
                        text = stringResource(
                            R.string.decoy_pin_min_hint,
                            DecoyConfig.MIN_PIN_LENGTH,
                        )
                    )
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        if (canSave) {
                            onSetPin(pinInput)
                            pinInput = ""
                        }
                    },
                    enabled = canSave,
                ) {
                    Text(stringResource(R.string.decoy_pin_save))
                }
                if (configured) {
                    TextButton(
                        onClick = {
                            onClearPin()
                            pinInput = ""
                        },
                    ) {
                        Text(stringResource(R.string.decoy_pin_clear))
                    }
                }
            }
        }
    }
}

@Composable
private fun RulesCard() {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.decoy_rules_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Divider()
            Text(
                text = stringResource(R.string.decoy_rules_body),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
