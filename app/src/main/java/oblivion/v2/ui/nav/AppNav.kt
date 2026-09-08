package oblivion.v2.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import oblivion.v2.feature.dashboard.DashboardScreen
import oblivion.v2.feature.deadman.DeadmanScreen
import oblivion.v2.feature.decoy.DecoyScreen
import oblivion.v2.feature.guard.GuardScreen
import oblivion.v2.feature.scheduled.ScheduledWipeScreen
import oblivion.v2.feature.sms.SmsKillScreen
import oblivion.v2.feature.usb.UsbKillScreen
import oblivion.v2.feature.voice.VoiceKillScreen
import oblivion.v2.feature.wipetest.WipeTestScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val WIPE_TEST = "wipeTest"
    const val GUARD = "guard"
    const val USB_KILL = "usbKill"
    const val VOICE_KILL = "voiceKill"
    const val SMS_KILL = "smsKill"
    const val DEADMAN = "deadman"
    const val SCHEDULED = "scheduled"
    const val DECOY = "decoy"
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onOpenGuard = { navController.navigate(Routes.GUARD) },
                onOpenUsbKill = { navController.navigate(Routes.USB_KILL) },
                onOpenVoiceKill = { navController.navigate(Routes.VOICE_KILL) },
                onOpenSmsKill = { navController.navigate(Routes.SMS_KILL) },
                onOpenDeadman = { navController.navigate(Routes.DEADMAN) },
                onOpenScheduled = { navController.navigate(Routes.SCHEDULED) },
                onOpenDecoy = { navController.navigate(Routes.DECOY) },
                onOpenWipeTest = { navController.navigate(Routes.WIPE_TEST) },
            )
        }
        composable(Routes.WIPE_TEST) {
            WipeTestScreen(
                onOpenGuard = { navController.navigate(Routes.GUARD) },
                onOpenUsbKill = { navController.navigate(Routes.USB_KILL) },
                onOpenVoiceKill = { navController.navigate(Routes.VOICE_KILL) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.GUARD) {
            GuardScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.USB_KILL) {
            UsbKillScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.VOICE_KILL) {
            VoiceKillScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SMS_KILL) {
            SmsKillScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.DEADMAN) {
            DeadmanScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SCHEDULED) {
            ScheduledWipeScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.DECOY) {
            DecoyScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
