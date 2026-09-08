package oblivion.v2.feature.deadman

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oblivion.v2.R
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadmanScreen(
    onBack: () -> Unit,
    vm: DeadmanViewModel = hiltViewModel(),
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val adminActive = vm.isAdminActive()

    val defaultUnit = remember(config.intervalMs) {
        if (config.intervalMs % TimeUnit.DAYS.toMillis(1) == 0L) IntervalUnit.DAYS else IntervalUnit.HOURS
    }
    var unit by remember { mutableStateOf(defaultUnit) }
    var valueText by remember(config.intervalMs, unit) {
        val v = when (unit) {
            IntervalUnit.HOURS -> vm.intervalAsHours(config)
            IntervalUnit.DAYS -> vm.intervalAsDays(config)
        }
        mutableStateOf(v.toString())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.deadman_title)) },
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

            MasterCard(
                enabled = config.enabled,
                canToggle = adminActive,
                onToggle = vm::setEnabled,
            )

            IntervalCard(
                unit = unit,
                valueText = valueText,
                onUnitChange = { newUnit ->
                    unit = newUnit

                    valueText = when (newUnit) {
                        IntervalUnit.HOURS -> vm.intervalAsHours(config).toString()
                        IntervalUnit.DAYS -> vm.intervalAsDays(config).toString()
                    }
                },
                onValueChange = { newText ->
                    valueText = newText
                    val parsed = newText.toLongOrNull() ?: return@IntervalCard
                    when (unit) {
                        IntervalUnit.HOURS -> vm.setIntervalHours(parsed)
                        IntervalUnit.DAYS -> vm.setIntervalDays(parsed)
                    }
                },
            )

            StatusCard(
                enabled = config.enabled,
                lastCheckInMs = config.lastCheckInMs,
                remainingMs = config.remainingMs(),
                onCheckInNow = vm::checkInNow,
            )

            RulesCard()
        }
    }
}

private enum class IntervalUnit { HOURS, DAYS }

@Composable
private fun AdminRequiredCard() {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.deadman_admin_required),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.deadman_admin_required_desc),
                style = MaterialTheme.typography.bodySmall,
            )
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
                    text = stringResource(R.string.deadman_master_title),
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
                text = stringResource(R.string.deadman_master_desc),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun IntervalCard(
    unit: IntervalUnit,
    valueText: String,
    onUnitChange: (IntervalUnit) -> Unit,
    onValueChange: (String) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.deadman_interval_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.deadman_interval_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = unit == IntervalUnit.HOURS,
                    onClick = { onUnitChange(IntervalUnit.HOURS) },
                    label = { Text(text = stringResource(R.string.deadman_unit_hours)) },
                )
                FilterChip(
                    selected = unit == IntervalUnit.DAYS,
                    onClick = { onUnitChange(IntervalUnit.DAYS) },
                    label = { Text(text = stringResource(R.string.deadman_unit_days)) },
                )
            }
            OutlinedTextField(
                value = valueText,
                onValueChange = { new -> onValueChange(new.filter { it.isDigit() }) },
                label = {
                    Text(
                        text = when (unit) {
                            IntervalUnit.HOURS -> stringResource(R.string.deadman_value_hours_label)
                            IntervalUnit.DAYS -> stringResource(R.string.deadman_value_days_label)
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatusCard(
    enabled: Boolean,
    lastCheckInMs: Long,
    remainingMs: Long,
    onCheckInNow: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.deadman_status_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (!enabled) {
                Text(
                    text = stringResource(R.string.deadman_status_disabled),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                val lastText = if (lastCheckInMs == 0L) {
                    stringResource(R.string.deadman_status_no_checkin)
                } else {
                    stringResource(R.string.deadman_status_last_checkin, formatAbsolute(lastCheckInMs))
                }
                Text(text = lastText, style = MaterialTheme.typography.bodySmall)

                val remainingText = if (remainingMs <= 0) {
                    stringResource(R.string.deadman_status_expired)
                } else {
                    stringResource(R.string.deadman_status_remaining, formatDuration(remainingMs))
                }
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Divider()
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                onClick = onCheckInNow,
            ) {
                Text(text = stringResource(R.string.deadman_checkin_now))
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
                text = stringResource(R.string.deadman_rules_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Divider()
            Text(
                text = stringResource(R.string.deadman_rules_body),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun formatAbsolute(ms: Long): String {
    val fmt = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
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
