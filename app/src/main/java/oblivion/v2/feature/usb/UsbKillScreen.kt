package oblivion.v2.feature.usb

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oblivion.v2.R
import oblivion.v2.core.usb.UsbKillConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbKillScreen(
    onBack: () -> Unit,
    vm: UsbKillViewModel = hiltViewModel(),
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val adminActive = vm.isAdminActive()
    val ctx = LocalContext.current

    val needsNotifPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var hasNotifPerm by remember {
        mutableStateOf(
            !needsNotifPerm || ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
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
                title = { Text(text = stringResource(R.string.usb_title)) },
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
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.usb_admin_required),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.usb_admin_required_desc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            if (needsNotifPerm && !hasNotifPerm) {
                NotifPermCard(
                    onRequest = {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            MasterCard(
                enabled = config.enabled,
                canToggle = adminActive,
                onToggle = { turnOn ->
                    if (turnOn && needsNotifPerm && !hasNotifPerm) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.setEnabled(turnOn)
                },
            )

            GraceCard(
                enabled = config.enabled,
                graceSeconds = config.graceSeconds,
                onChange = vm::setGraceSeconds,
            )

            RulesCard()
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
                text = stringResource(R.string.usb_notif_perm_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.usb_notif_perm_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRequest,
            ) {
                Text(text = stringResource(R.string.usb_notif_perm_grant))
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
                    text = stringResource(R.string.usb_master_title),
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
                text = stringResource(R.string.usb_master_desc),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun GraceCard(
    enabled: Boolean,
    graceSeconds: Int,
    onChange: (Int) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.usb_grace_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.usb_grace_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = if (graceSeconds == 0) {
                    stringResource(R.string.usb_grace_value_immediate)
                } else {
                    stringResource(R.string.usb_grace_value, graceSeconds)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Slider(
                value = graceSeconds.toFloat(),
                onValueChange = { onChange(it.toInt()) },
                valueRange = UsbKillConfig.MIN_GRACE_SECONDS.toFloat()
                    ..UsbKillConfig.MAX_GRACE_SECONDS.toFloat(),
                steps = UsbKillConfig.MAX_GRACE_SECONDS - UsbKillConfig.MIN_GRACE_SECONDS - 1,
                enabled = enabled,
            )
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
                text = stringResource(R.string.usb_rules_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Divider()
            Text(
                text = stringResource(R.string.usb_rules_body),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
