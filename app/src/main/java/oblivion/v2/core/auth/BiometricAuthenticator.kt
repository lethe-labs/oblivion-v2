package oblivion.v2.core.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import oblivion.v2.core.log.SecLog

object BiometricAuthenticator {
    private const val TAG = "BiometricAuth"

    enum class Capability {
        AVAILABLE,

        NONE_ENROLLED,

        UNAVAILABLE,
    }

    fun canAuthenticate(context: Context): Capability {
        val manager = BiometricManager.from(context)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL,
        )
        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> Capability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Capability.NONE_ENROLLED
            else -> Capability.UNAVAILABLE
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String?,
        onSuccess: () -> Unit,
        onFailure: (errorCode: Int, errorMessage: String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    SecLog.d(TAG, "Authentication succeeded")
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    SecLog.w(TAG, "Authentication error code=$errorCode")
                    onFailure(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    SecLog.d(TAG, "Authentication failed (retryable)")
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()

        prompt.authenticate(info)
    }
}
