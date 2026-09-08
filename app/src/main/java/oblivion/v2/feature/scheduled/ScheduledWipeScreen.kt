package oblivion.v2.feature.scheduled

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oblivion.v2.R
import oblivion.v2.core.auth.BiometricAuthenticator
import java.util.Calendar
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledWipeScreen(
    onBack: () -> Unit,
    vm: ScheduledWipeViewModel = hiltViewModel(),
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val adminActive = vm.isAdminActive()
    val ctx = LocalContext.current
    val canExact = vm.canScheduleExactAlarms()

    var selectedMs by remember {
        mutableStateOf(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.scheduled_title)) },
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
                WarnCard(
                    title = stringResource(R.string.scheduled_admin_required),
                    body = stringResource(R.string.scheduled_admin_required_desc),
                )
            }

            if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ExactAlarmPermCard(onOpenSettings = {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                })
            }

            if (config.enabled && config.wipeAtMs > 0) {
                ArmedCard(
                    wipeAtMs = config.wipeAtMs,
                    onDisarm = {
                        val activity = ctx as? FragmentActivity ?: return@ArmedCard
                        BiometricAuthenticator.authenticate(
                            activity = activity,
                            title = ctx.getString(R.string.scheduled_disarm_prompt_title),
                            subtitle = ctx.getString(R.string.scheduled_disarm_prompt_subtitle),
                            onSuccess = { vm.disarmAfterAuth() },
                            onFailure = { _, _ ->  },
                        )
                    },
                )
            } else {
                ArmCard(
                    selectedMs = selectedMs,
                    onPickDateTime = { newMs -> selectedMs = newMs },
                    canArm = adminActive && canExact,
                    onArm = { vm.arm(selectedMs) },
                )
            }

            RulesCard()
        }
    }
}

@Composable
private fun WarnCard(title: String, body: String) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Text(text = body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ExactAlarmPermCard(onOpenSettings: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.scheduled_exact_alarm_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.scheduled_exact_alarm_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.scheduled_exact_alarm_grant))
            }
        }
    }
}

@Composable
private fun ArmCard(
    selectedMs: Long,
    onPickDateTime: (Long) -> Unit,
    canArm: Boolean,
    onArm: () -> Unit,
) {
    val ctx = LocalContext.current
    val cal = remember(selectedMs) {
        Calendar.getInstance().apply { timeInMillis = selectedMs }
    }

    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.scheduled_arm_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.scheduled_arm_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(R.string.scheduled_arm_selected, formatAbsolute(selectedMs)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        DatePickerDialog(
                            ctx,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedMs
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                onPickDateTime(newCal.timeInMillis)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH),
                        ).apply {
                            datePicker.minDate = System.currentTimeMillis()
                        }.show()
                    },
                ) {
                    Text(text = stringResource(R.string.scheduled_pick_date))
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        TimePickerDialog(
                            ctx,
                            { _, h, min ->
                                val newCal = Calendar.getInstance().apply {
                                    timeInMillis = selectedMs
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, min)
                                    set(Calendar.SECOND, 0)
                                }
                                onPickDateTime(newCal.timeInMillis)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true,
                        ).show()
                    },
                ) {
                    Text(text = stringResource(R.string.scheduled_pick_time))
                }
            }
            Divider()
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = canArm && selectedMs > System.currentTimeMillis(),
                onClick = onArm,
            ) {
                Text(text = stringResource(R.string.scheduled_arm_button))
            }
        }
    }
}

@Composable
private fun ArmedCard(
    wipeAtMs: Long,
    onDisarm: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.scheduled_armed_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.scheduled_armed_at, formatAbsolute(wipeAtMs)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            val remainingMs = wipeAtMs - System.currentTimeMillis()
            val remainingText = if (remainingMs <= 0) {
                stringResource(R.string.scheduled_armed_expired)
            } else {
                stringResource(R.string.scheduled_armed_remaining, formatDuration(remainingMs))
            }
            Text(text = remainingText, style = MaterialTheme.typography.bodySmall)
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.scheduled_disarm_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDisarm,
            ) {
                Text(text = stringResource(R.string.scheduled_disarm_button))
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
                text = stringResource(R.string.scheduled_rules_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Divider()
            Text(
                text = stringResource(R.string.scheduled_rules_body),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatAbsolute(ms: Long): String {
    val fmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
    return fmt.format(java.util.Date(ms))
}

private fun formatDuration(ms: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> "${days}j ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}
