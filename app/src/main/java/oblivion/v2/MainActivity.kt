package oblivion.v2

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import oblivion.v2.core.auth.BiometricAuthState
import oblivion.v2.core.guard.GuardRevocationDetector
import oblivion.v2.ui.auth.BiometricGate
import oblivion.v2.ui.nav.AppNavHost
import oblivion.v2.ui.theme.OblivionTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var revocationDetector: GuardRevocationDetector
    @Inject lateinit var authState: BiometricAuthState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )

        authState.invalidate()

        enableEdgeToEdge()
        setContent {
            OblivionTheme {
                BiometricGate(authState = authState) {
                    AppNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        authState.checkStillValid()

        runCatching {
            revocationDetector.checkAndWipeIfRevoked(this)
        }
    }
}
