package oblivion.v2.feature.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oblivion.v2.R
import oblivion.v2.ui.theme.OblivionRed
import oblivion.v2.ui.theme.OblivionRedDark
import oblivion.v2.ui.theme.OblivionRedLight

@Composable
fun DashboardScreen(
    onOpenGuard: () -> Unit,
    onOpenUsbKill: () -> Unit,
    onOpenVoiceKill: () -> Unit,
    onOpenSmsKill: () -> Unit,
    onOpenDeadman: () -> Unit,
    onOpenScheduled: () -> Unit,
    onOpenDecoy: () -> Unit,
    onOpenWipeTest: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val adminActive by vm.adminActive.collectAsStateWithLifecycle()
    val guardCfg by vm.guardConfig.collectAsStateWithLifecycle()
    val usbCfg by vm.usbConfig.collectAsStateWithLifecycle()
    val voiceCfg by vm.voiceConfig.collectAsStateWithLifecycle()
    val smsCfg by vm.smsConfig.collectAsStateWithLifecycle()
    val deadmanCfg by vm.deadmanConfig.collectAsStateWithLifecycle()
    val scheduledCfg by vm.scheduledConfig.collectAsStateWithLifecycle()
    val decoyCfg by vm.decoyConfig.collectAsStateWithLifecycle()
    val showPasswordOk by vm.showPasswordOk.collectAsStateWithLifecycle()
    val pinPrivacyOk by vm.pinPrivacyOk.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshAdminState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activeCount = listOf(
        guardCfg.masterEnabled,
        usbCfg.enabled,
        voiceCfg.enabled,
        smsCfg.enabled,
        deadmanCfg.enabled,
        scheduledCfg.enabled,
        decoyCfg.enabled,
    ).count { it }
    val totalCount = 7

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderSection(activeCount = activeCount, totalCount = totalCount)

            Spacer(Modifier.height(28.dp))

            AdminBanner(
                active = adminActive,
                onActivate = { ctx.startActivity(vm.requestAdminIntent()) },
            )

            if (!showPasswordOk) {
                Spacer(Modifier.height(10.dp))
                SystemWarningBanner(
                    message = stringResource(R.string.dash_warn_show_password),
                    buttonLabel = stringResource(R.string.dash_warn_open_settings),
                    onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                )
            }
            if (!pinPrivacyOk) {
                Spacer(Modifier.height(10.dp))
                SystemWarningBanner(
                    message = stringResource(R.string.dash_warn_pin_privacy),
                    buttonLabel = stringResource(R.string.dash_warn_open_settings),
                    onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    },
                )
            }

            Spacer(Modifier.height(20.dp))

            SectionHeader(text = stringResource(R.string.dash_section_triggers))

            Spacer(Modifier.height(12.dp))

            TriggerCard(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.dash_guard_title),
                enabled = guardCfg.masterEnabled,
                summary = buildGuardSummary(guardCfg),
                onClick = onOpenGuard,
            )

            Spacer(Modifier.height(10.dp))

            TriggerCard(
                icon = Icons.Filled.Power,
                title = stringResource(R.string.dash_usb_title),
                enabled = usbCfg.enabled,
                summary = if (usbCfg.enabled) {
                    stringResource(R.string.dash_usb_on, usbCfg.graceSeconds)
                } else {
                    stringResource(R.string.dash_usb_off)
                },
                onClick = onOpenUsbKill,
            )

            Spacer(Modifier.height(10.dp))

            TriggerCard(
                icon = Icons.Filled.Mic,
                title = stringResource(R.string.dash_voice_title),
                enabled = voiceCfg.enabled,
                summary = if (voiceCfg.enabled) {
                    stringResource(R.string.dash_voice_on)
                } else {
                    stringResource(R.string.dash_voice_off)
                },
                onClick = onOpenVoiceKill,
            )

            Spacer(Modifier.height(10.dp))

            TriggerCard(
                icon = Icons.Filled.Message,
                title = stringResource(R.string.dash_sms_title),
                enabled = smsCfg.enabled,
                summary = if (smsCfg.enabled) {
                    stringResource(R.string.dash_sms_on)
                } else {
                    stringResource(R.string.dash_sms_off)
                },
                onClick = onOpenSmsKill,
            )

            Spacer(Modifier.height(10.dp))

            TriggerCard(
                icon = Icons.Filled.HourglassBottom,
                title = stringResource(R.string.dash_deadman_title),
                enabled = deadmanCfg.enabled,
                summary = if (deadmanCfg.enabled) {
                    stringResource(
                        R.string.dash_deadman_on,
                        java.util.concurrent.TimeUnit.MILLISECONDS.toHours(deadmanCfg.intervalMs),
                    )
                } else {
                    stringResource(R.string.dash_deadman_off)
                },
                onClick = onOpenDeadman,
            )

            Spacer(Modifier.height(10.dp))

            TriggerCard(
                icon = Icons.Filled.Schedule,
                title = stringResource(R.string.dash_scheduled_title),
                enabled = scheduledCfg.enabled,
                summary = if (scheduledCfg.enabled && scheduledCfg.wipeAtMs > 0) {
                    stringResource(
                        R.string.dash_scheduled_on,
                        java.text.SimpleDateFormat(
                            "dd/MM HH:mm",
                            java.util.Locale.getDefault(),
                        ).format(java.util.Date(scheduledCfg.wipeAtMs)),
                    )
                } else {
                    stringResource(R.string.dash_scheduled_off)
                },
                onClick = onOpenScheduled,
            )

            Spacer(Modifier.height(10.dp))

            TriggerCard(
                icon = Icons.Filled.VisibilityOff,
                title = stringResource(R.string.dash_decoy_title),
                enabled = decoyCfg.enabled,
                summary = if (decoyCfg.enabled) {
                    stringResource(R.string.dash_decoy_on)
                } else {
                    stringResource(R.string.dash_decoy_off)
                },
                onClick = onOpenDecoy,
            )

            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = onOpenWipeTest,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.BugReport,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.dash_open_wipe_test),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun HeaderSection(activeCount: Int, totalCount: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseRadius",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp),
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = (size.minDimension / 2) * pulseRadius
                drawCircle(
                    color = OblivionRed.copy(alpha = pulseAlpha * 0.3f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCircle(
                    color = OblivionRed.copy(alpha = pulseAlpha * 0.15f),
                    radius = radius * 1.1f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            Image(
                painter = painterResource(R.drawable.ic_oblivion_logo),
                contentDescription = "Oblivion",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "OBLIVION",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = OblivionRed,
            letterSpacing = 6.sp,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.dash_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.dash_active_count, activeCount, totalCount),
            style = MaterialTheme.typography.labelMedium,
            color = if (activeCount > 0) OblivionRed else MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun AdminBanner(active: Boolean, onActivate: () -> Unit) {
    val borderColor = if (active) OblivionRed.copy(alpha = 0.5f) else Color(0xFF661111)
    val bgColor = if (active) Color(0xFF1A0808) else Color(0xFF1A0505)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (active) Color(0xFF4CAF50) else OblivionRed),
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (active) R.string.dash_admin_active
                        else R.string.dash_admin_inactive
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) Color(0xFF4CAF50) else OblivionRedLight,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = stringResource(
                        if (active) R.string.dash_admin_active_desc
                        else R.string.dash_admin_inactive_desc
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }

            if (!active) {
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onActivate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OblivionRed,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dash_admin_enable),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(OblivionRed, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = OblivionRed.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.width(12.dp))
        Divider(
            modifier = Modifier.weight(1f),
            color = OblivionRed.copy(alpha = 0.15f),
            thickness = 1.dp,
        )
    }
}

@Composable
private fun TriggerCard(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    summary: String,
    onClick: () -> Unit,
) {
    val accentColor = if (enabled) OblivionRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    val cardBg = if (enabled) Color(0xFF140A0A) else Color(0xFF111111)
    val borderAlpha = if (enabled) 0.4f else 0.1f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OblivionRed.copy(alpha = borderAlpha)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(88.dp)
                    .background(accentColor),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (enabled) OblivionRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    StatusChip(enabled = enabled)
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(enabled: Boolean) {
    val bgColor = if (enabled) OblivionRed.copy(alpha = 0.15f) else Color(0xFF222222)
    val textColor = if (enabled) OblivionRed else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (enabled) OblivionRed.copy(alpha = 0.4f) else Color(0xFF333333)

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (enabled) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(OblivionRed),
                )
            }
            Text(
                text = stringResource(
                    if (enabled) R.string.dash_status_on else R.string.dash_status_off
                ),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun SystemWarningBanner(
    message: String,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    val warningColor = Color(0xFFFF9800)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, warningColor.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1200)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = warningColor,
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = warningColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onClick,
                border = BorderStroke(1.dp, warningColor.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp, vertical = 4.dp,
                ),
            ) {
                Text(
                    text = buttonLabel,
                    fontSize = 10.sp,
                    color = warningColor,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun buildGuardSummary(cfg: oblivion.v2.core.guard.GuardConfig): String {
    if (!cfg.masterEnabled) return stringResource(R.string.dash_guard_off)

    val parts = mutableListOf<String>()
    if (cfg.typeAEnabled) parts += stringResource(R.string.dash_guard_type_a)
    if (cfg.typeBEnabled) parts += stringResource(R.string.dash_guard_type_b, cfg.typeBLength)
    if (cfg.emergencyEnabled) parts += stringResource(R.string.dash_guard_emergency)
    if (cfg.failedAttemptsEnabled) parts += stringResource(R.string.dash_guard_failed, cfg.failedAttemptsThreshold)

    return if (parts.isEmpty()) {
        stringResource(R.string.dash_guard_on_nothing)
    } else {
        parts.joinToString(" · ")
    }
}
