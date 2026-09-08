package oblivion.v2.feature.voice

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oblivion.v2.R
import oblivion.v2.core.voice.VoiceKillConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceKillScreen(
    onBack: () -> Unit,
    vm: VoiceKillViewModel = hiltViewModel(),
) {
    val config by vm.config.collectAsStateWithLifecycle()
    val adminActive = vm.isAdminActive()
    val ctx = LocalContext.current

    val needsNotifPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var hasNotifPerm by remember {
        mutableStateOf(
            !needsNotifPerm || ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasMicPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { hasNotifPerm = it }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { hasMicPerm = it }

    var phraseInput by remember(config.phrase) { mutableStateOf(config.phrase) }

    val phraseValid = phraseInput.trim().length in
        VoiceKillConfig.MIN_PHRASE_LENGTH..VoiceKillConfig.MAX_PHRASE_LENGTH
    val canEnable = adminActive && phraseValid && hasMicPerm

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.voice_title)) },
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
                WarningCard(
                    title = stringResource(R.string.voice_admin_required),
                    body = stringResource(R.string.voice_admin_required_desc),
                )
            }

            if (needsNotifPerm && !hasNotifPerm) {
                PermCard(
                    title = stringResource(R.string.voice_perm_notif_title),
                    body = stringResource(R.string.voice_perm_notif_desc),
                    action = stringResource(R.string.voice_perm_grant),
                    onAction = {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            if (!hasMicPerm) {
                PermCard(
                    title = stringResource(R.string.voice_perm_mic_title),
                    body = stringResource(R.string.voice_perm_mic_desc),
                    action = stringResource(R.string.voice_perm_grant),
                    onAction = {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                )
            }

            ModelReminderCard()

            LanguageCard(
                language = config.language,
                onLanguageChange = vm::setLanguage,
            )

            PhraseCard(
                phrase = phraseInput,
                onPhraseChange = { phraseInput = it },
                onSave = { vm.setPhrase(phraseInput) },
                valid = phraseValid,
                savedPhrase = config.phrase,
            )

            StrictCard(
                strict = config.strict,
                onToggle = vm::setStrict,
            )

            MasterCard(
                enabled = config.enabled,
                canToggle = canEnable,
                onToggle = { turnOn ->
                    if (turnOn && needsNotifPerm && !hasNotifPerm) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    vm.setEnabled(turnOn)
                },
            )

            RulesCard()
        }
    }
}

@Composable
private fun WarningCard(title: String, body: String) {
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
private fun PermCard(title: String, body: String, action: String, onAction: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(text = body, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAction,
            ) {
                Text(text = action)
            }
        }
    }
}

@Composable
private fun ModelReminderCard() {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.voice_model_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.voice_model_desc),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LanguageCard(
    language: String,
    onLanguageChange: (String) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.voice_lang_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.voice_lang_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = language == VoiceKillConfig.LANG_FR,
                    onClick = { onLanguageChange(VoiceKillConfig.LANG_FR) },
                    label = { Text(text = stringResource(R.string.voice_lang_fr)) },
                )
                FilterChip(
                    selected = language == VoiceKillConfig.LANG_EN,
                    onClick = { onLanguageChange(VoiceKillConfig.LANG_EN) },
                    label = { Text(text = stringResource(R.string.voice_lang_en)) },
                )
            }
        }
    }
}

@Composable
private fun PhraseCard(
    phrase: String,
    onPhraseChange: (String) -> Unit,
    onSave: () -> Unit,
    valid: Boolean,
    savedPhrase: String,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.voice_phrase_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.voice_phrase_desc),
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = phrase,
                onValueChange = onPhraseChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(R.string.voice_phrase_label)) },
                isError = !valid && phrase.isNotEmpty(),
                supportingText = {
                    if (!valid && phrase.isNotEmpty()) {
                        Text(
                            text = stringResource(
                                R.string.voice_phrase_bounds,
                                VoiceKillConfig.MIN_PHRASE_LENGTH,
                                VoiceKillConfig.MAX_PHRASE_LENGTH,
                            ),
                        )
                    } else if (savedPhrase != phrase.trim() && phrase.trim().isNotEmpty()) {
                        Text(text = stringResource(R.string.voice_phrase_unsaved))
                    }
                },
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = valid && phrase.trim() != savedPhrase,
                onClick = onSave,
            ) {
                Text(text = stringResource(R.string.voice_phrase_save))
            }
        }
    }
}

@Composable
private fun StrictCard(strict: Boolean, onToggle: (Boolean) -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.voice_strict_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = strict,
                    onCheckedChange = onToggle,
                )
            }
            Text(
                text = stringResource(
                    if (strict) R.string.voice_strict_on_desc
                    else R.string.voice_strict_off_desc
                ),
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
                    text = stringResource(R.string.voice_master_title),
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
                text = stringResource(R.string.voice_master_desc),
                style = MaterialTheme.typography.bodySmall,
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
                text = stringResource(R.string.voice_rules_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Divider()
            Text(
                text = stringResource(R.string.voice_rules_body),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
