package oblivion.v2.feature.wipetest

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import oblivion.v2.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WipeTestScreen(
    onOpenGuard: () -> Unit = {},
    onOpenUsbKill: () -> Unit = {},
    onOpenVoiceKill: () -> Unit = {},
    onBack: () -> Unit = {},
    vm: WipeTestViewModel = viewModel(),
) {
    val adminActive by vm.adminActive.collectAsStateWithLifecycle()
    val lastWipeMessage by vm.lastWipeMessage.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showStep1 by remember { mutableStateOf(false) }
    var showStep2 by remember { mutableStateOf(false) }
    var typedConfirmation by remember { mutableStateOf("") }

    LaunchedEffect(lastWipeMessage) {
        lastWipeMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearWipeMessage()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshAdminState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.wipe_test_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
        ) {
            AdminStatusCard(adminActive = adminActive)

            Spacer(Modifier.height(12.dp))

            DiagnosticCard(
                componentName = vm.adminComponentName,
                adminActive = adminActive,
            )

            Spacer(Modifier.height(16.dp))

            if (!adminActive) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { ctx.startActivity(vm.requestAdminIntent()) },
                ) {
                    Text(text = stringResource(R.string.wipe_test_enable_admin))
                }
                Spacer(Modifier.height(8.dp))
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intents = listOf(
                        Intent().setComponent(
                            android.content.ComponentName(
                                "com.android.settings",
                                "com.android.settings.DeviceAdminSettings",
                            )
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        Intent(Settings.ACTION_SECURITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    for (i in intents) {
                        if (runCatching { ctx.startActivity(i) }.isSuccess) break
                    }
                },
            ) {
                Text(text = stringResource(R.string.wipe_test_open_admin_settings))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenGuard,
            ) {
                Text(text = stringResource(R.string.wipe_test_open_guard))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenUsbKill,
            ) {
                Text(text = stringResource(R.string.wipe_test_open_usb))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenVoiceKill,
            ) {
                Text(text = stringResource(R.string.wipe_test_open_voice))
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.wipe_test_warning),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = adminActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                onClick = { showStep1 = true },
            ) {
                Text(
                    text = stringResource(R.string.wipe_test_button),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }

    if (showStep1) {
        AlertDialog(
            onDismissRequest = { showStep1 = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(text = stringResource(R.string.wipe_test_confirm1_title)) },
            text = { Text(text = stringResource(R.string.wipe_test_confirm1_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showStep1 = false
                    typedConfirmation = ""
                    showStep2 = true
                }) {
                    Text(text = stringResource(R.string.wipe_test_confirm1_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStep1 = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showStep2) {
        val expected = stringResource(R.string.wipe_test_confirm2_expected)
        val match = typedConfirmation.trim() == expected

        AlertDialog(
            onDismissRequest = { showStep2 = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(text = stringResource(R.string.wipe_test_confirm2_title)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.wipe_test_confirm2_text, expected))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = typedConfirmation,
                        onValueChange = { typedConfirmation = it },
                        singleLine = true,
                        label = { Text(text = expected) },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = match,
                    onClick = {
                        showStep2 = false

                        vm.triggerWipeNow()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.wipe_test_confirm2_go),
                        color = if (match) MaterialTheme.colorScheme.error else Color.Gray,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showStep2 = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun AdminStatusCard(adminActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (adminActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (adminActive) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(
                        if (adminActive) R.string.wipe_test_admin_active
                        else R.string.wipe_test_admin_inactive
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(
                    if (adminActive) R.string.wipe_test_admin_active_desc
                    else R.string.wipe_test_admin_inactive_desc
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DiagnosticCard(componentName: String, adminActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.wipe_test_diag_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.wipe_test_diag_component, componentName),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    R.string.wipe_test_diag_admin_state,
                    adminActive.toString()
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
