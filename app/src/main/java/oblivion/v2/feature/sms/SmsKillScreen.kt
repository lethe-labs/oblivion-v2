package oblivion.v2.feature.sms

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oblivion.v2.BuildConfig
import oblivion.v2.R
import oblivion.v2.core.sms.SmsKillConfig
import oblivion.v2.ui.theme.OblivionRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsKillScreen(
    onBack: () -> Unit,
    vm: SmsKillViewModel = hiltViewModel(),
) {
    val cfg by vm.config.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    var senderInput by remember(cfg.senderNumber) { mutableStateOf(cfg.senderNumber) }
    var keywordInput by remember(cfg.keyword) { mutableStateOf(cfg.keyword) }

    var simSender by remember { mutableStateOf("") }
    var simBody by remember { mutableStateOf("") }
    var simResult by remember { mutableStateOf<Boolean?>(null) }

    val smsPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  }

    val hasSmsPermission = ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.RECEIVE_SMS
    ) == PackageManager.PERMISSION_GRANTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sms_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
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
            if (!hasSmsPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.sms_perm_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.sms_perm_desc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            smsPermLauncher.launch(Manifest.permission.RECEIVE_SMS)
                        }) {
                            Text(stringResource(R.string.sms_perm_grant))
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.sms_sender_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.sms_sender_desc),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = senderInput,
                        onValueChange = { senderInput = it.take(20) },
                        label = { Text(stringResource(R.string.sms_sender_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            vm.setSenderNumber(senderInput)
                            Toast.makeText(ctx, "Numéro enregistré: $senderInput", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = senderInput.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.sms_sender_save))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.sms_keyword_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.sms_keyword_desc),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keywordInput,
                        onValueChange = {
                            keywordInput = it.take(SmsKillConfig.MAX_KEYWORD_LENGTH)
                        },
                        label = { Text(stringResource(R.string.sms_keyword_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(
                            R.string.sms_keyword_bounds,
                            SmsKillConfig.MIN_KEYWORD_LENGTH,
                            SmsKillConfig.MAX_KEYWORD_LENGTH,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            vm.setKeyword(keywordInput)
                            Toast.makeText(ctx, "Mot-clé enregistré", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = keywordInput.length >= SmsKillConfig.MIN_KEYWORD_LENGTH,
                    ) {
                        Text(stringResource(R.string.sms_keyword_save))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sms_master_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.sms_master_desc),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = cfg.enabled,
                            onCheckedChange = { vm.setEnabled(it) },
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.sms_rules_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.sms_rules_body),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1200),
                    ),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.sms_sim_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800),
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.sms_sim_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800).copy(alpha = 0.7f),
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = simSender,
                            onValueChange = { simSender = it },
                            label = { Text(stringResource(R.string.sms_sim_sender)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = simBody,
                            onValueChange = { simBody = it },
                            label = { Text(stringResource(R.string.sms_sim_body)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                simResult = vm.simulateSms(simSender, simBody)
                                val msg = if (simResult == true) "MATCH → Wipe triggered!"
                                    else "No match"
                                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.5f)),
                        ) {
                            Text(
                                text = stringResource(R.string.sms_sim_button),
                                color = Color(0xFFFF9800),
                                fontFamily = FontFamily.Monospace,
                            )
                        }

                        if (simResult != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (simResult == true) {
                                    stringResource(R.string.sms_sim_result_match)
                                } else {
                                    stringResource(R.string.sms_sim_result_no_match)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (simResult == true) OblivionRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}
